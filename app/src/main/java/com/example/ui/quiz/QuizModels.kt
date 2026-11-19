package com.example.ui.quiz

enum class GameMode {
  SOLO,
  GROUP
}

enum class QuizScreenMode {
  SETUP,
  QUIZ,
  RESULTS
}

enum class QuizDifficulty {
  EASY,
  NORMAL
}

enum class ModalGroup(val id: String, val label: String) {
  ALL("all", "Todas"),
  CAN("can", "Can / Can't"),
  COULD("could", "Could / Couldn't"),
  MAY("may", "May"),
  MIGHT("might", "Might"),
  MUST("must", "Must / Mustn't"),
  HAVE_TO("have to", "Have to / Don't have to"),
  SHOULD("should", "Should / Shouldn't"),
  WOULD("would", "Would")
}

enum class QuestionType {
  CHOICE,
  TYPING
}

enum class AnswerOutcome {
  CORRECT,
  MAYBE,
  WRONG
}

data class ModalDefinition(
  val key: String,
  val name: String,
  val role: String,
  val structure: String,
  val contrast: String
)

data class QuizQuestion(
  val id: Int,
  val group: ModalGroup,
  val category: String,
  val context: String,
  val sentence: String,
  val type: QuestionType,
  val options: List<String> = emptyList(),
  val acceptedAnswers: List<String>,
  val reason: String,
  val maybeAnswers: List<String> = emptyList(),
  val maybeReason: String = ""
) {
  fun evaluateAnswer(rawAnswer: String): AnswerOutcome {
    val normalized = rawAnswer.trim().lowercase()
    return when {
      acceptedAnswers.any { it.trim().lowercase() == normalized } -> AnswerOutcome.CORRECT
      maybeAnswers.any { it.trim().lowercase() == normalized } -> AnswerOutcome.MAYBE
      else -> AnswerOutcome.WRONG
    }
  }
}

data class Player(
  val id: String,
  val name: String,
  val score: Int = 0
)

data class AnswerRecord(
  val questionId: Int,
  val submittedAnswer: String,
  val outcome: AnswerOutcome,
  val correctModal: ModalDefinition,
  val submittedModal: ModalDefinition?,
  val xpEarned: Int = 0
) {
  val isCorrect: Boolean get() = outcome == AnswerOutcome.CORRECT
  val isMaybe: Boolean get() = outcome == AnswerOutcome.MAYBE
}
