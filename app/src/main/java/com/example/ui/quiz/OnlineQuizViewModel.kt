package com.example.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.OnlinePlayer
import com.example.data.OnlineQuizRepository
import com.example.data.QuizRepository
import com.example.data.QuizRoom
import com.example.data.RoomPhase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OnlineGroupSubMode {
  CREATE,
  JOIN
}

sealed interface OnlineQuizEffect {
  data class PlayAnswerSound(val outcome: AnswerOutcome) : OnlineQuizEffect
}

data class OnlineQuizUiState(
  val activeRoomCode: String? = null,
  val room: QuizRoom? = null,
  val players: List<OnlinePlayer> = emptyList(),
  val isLoading: Boolean = false,
  val isSubmitting: Boolean = false,
  val errorMessage: String? = null,
  val pinInput: String = "",
  val displayNameInput: String = "",
  val onlineSubMode: OnlineGroupSubMode = OnlineGroupSubMode.CREATE,
  val typingInput: String = "",
  val currentStreak: Int = 0,
  val bestStreak: Int = 0,
  val lastPointsAwarded: Long = 0L
) {
  fun isHost(userId: String): Boolean = room?.hostId == userId

  fun currentPlayer(userId: String): OnlinePlayer? =
    players.firstOrNull { it.userId == userId || it.id == userId }

  val totalQuestions: Int
    get() = room?.questionIds?.size ?: 0

  val currentQuestionIndex: Int
    get() = (room?.currentQuestionIndex ?: 0L).toInt()

  val currentQuestionNumber: Int
    get() = (currentQuestionIndex + 1).coerceIn(1, totalQuestions.coerceAtLeast(1))

  val answeredCount: Int
    get() {
      val idx = room?.currentQuestionIndex ?: return 0
      return players.count { it.lastAnsweredIndex == idx }
    }

  fun hasAnsweredCurrent(userId: String): Boolean {
    val idx = room?.currentQuestionIndex ?: return false
    return currentPlayer(userId)?.lastAnsweredIndex == idx
  }
}

