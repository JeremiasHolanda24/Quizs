package com.example.data

import android.content.Context
import com.example.R
import com.example.ui.quiz.AnswerOutcome
import com.example.ui.quiz.QuizDifficulty
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class OnlineQuizRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("Você precisa entrar com o Google para jogar online.")

  fun generateRoomCode(): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    return buildString(6) {
      repeat(6) {
        append(chars[Random.nextInt(chars.length)])
      }
    }
  }

  fun observeRoom(roomCode: String): Flow<QuizRoom?> = flow {
    val uid = requireUserId()
    val code = roomCode.trim().uppercase()
    val path = "quiz_rooms/$code"
    emitAll(
      db.collection("quiz_rooms").document(code)
        .snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            snapshot.toObject(QuizRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          } else {
            null
          }
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val toLog = firestoreError ?: (error as? Exception)
          if (toLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(toLog, OperationType.GET, path)
          }
          throw error
        }
    )
  }

  fun observePlayers(roomCode: String): Flow<List<OnlinePlayer>> = flow {
    val uid = requireUserId()
    val code = roomCode.trim().uppercase()
    val path = "quiz_rooms/$code/players"
    emitAll(
      db.collection("quiz_rooms").document(code)
        .collection("players")
        .whereEqualTo("roomCode", code)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(OnlinePlayer::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedWith(compareByDescending<OnlinePlayer> { it.score }.thenBy { it.displayName })
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val toLog = firestoreError ?: (error as? Exception)
          if (toLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(toLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  suspend fun createRoom(
    hostName: String,
    difficulty: QuizDifficulty,
    questionIds: List<Int>
  ): Result<String> {
    val safeName = hostName.trim().take(40).ifEmpty { "Anfitrião" }
    val roomCode = generateRoomCode()
    val roomPath = "quiz_rooms/$roomCode"
    return try {
      val uid = requireUserId()
      require(questionIds.isNotEmpty() && questionIds.size <= 40) {
        "Selecione entre 1 e 40 questões."
      }
      val roomPayload = mapOf(
        "hostId" to uid,
        "hostName" to safeName,
        "status" to RoomPhase.LOBBY.name,
        "difficulty" to difficulty.name,
        "questionIds" to questionIds.map { it.toLong() },
        "currentQuestionIndex" to 0L,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      db.collection("quiz_rooms").document(roomCode).set(roomPayload).await()

      val playerPayload = mapOf(
        "userId" to uid,
        "roomCode" to roomCode,
        "displayName" to safeName,
        "score" to 0L,
        "correctCount" to 0L,
        "incorrectCount" to 0L,
        "lastAnsweredIndex" to -1L,
        "lastAnswer" to "",
        "lastAnswerCorrect" to false,
        "joinedAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      db.collection("quiz_rooms").document(roomCode)
        .collection("players").document(uid)
        .set(playerPayload)
        .await()

      Result.success(roomCode)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, roomPath)
      Result.failure(e)
    }
  }

  suspend fun joinRoom(rawRoomCode: String, displayName: String): Result<String> {
    val code = rawRoomCode.trim().uppercase()
    val safeName = displayName.trim().take(40).ifEmpty { "Jogador" }
    val roomPath = "quiz_rooms/$code"
    return try {
      val uid = requireUserId()
      require(code.length in 4..12 && code.matches(Regex("^[A-Z0-9_-]+$"))) {
        "PIN de sala inválido."
      }
      val roomSnap = db.collection("quiz_rooms").document(code).get().await()
      if (!roomSnap.exists()) {
        return Result.failure(IllegalArgumentException("Sala $code não encontrada."))
      }
      val room = roomSnap.toObject(QuizRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      if (room == null) {
        return Result.failure(IllegalArgumentException("Não foi possível carregar a sala $code."))
      }
      val playerRef = db.collection("quiz_rooms").document(code)
        .collection("players").document(uid)
      val existingPlayer = playerRef.get().await()
      if (!existingPlayer.exists()) {
        if (room.phase() != RoomPhase.LOBBY) {
          return Result.failure(IllegalStateException("Esta partida já foi iniciada."))
        }
        val playerPayload = mapOf(
          "userId" to uid,
          "roomCode" to code,
          "displayName" to safeName,
          "score" to 0L,
          "correctCount" to 0L,
          "incorrectCount" to 0L,
          "lastAnsweredIndex" to -1L,
          "lastAnswer" to "",
          "lastAnswerCorrect" to false,
          "joinedAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
        playerRef.set(playerPayload).await()
      }
      Result.success(code)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, roomPath)
      Result.failure(e)
    }
  }

  suspend fun startQuestion(roomCode: String, questionIndex: Int): Result<Unit> {
    val code = roomCode.trim().uppercase()
    val path = "quiz_rooms/$code"
    return try {
      requireUserId()
      db.collection("quiz_rooms").document(code)
        .update(
          mapOf(
            "status" to RoomPhase.QUESTION.name,
            "currentQuestionIndex" to questionIndex.toLong(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun revealAnswer(roomCode: String): Result<Unit> {
    val code = roomCode.trim().uppercase()
    val path = "quiz_rooms/$code"
    return try {
      requireUserId()
      db.collection("quiz_rooms").document(code)
        .update(
          mapOf(
            "status" to RoomPhase.REVEAL.name,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun finishRoom(roomCode: String): Result<Unit> {
    val code = roomCode.trim().uppercase()
    val path = "quiz_rooms/$code"
    return try {
      requireUserId()
      db.collection("quiz_rooms").document(code)
        .update(
          mapOf(
            "status" to RoomPhase.FINISHED.name,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun submitPlayerAnswer(
    roomCode: String,
    currentPlayer: OnlinePlayer,
    questionIndex: Int,
    answer: String,
    outcome: AnswerOutcome,
    pointsAwarded: Long
  ): Result<Unit> {
    val code = roomCode.trim().uppercase()
    var path = "quiz_rooms/$code/players"
    return try {
      val uid = requireUserId()
      path = "quiz_rooms/$code/players/$uid"
      val safeAnswer = answer.trim().take(60)
      val isCorrect = outcome == AnswerOutcome.CORRECT
      val nextScore = (currentPlayer.score + pointsAwarded.coerceAtLeast(0L)).coerceAtMost(100000L)
      val nextCorrect = (currentPlayer.correctCount + if (outcome == AnswerOutcome.CORRECT) 1L else 0L).coerceAtMost(40L)
      val nextIncorrect = (currentPlayer.incorrectCount + if (outcome == AnswerOutcome.WRONG) 1L else 0L).coerceAtMost(40L)

      db.collection("quiz_rooms").document(code)
        .collection("players").document(uid)
        .update(
          mapOf(
            "score" to nextScore,
            "correctCount" to nextCorrect,
            "incorrectCount" to nextIncorrect,
            "lastAnsweredIndex" to questionIndex.toLong(),
            "lastAnswer" to safeAnswer,
            "lastAnswerCorrect" to isCorrect,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun leaveRoom(roomCode: String): Result<Unit> {
    val code = roomCode.trim().uppercase()
    var path = "quiz_rooms/$code/players"
    return try {
      val uid = requireUserId()
      path = "quiz_rooms/$code/players/$uid"
      db.collection("quiz_rooms").document(code)
        .collection("players").document(uid)
        .delete()
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, path)
      Result.failure(e)
    }
  }
}
