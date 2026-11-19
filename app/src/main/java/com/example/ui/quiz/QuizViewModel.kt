package com.example.ui.quiz

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.data.ModalAiService
import com.example.data.QuizRepository
import com.example.ui.base.BaseViewModel
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch

private const val TAG = "QuizViewModel"

class QuizViewModel(
  private val repository: QuizRepository = QuizRepository(),
  private val aiService: ModalAiService = ModalAiService()
) : BaseViewModel<QuizUiState, QuizEvent, QuizEffect>(
  initialState = QuizUiState(
    filteredQuestionCount = repository.allQuestions.size
  )
) {

  private var aiTutorJob: Job? = null

  override fun onEvent(event: QuizEvent) {
    when (event) {
      QuizEvent.ToggleTheme -> updateState { copy(isLightTheme = !isLightTheme) }
      is QuizEvent.SelectGameMode -> updateState { copy(gameMode = event.mode) }
      is QuizEvent.UpdateSoloName -> updateState { copy(soloNameInput = event.name) }
      is QuizEvent.UpdateGroupNameInput -> updateState { copy(groupNameInput = event.name) }
      QuizEvent.AddGroupPlayer -> addGroupPlayer()
      is QuizEvent.RemovePlayer -> removePlayer(event.playerId)
      is QuizEvent.ToggleModalGroup -> toggleModalGroup(event.group)
      is QuizEvent.SelectDifficulty -> selectDifficulty(event.difficulty)
      QuizEvent.StartQuiz -> startQuiz()
      is QuizEvent.SubmitAnswer -> submitAnswer(event.questionId, event.answer)
      QuizEvent.ToggleSettingsDrawer -> updateState { copy(isSettingsDrawerOpen = !isSettingsDrawerOpen) }
      QuizEvent.ToggleSound -> updateState { copy(isSoundEnabled = !isSoundEnabled) }
      QuizEvent.ShuffleActiveQuestions -> shuffleActiveQuestions()
      QuizEvent.OpenResults -> updateState { copy(screenMode = QuizScreenMode.RESULTS, isSettingsDrawerOpen = false) }
      QuizEvent.ReviewAnswers -> updateState { copy(screenMode = QuizScreenMode.QUIZ) }
      QuizEvent.PlayAgain -> playAgain()
      QuizEvent.RetryMistakes -> retryMistakes()
      QuizEvent.ResetToSetup -> resetToSetup()
      is QuizEvent.LoadHighScore -> updateState {
        if (event.score > highScore) copy(highScore = event.score) else this
      }
      is QuizEvent.AskAiTutor -> askAiTutor(event.questionId)
      QuizEvent.DismissAiTutor -> {
        aiTutorJob?.cancel()
        updateState { copy(aiTutorState = AiTutorState()) }
      }
    }
  }

  private fun addGroupPlayer() {
    val name = currentState.groupNameInput.trim()
    if (name.isEmpty()) return
    val newPlayer = Player(id = UUID.randomUUID().toString(), name = name)
    updateState {
      copy(
        players = players + newPlayer,
        groupNameInput = ""
      )
    }
  }

  private fun removePlayer(playerId: String) {
    updateState {
      val updatedPlayers = players.filterNot { it.id == playerId }
      val nextTurn = if (updatedPlayers.isEmpty()) 0 else currentTurnIndex % updatedPlayers.size
      copy(players = updatedPlayers, currentTurnIndex = nextTurn)
    }
  }

  private fun toggleModalGroup(group: ModalGroup) {
    updateState {
      val updatedGroups = if (group == ModalGroup.ALL) {
        setOf(ModalGroup.ALL)
      } else {
        val withoutAll = selectedGroups - ModalGroup.ALL
        val toggled = if (group in withoutAll) withoutAll - group else withoutAll + group
        toggled.ifEmpty { setOf(ModalGroup.ALL) }
      }
      copy(
        selectedGroups = updatedGroups,
        filteredQuestionCount = repository.getQuestionsForGroups(updatedGroups).size
      )
    }
  }

  private fun selectDifficulty(difficulty: QuizDifficulty) {
    updateState {
      copy(
        difficulty = difficulty,
        showContext = true,
        showCategory = difficulty == QuizDifficulty.EASY
      )
    }
  }

  private fun startQuiz() {
    val resolvedPlayers = resolvePlayersForStart(currentState)
    val randomized = prepareRandomizedQuestions(
      repository.getQuestionsForGroups(currentState.selectedGroups)
    )
    updateState {
      copy(
        screenMode = QuizScreenMode.QUIZ,
        players = resolvedPlayers,
        groupNameInput = if (gameMode == GameMode.GROUP) "" else groupNameInput,
        currentTurnIndex = 0,
        activeQuestions = randomized,
        answersByQuestionId = emptyMap(),
        showContext = true,
        showCategory = difficulty == QuizDifficulty.EASY,
        isSettingsDrawerOpen = false,
        isNewHighScore = false,
        currentStreak = 0,
        bestStreak = 0
      )
    }
    sendEffect(QuizEffect.ScrollToTop)
  }

  private fun resolvePlayersForStart(state: QuizUiState): List<Player> {
    return when (state.gameMode) {
      GameMode.SOLO -> {
        val name = state.soloNameInput.trim().ifEmpty { "Jogador" }
        listOf(Player(id = UUID.randomUUID().toString(), name = name))
      }
      GameMode.GROUP -> {
        if (state.players.isNotEmpty()) {
          state.players.map { it.copy(score = 0) }
        } else {
          val pending = state.groupNameInput.trim()
          if (pending.isNotEmpty()) {
            listOf(Player(id = UUID.randomUUID().toString(), name = pending))
          } else {
            listOf(
              Player(id = UUID.randomUUID().toString(), name = "Jogador 1"),
              Player(id = UUID.randomUUID().toString(), name = "Jogador 2")
            )
          }
        }
      }
    }
  }

  private fun submitAnswer(questionId: Int, rawAnswer: String) {
    val trimmed = rawAnswer.trim()
    if (trimmed.isEmpty() || questionId in currentState.answersByQuestionId) return

    val question = currentState.activeQuestions.find { it.id == questionId } ?: return
    val outcome = question.evaluateAnswer(trimmed)

    val xpEarned = when (outcome) {
      AnswerOutcome.CORRECT -> 100
      AnswerOutcome.MAYBE -> 50
      AnswerOutcome.WRONG -> 0
    }

    val record = AnswerRecord(
      questionId = questionId,
      submittedAnswer = trimmed,
      outcome = outcome,
      correctModal = repository.getModalDefinition(
        key = question.acceptedAnswers.first(),
        fallbackCategory = question.category
      ),
      submittedModal = repository.findModalDefinitionOrNull(trimmed),
      xpEarned = xpEarned
    )

    updateState {
      val updatedPlayers = players.mapIndexed { index, player ->
        if (xpEarned > 0 && index == currentTurnIndex) player.copy(score = player.score + xpEarned) else player
      }
      val nextTurn = if (updatedPlayers.size > 1) {
        (currentTurnIndex + 1) % updatedPlayers.size
      } else {
        0
      }
      val updatedAnswers = answersByQuestionId + (questionId to record)
      val nextStreak = when (outcome) {
        AnswerOutcome.CORRECT -> currentStreak + 1
        AnswerOutcome.MAYBE -> currentStreak
        AnswerOutcome.WRONG -> 0
      }
      val nextBestStreak = maxOf(bestStreak, nextStreak)
      val completedNow = activeQuestions.isNotEmpty() && updatedAnswers.size >= activeQuestions.size
      val roundScore = updatedPlayers.sumOf { it.score }
      val achievedNewHighScore = completedNow && roundScore > 0 && roundScore > highScore
      copy(
        players = updatedPlayers,
        currentTurnIndex = nextTurn,
        answersByQuestionId = updatedAnswers,
        screenMode = if (completedNow) QuizScreenMode.RESULTS else screenMode,
        isSettingsDrawerOpen = if (completedNow) false else isSettingsDrawerOpen,
        highScore = if (achievedNewHighScore) roundScore else highScore,
        isNewHighScore = if (completedNow) achievedNewHighScore else isNewHighScore,
        currentStreak = nextStreak,
        bestStreak = nextBestStreak
      )
    }
    if (currentState.isSoundEnabled) {
      sendEffect(QuizEffect.PlayAnswerSound(outcome))
    }
  }

  private fun playAgain() {
    val randomized = prepareRandomizedQuestions(
      repository.getQuestionsForGroups(currentState.selectedGroups)
    )
    updateState {
      copy(
        screenMode = QuizScreenMode.QUIZ,
        activeQuestions = randomized,
        answersByQuestionId = emptyMap(),
        players = players.map { it.copy(score = 0) },
        currentTurnIndex = 0,
        showContext = true,
        showCategory = difficulty == QuizDifficulty.EASY,
        isSettingsDrawerOpen = false,
        isNewHighScore = false,
        currentStreak = 0,
        bestStreak = 0
      )
    }
    sendEffect(QuizEffect.ScrollToTop)
  }

  private fun retryMistakes() {
    if (currentState.gameMode != GameMode.SOLO) return
    val mistakeIds = currentState.answersByQuestionId.values
      .filter { it.outcome != AnswerOutcome.CORRECT }
      .map { it.questionId }
      .toSet()
    val subset = currentState.activeQuestions.filter { it.id in mistakeIds }
    if (subset.isEmpty()) return

    val randomized = prepareRandomizedQuestions(subset)
    updateState {
      copy(
        screenMode = QuizScreenMode.QUIZ,
        activeQuestions = randomized,
        answersByQuestionId = emptyMap(),
        players = players.map { it.copy(score = 0) },
        currentTurnIndex = 0,
        isSettingsDrawerOpen = false,
        isNewHighScore = false,
        currentStreak = 0,
        bestStreak = 0
      )
    }
    sendEffect(QuizEffect.ScrollToTop)
  }

  private fun shuffleActiveQuestions() {
    updateState {
      copy(
        activeQuestions = prepareRandomizedQuestions(activeQuestions),
        answersByQuestionId = emptyMap(),
        players = players.map { it.copy(score = 0) },
        currentTurnIndex = 0,
        isSettingsDrawerOpen = false,
        isNewHighScore = false,
        currentStreak = 0,
        bestStreak = 0
      )
    }
    sendEffect(QuizEffect.ScrollToTop)
  }

  private fun resetToSetup() {
    updateState {
      copy(
        screenMode = QuizScreenMode.SETUP,
        isSettingsDrawerOpen = false,
        isNewHighScore = false
      )
    }
    sendEffect(QuizEffect.ScrollToTop)
  }

  private fun prepareRandomizedQuestions(list: List<QuizQuestion>): List<QuizQuestion> {
    return list.shuffled().map { question ->
      if (question.options.isNotEmpty()) {
        question.copy(options = question.options.shuffled())
      } else {
        question
      }
    }
  }

  private fun askAiTutor(questionId: Int) {
    val question = currentState.activeQuestions.find { it.id == questionId } ?: return
    val record = currentState.answersByQuestionId[questionId] ?: return
    aiTutorJob?.cancel()
    aiTutorJob = viewModelScope.launch {
      updateState {
        copy(
          aiTutorState = AiTutorState(
            isVisible = true,
            questionSentence = question.sentence,
            isLoading = true,
            text = "",
            errorMessage = null
          )
        )
      }
      val sb = StringBuilder()
      try {
        aiService.explainQuestionStream(
          question = question,
          userAnswer = record.submittedAnswer,
          outcome = record.outcome
        ).collect { chunk ->
          sb.append(chunk)
          updateState {
            copy(
              aiTutorState = aiTutorState.copy(
                isLoading = false,
                text = sb.toString()
              )
            )
          }
        }
        if (sb.isBlank()) {
          updateState {
            copy(
              aiTutorState = aiTutorState.copy(
                isLoading = false,
                errorMessage = "O Tutor IA retornou uma resposta vazia. Tente novamente."
              )
            )
          }
        }
      } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "AI Tutor timed out", e)
        updateState {
          copy(
            aiTutorState = aiTutorState.copy(
              isLoading = false,
              errorMessage = "Tempo esgotado ao consultar o Tutor IA. Tente novamente."
            )
          )
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "AI Tutor failed", e)
        updateState {
          copy(
            aiTutorState = aiTutorState.copy(
              isLoading = false,
              errorMessage = e.localizedMessage ?: "Erro ao consultar o Tutor IA."
            )
          )
        }
      }
    }
  }
}
