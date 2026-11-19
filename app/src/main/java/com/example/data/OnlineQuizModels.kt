package com.example.data

import androidx.annotation.Keep
import com.example.ui.quiz.QuizDifficulty
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class RoomPhase {
  LOBBY,
  QUESTION,
  REVEAL,
  FINISHED;

  companion object {
    fun fromRaw(raw: String): RoomPhase =
      entries.firstOrNull { it.name == raw } ?: LOBBY
  }
}

@Keep
@IgnoreExtraProperties
data class QuizRoom(
  @DocumentId val roomCode: String = "",
  val hostId: String = "",
  val hostName: String = "",
  val status: String = RoomPhase.LOBBY.name,
  val difficulty: String = QuizDifficulty.EASY.name,
  val questionIds: List<Long> = emptyList(),
  val currentQuestionIndex: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  fun phase(): RoomPhase = RoomPhase.fromRaw(status)

  fun parsedDifficulty(): QuizDifficulty =
    QuizDifficulty.entries.firstOrNull { it.name == difficulty } ?: QuizDifficulty.EASY
}

@Keep
@IgnoreExtraProperties
data class OnlinePlayer(
  @DocumentId val id: String = "",
  val userId: String = "",
  val roomCode: String = "",
  val displayName: String = "",
  val score: Long = 0L,
  val correctCount: Long = 0L,
  val incorrectCount: Long = 0L,
  val lastAnsweredIndex: Long = -1L,
  val lastAnswer: String = "",
  val lastAnswerCorrect: Boolean = false,
  @ServerTimestamp val joinedAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)
