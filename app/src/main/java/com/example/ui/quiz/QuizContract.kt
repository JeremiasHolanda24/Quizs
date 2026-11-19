package com.example.ui.quiz

import com.example.ui.base.UiEffect
import com.example.ui.base.UiEvent
import com.example.ui.base.UiState

data class ModalGroupStat(
  val group: ModalGroup,
  val total: Int,
  val correct: Int,
  val maybe: Int,
  val wrong: Int
) {
  val accuracyPercent: Int
    get() = if (total > 0) ((correct.toFloat() / total.toFloat()) * 100f).toInt() else 0
}

data class AiTutorState(
  val isVisible: Boolean = false,
  val questionSentence: String = "",
  val isLoading: Boolean = false,
  val text: String = "",
  val errorMessage: String? = null
)

data class QuizUiState(
  val screenMode: QuizScreenMode = QuizScreenMode.SETUP,
  val isLightTheme: Boolean = false,
  val gameMode: GameMode = GameMode.SOLO,
  val soloNameInput: String = "",
  val groupNameInput: String = "",
  val players: List<Player> = emptyList(),
  val currentTurnIndex: Int = 0,
  val selectedGroups: Set<ModalGroup> = setOf(ModalGroup.ALL),
  val filteredQuestionCount: Int = 40,
  val activeQuestions: List<QuizQuestion> = emptyList(),
  val answersByQuestionId: Map<Int, AnswerRecord> = emptyMap(),
  val difficulty: QuizDifficulty = QuizDifficulty.EASY,
  val showContext: Boolean = true,
  val showCategory: Boolean = true,
  val isSoundEnabled: Boolean = true,
  val isSettingsDrawerOpen: Boolean = false,
  val highScore: Int = 0,
  val isNewHighScore: Boolean = false,
  val currentStreak: Int = 0,
  val bestStreak: Int = 0,
  val aiTutorState: AiTutorState = AiTutorState()
) : UiState {
  val answeredCount: Int
    get() = answersByQuestionId.size

  val totalActiveQuestions: Int
    get() = activeQuestions.size

  val currentPlayer: Player?
    get() = players.getOrNull(currentTurnIndex)

  val isQuizCompleted: Boolean
    get() = activeQuestions.isNotEmpty() && answeredCount >= activeQuestions.size

  val correctCount: Int
    get() = answersByQuestionId.values.count { it.outcome == AnswerOutcome.CORRECT }

  val maybeCount: Int
    get() = answersByQuestionId.values.count { it.outcome == AnswerOutcome.MAYBE }

  val incorrectCount: Int
    get() = answersByQuestionId.values.count { it.outcome == AnswerOutcome.WRONG }

  val reviewableMistakesCount: Int
    get() = answersByQuestionId.values.count { it.outcome != AnswerOutcome.CORRECT }

  val modalStats: List<ModalGroupStat>
    get() = activeQuestions
      .groupBy { it.group }
      .map { (group, questions) ->
        var c = 0
        var m = 0
        var w = 0
        questions.forEach { q ->
          when (answersByQuestionId[q.id]?.outcome) {
            AnswerOutcome.CORRECT -> c++
            AnswerOutcome.MAYBE -> m++
            AnswerOutcome.WRONG -> w++
            null -> Unit
          }
        }
        ModalGroupStat(group = group, total = questions.size, correct = c, maybe = m, wrong = w)
      }
      .sortedBy { it.accuracyPercent }

  val totalScore: Int
    get() = players.sumOf { it.score }
}

sealed interface QuizEvent : UiEvent {
  data object ToggleTheme : QuizEvent
  data class SelectGameMode(val mode: GameMode) : QuizEvent
  data class UpdateSoloName(val name: String) : QuizEvent
  data class UpdateGroupNameInput(val name: String) : QuizEvent
  data object AddGroupPlayer : QuizEvent
  data class RemovePlayer(val playerId: String) : QuizEvent
  data class ToggleModalGroup(val group: ModalGroup) : QuizEvent
  data class SelectDifficulty(val difficulty: QuizDifficulty) : QuizEvent
  data object StartQuiz : QuizEvent
  data class SubmitAnswer(
    val questionId: Int,
    val answer: String
  ) : QuizEvent
  data object ToggleSettingsDrawer : QuizEvent
  data object ToggleSound : QuizEvent
  data object ShuffleActiveQuestions : QuizEvent
  data object OpenResults : QuizEvent
  data object ReviewAnswers : QuizEvent
  data object PlayAgain : QuizEvent
  data object RetryMistakes : QuizEvent
  data object ResetToSetup : QuizEvent
  data class LoadHighScore(val score: Int) : QuizEvent
  data class AskAiTutor(val questionId: Int) : QuizEvent
  data object DismissAiTutor : QuizEvent
}

sealed interface QuizEffect : UiEffect {
  data object ScrollToTop : QuizEffect
  data class PlayAnswerSound(val outcome: AnswerOutcome) : QuizEffect
  data class ShowMessage(val message: String) : QuizEffect
}