class OnlineQuizViewModel(
  private val onlineRepository: OnlineQuizRepository,
  val quizRepository: QuizRepository,
  val currentUserId: String
) : ViewModel() {

  private val _uiState = MutableStateFlow(OnlineQuizUiState())
  val uiState: StateFlow<OnlineQuizUiState> = _uiState.asStateFlow()

  private val _effects = MutableSharedFlow<OnlineQuizEffect>(extraBufferCapacity = 4)
  val effects: SharedFlow<OnlineQuizEffect> = _effects.asSharedFlow()

  private var roomObserverJob: Job? = null
  private var playersObserverJob: Job? = null
  private var pendingRevealQuestionIndex: Int = -1
  private var pendingRevealOutcome: AnswerOutcome? = null
  private var pendingRevealSoundEnabled: Boolean = true
  private var pendingNextStreak: Int? = null
  private var lastRevealedSoundIndex: Int = -1

  private val questionsById: Map<Int, QuizQuestion> =
    quizRepository.allQuestions.associateBy { it.id }

  fun prefillDisplayNameIfBlank(name: String?) {
    if (name.isNullOrBlank()) return
    _uiState.update { state ->
      if (state.displayNameInput.isBlank()) {
        state.copy(displayNameInput = name.trim().take(40))
      } else {
        state
      }
    }
  }

  fun selectSubMode(mode: OnlineGroupSubMode) {
    _uiState.update { it.copy(onlineSubMode = mode, errorMessage = null) }
  }

  fun updateDisplayName(value: String) {
    _uiState.update { it.copy(displayNameInput = value.take(40), errorMessage = null) }
  }

  fun updatePinInput(value: String) {
    val sanitized = value.uppercase().filter { it.isLetterOrDigit() }.take(8)
    _uiState.update { it.copy(pinInput = sanitized, errorMessage = null) }
  }

  fun updateTypingInput(value: String) {
    _uiState.update { it.copy(typingInput = value.take(40)) }
  }

  fun clearError() {
    _uiState.update { it.copy(errorMessage = null) }
  }

  fun currentQuestionFor(room: QuizRoom?): QuizQuestion? {
    val qId = room?.questionIds?.getOrNull(room.currentQuestionIndex.toInt())?.toInt() ?: return null
    return questionsById[qId]
  }

  fun createRoom(selectedGroups: Set<ModalGroup>, difficulty: QuizDifficulty) {
    val state = _uiState.value
    if (state.isLoading) return
    val questions = quizRepository.getQuestionsForGroups(selectedGroups).shuffled()
    if (questions.isEmpty()) {
      _uiState.update { it.copy(errorMessage = "Selecione ao menos um verbo modal.") }
      return
    }
    val questionIds = questions.map { it.id }
    val hostName = state.displayNameInput.trim().ifEmpty { "Anfitrião" }

    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      val result = onlineRepository.createRoom(
        hostName = hostName,
        difficulty = difficulty,
        questionIds = questionIds
      )
      result.fold(
        onSuccess = { code ->
          _uiState.update {
            it.copy(
              activeRoomCode = code,
              isLoading = false,
              errorMessage = null,
              typingInput = ""
            )
          }
          startObservingRoom(code)
        },
        onFailure = { error ->
          _uiState.update {
            it.copy(
              isLoading = false,
              errorMessage = error.localizedMessage ?: "Não foi possível criar a sala."
            )
          }
        }
      )
    }
  }

  fun joinRoom() {
    val state = _uiState.value
    if (state.isLoading) return
    val code = state.pinInput.trim().uppercase()
    if (code.length < 4) {
      _uiState.update { it.copy(errorMessage = "Digite um PIN válido de 6 caracteres.") }
      return
    }
    val playerName = state.displayNameInput.trim().ifEmpty { "Jogador" }

    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      val result = onlineRepository.joinRoom(rawRoomCode = code, displayName = playerName)
      result.fold(
        onSuccess = { joinedCode ->
          _uiState.update {
            it.copy(
              activeRoomCode = joinedCode,
              isLoading = false,
              errorMessage = null,
              typingInput = ""
            )
          }
          startObservingRoom(joinedCode)
        },
        onFailure = { error ->
          _uiState.update {
            it.copy(
              isLoading = false,
              errorMessage = error.localizedMessage ?: "Não foi possível entrar na sala."
            )
          }
        }
      )
    }
  }

  fun startGame() {
    val code = _uiState.value.activeRoomCode ?: return
    val room = _uiState.value.room ?: return
    if (room.hostId != currentUserId || _uiState.value.isSubmitting) return

    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
      onlineRepository.startQuestion(code, 0).onFailure { error ->
        _uiState.update {
          it.copy(errorMessage = error.localizedMessage ?: "Erro ao iniciar partida.")
        }
      }
      _uiState.update { it.copy(isSubmitting = false) }
    }
  }

  fun submitAnswer(rawAnswer: String, isSoundEnabled: Boolean, remainingSeconds: Int = 0) {
    val state = _uiState.value
    val code = state.activeRoomCode ?: return
    val room = state.room ?: return
    if (room.phase() != RoomPhase.QUESTION || state.isSubmitting) return
    val player = state.currentPlayer(currentUserId) ?: return
    val questionIdx = room.currentQuestionIndex.toInt()
    if (player.lastAnsweredIndex == room.currentQuestionIndex) return
    val question = currentQuestionFor(room) ?: return

    val cleaned = rawAnswer.trim()
    if (cleaned.isEmpty()) return

    val outcome = question.evaluateAnswer(cleaned)
    val clampedRemaining = remainingSeconds.coerceIn(0, ONLINE_QUESTION_SECONDS)
    val speedBonus = ((clampedRemaining.toFloat() / ONLINE_QUESTION_SECONDS.toFloat()) * 50f).toLong()
    val pointsAwarded = when (outcome) {
      AnswerOutcome.CORRECT -> 100L + speedBonus
      AnswerOutcome.MAYBE -> 50L + (speedBonus / 2L)
      AnswerOutcome.WRONG -> 0L
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
      val result = onlineRepository.submitPlayerAnswer(
        roomCode = code,
        currentPlayer = player,
        questionIndex = questionIdx,
        answer = cleaned,
        outcome = outcome,
        pointsAwarded = pointsAwarded
      )
      result.fold(
        onSuccess = {
          val current = _uiState.value
          val nextStreak = when (outcome) {
            AnswerOutcome.CORRECT -> current.currentStreak + 1
            AnswerOutcome.MAYBE -> current.currentStreak
            AnswerOutcome.WRONG -> 0
          }
          // Hold sound & streak update until the host reveals the answer (RoomPhase.REVEAL)
          pendingRevealQuestionIndex = questionIdx
          pendingRevealOutcome = outcome
          pendingRevealSoundEnabled = isSoundEnabled
          pendingNextStreak = nextStreak
          _uiState.update {
            it.copy(
              isSubmitting = false,
              lastPointsAwarded = pointsAwarded
            )
          }
        },
        onFailure = { error ->
          _uiState.update {
            it.copy(
              isSubmitting = false,
              errorMessage = error.localizedMessage ?: "Erro ao enviar resposta."
            )
          }
        }
      )
    }
  }

  fun revealAnswer() {
    val code = _uiState.value.activeRoomCode ?: return
    val room = _uiState.value.room ?: return
    if (room.hostId != currentUserId || _uiState.value.isSubmitting) return

    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
      onlineRepository.revealAnswer(code).onFailure { error ->
        _uiState.update {
          it.copy(errorMessage = error.localizedMessage ?: "Erro ao revelar gabarito.")
        }
      }
      _uiState.update { it.copy(isSubmitting = false) }
    }
  }

  fun nextQuestionOrFinish() {
    val code = _uiState.value.activeRoomCode ?: return
    val room = _uiState.value.room ?: return
    if (room.hostId != currentUserId || _uiState.value.isSubmitting) return

    val nextIndex = room.currentQuestionIndex.toInt() + 1
    viewModelScope.launch {
      _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
      val result = if (nextIndex < room.questionIds.size) {
        onlineRepository.startQuestion(code, nextIndex)
      } else {
        onlineRepository.finishRoom(code)
      }
      result.onFailure { error ->
        _uiState.update {
          it.copy(errorMessage = error.localizedMessage ?: "Erro ao avançar questão.")
        }
      }
      _uiState.update { it.copy(isSubmitting = false) }
    }
  }

  fun leaveRoom() {
    val code = _uiState.value.activeRoomCode
    stopObservingRoom()
    pendingRevealQuestionIndex = -1
    pendingRevealOutcome = null
    pendingNextStreak = null
    lastRevealedSoundIndex = -1
    _uiState.update {
      it.copy(
        activeRoomCode = null,
        room = null,
        players = emptyList(),
        isLoading = false,
        isSubmitting = false,
        errorMessage = null,
        typingInput = "",
        currentStreak = 0,
        bestStreak = 0,
        lastPointsAwarded = 0L
      )
    }
    if (code != null) {
      viewModelScope.launch {
        onlineRepository.leaveRoom(code)
      }
    }
  }

  private fun startObservingRoom(code: String) {
    stopObservingRoom()

    roomObserverJob = viewModelScope.launch {
      onlineRepository.observeRoom(code)
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          _uiState.update {
            it.copy(errorMessage = error.localizedMessage ?: "Conexão com a sala interrompida.")
          }
        }
        .collect { room ->
          val prevPhase = _uiState.value.room?.phase()
          val newPhase = room?.phase()
          val qIdx = room?.currentQuestionIndex?.toInt() ?: -1
          val enteredReveal = prevPhase != RoomPhase.REVEAL && newPhase == RoomPhase.REVEAL

          var streakToApply: Int? = null
          if (enteredReveal && qIdx >= 0 && lastRevealedSoundIndex != qIdx) {
            lastRevealedSoundIndex = qIdx
            val answeredThisRound = pendingRevealQuestionIndex == qIdx && pendingRevealOutcome != null
            val revealedOutcome = if (answeredThisRound) pendingRevealOutcome!! else AnswerOutcome.WRONG
            streakToApply = if (answeredThisRound) (pendingNextStreak ?: 0) else 0
            if (pendingRevealSoundEnabled) {
              _effects.tryEmit(OnlineQuizEffect.PlayAnswerSound(revealedOutcome))
            }
            pendingRevealQuestionIndex = -1
            pendingRevealOutcome = null
            pendingNextStreak = null
          }

          _uiState.update { current ->
            val questionChanged = current.room?.currentQuestionIndex != room?.currentQuestionIndex
            val enteredQuestion = current.room?.phase() != RoomPhase.QUESTION && room?.phase() == RoomPhase.QUESTION
            val updatedStreak = streakToApply ?: current.currentStreak
            current.copy(
              room = room,
              typingInput = if (questionChanged || enteredQuestion) "" else current.typingInput,
              currentStreak = updatedStreak,
              bestStreak = maxOf(current.bestStreak, updatedStreak),
              errorMessage = if (room == null) "A sala foi encerrada." else current.errorMessage
            )
          }
        }
    }

    playersObserverJob = viewModelScope.launch {
      onlineRepository.observePlayers(code)
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          _uiState.update {
            it.copy(errorMessage = error.localizedMessage ?: "Erro ao carregar participantes.")
          }
        }
        .collect { players ->
          _uiState.update { it.copy(players = players) }
        }
    }
  }

  private fun stopObservingRoom() {
    roomObserverJob?.cancel()
    playersObserverJob?.cancel()
    roomObserverJob = null
    playersObserverJob = null
  }

  override fun onCleared() {
    stopObservingRoom()
    super.onCleared()
  }

  companion object {
    const val ONLINE_QUESTION_SECONDS = 25
  }
}
