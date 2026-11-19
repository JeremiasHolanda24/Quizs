package com.example.ui.quiz

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Help
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.OnlinePlayer
import com.example.data.QuizRepository
import com.example.data.QuizRoom
import com.example.data.RoomPhase
import com.example.ui.theme.QuizTheme
import kotlinx.coroutines.launch

@Composable
fun OnlineRoomScreen(
  uiState: OnlineQuizUiState,
  currentUserId: String,
  quizRepository: QuizRepository,
  isLightTheme: Boolean,
  isSoundEnabled: Boolean,
  onToggleTheme: () -> Unit,
  onToggleSound: () -> Unit,
  onStartGame: () -> Unit,
  onUpdateTyping: (String) -> Unit,
  onSubmitAnswer: (String, Int) -> Unit,
  onRevealAnswer: () -> Unit,
  onNextOrFinish: () -> Unit,
  onLeaveRoom: () -> Unit,
  modifier: Modifier = Modifier
) {
  BackHandler(onBack = onLeaveRoom)

  val extendedColors = QuizTheme.colors
  val snackbarHostState = remember { SnackbarHostState() }
  val clipboardManager = LocalClipboardManager.current
  val scope = rememberCoroutineScope()

  val room = uiState.room
  val phase = room?.phase() ?: RoomPhase.LOBBY
  val isHost = uiState.isHost(currentUserId)
  val currentPlayer = uiState.currentPlayer(currentUserId)
  val roomCode = uiState.activeRoomCode.orEmpty()

  val backgroundBrush = Brush.verticalGradient(
    colors = listOf(
      MaterialTheme.colorScheme.background,
      MaterialTheme.colorScheme.background,
      MaterialTheme.colorScheme.primary.copy(alpha = if (isLightTheme) 0.06f else 0.12f)
    )
  )

  val flashAlpha = remember { Animatable(0f) }
  var flashOutcome by remember { mutableStateOf<AnswerOutcome?>(null) }
  val myLastAnsweredIndex = currentPlayer?.lastAnsweredIndex ?: -1L
  val myLastAnswerText = currentPlayer?.lastAnswer.orEmpty()
  val myLastAnswerCorrect = currentPlayer?.lastAnswerCorrect ?: false
  val currentQIdx = room?.currentQuestionIndex ?: -1L
  val currentRoundQuestion = room?.questionIds?.getOrNull(currentQIdx.toInt())?.toInt()?.let { qId ->
    quizRepository.allQuestions.firstOrNull { it.id == qId }
  }

  LaunchedEffect(phase, currentQIdx) {
    if (phase == RoomPhase.REVEAL && currentQIdx >= 0L) {
      val answeredThisRound = myLastAnsweredIndex == currentQIdx
      flashOutcome = if (answeredThisRound) {
        currentRoundQuestion?.evaluateAnswer(myLastAnswerText)
          ?: if (myLastAnswerCorrect) AnswerOutcome.CORRECT else AnswerOutcome.WRONG
      } else {
        AnswerOutcome.WRONG
      }
      flashAlpha.snapTo(0f)
      flashAlpha.animateTo(1f, animationSpec = tween(durationMillis = 220))
      kotlinx.coroutines.delay(180L)
      flashAlpha.animateTo(0f, animationSpec = tween(durationMillis = 1200))
    }
  }

  Box(modifier = modifier.fillMaxSize()) {
  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = Color.Transparent,
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    topBar = {
      OnlineRoomTopBar(
        roomCode = roomCode,
        phase = phase,
        currentQuestionNumber = uiState.currentQuestionNumber,
        totalQuestions = uiState.totalQuestions,
        answeredCount = uiState.answeredCount,
        totalPlayers = uiState.players.size,
        isLightTheme = isLightTheme,
        isSoundEnabled = isSoundEnabled,
        onToggleTheme = onToggleTheme,
        onToggleSound = onToggleSound,
        onLeaveRoom = onLeaveRoom
      )
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(backgroundBrush)
        .padding(innerPadding)
        .navigationBarsPadding(),
      contentAlignment = Alignment.TopCenter
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .widthIn(max = 720.dp)
          .padding(horizontal = 16.dp)
      ) {
        if (uiState.errorMessage != null) {
          Spacer(modifier = Modifier.height(10.dp))
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = extendedColors.errorBg,
            border = BorderStroke(1.dp, extendedColors.errorBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = uiState.errorMessage,
              style = MaterialTheme.typography.bodySmall,
              color = extendedColors.errorText,
              modifier = Modifier.padding(12.dp),
              textAlign = TextAlign.Center
            )
          }
        }

        if (room == null) {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = "Conectando à sala $roomCode…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        } else {
          AnimatedContent(
            targetState = phase,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "online_room_phase",
            modifier = Modifier.fillMaxSize()
          ) { currentPhase ->
            when (currentPhase) {
              RoomPhase.LOBBY -> {
                OnlineLobbyContent(
                  room = room,
                  players = uiState.players,
                  currentUserId = currentUserId,
                  isHost = isHost,
                  isSubmitting = uiState.isSubmitting,
                  onCopyPin = {
                    clipboardManager.setText(AnnotatedString(roomCode))
                    scope.launch {
                      snackbarHostState.showSnackbar("PIN $roomCode copiado!")
                    }
                  },
                  onStartGame = onStartGame,
                  onLeaveRoom = onLeaveRoom
                )
              }

              RoomPhase.QUESTION -> {
                val question = quizRepository.allQuestions.firstOrNull {
                  it.id == room.questionIds.getOrNull(room.currentQuestionIndex.toInt())?.toInt()
                }
                if (question != null) {
                  OnlineQuestionContent(
                    room = room,
                    question = question,
                    hasAnswered = uiState.hasAnsweredCurrent(currentUserId),
                    myLastAnswer = currentPlayer?.lastAnswer.orEmpty(),
                    currentStreak = uiState.currentStreak,
                    typingInput = uiState.typingInput,
                    answeredCount = uiState.answeredCount,
                    totalPlayers = uiState.players.size,
                    isHost = isHost,
                    isSubmitting = uiState.isSubmitting,
                    onUpdateTyping = onUpdateTyping,
                    onSubmitAnswer = onSubmitAnswer,
                    onRevealAnswer = onRevealAnswer
                  )
                }
              }

              RoomPhase.REVEAL -> {
                val question = quizRepository.allQuestions.firstOrNull {
                  it.id == room.questionIds.getOrNull(room.currentQuestionIndex.toInt())?.toInt()
                }
                if (question != null) {
                  OnlineRevealContent(
                    room = room,
                    question = question,
                    players = uiState.players,
                    currentPlayer = currentPlayer,
                    currentUserId = currentUserId,
                    lastPointsAwarded = uiState.lastPointsAwarded,
                    currentStreak = uiState.currentStreak,
                    quizRepository = quizRepository,
                    isHost = isHost,
                    isSubmitting = uiState.isSubmitting,
                    isLastQuestion = room.currentQuestionIndex.toInt() + 1 >= room.questionIds.size,
                    onNextOrFinish = onNextOrFinish
                  )
                }
              }

              RoomPhase.FINISHED -> {
                OnlinePodiumContent(
                  players = uiState.players,
                  currentUserId = currentUserId,
                  onBackHome = onLeaveRoom
                )
              }
            }
          }
        }
      }
    }
  }

  AnswerEdgeFlashOverlay(
    outcome = flashOutcome,
    alpha = flashAlpha.value
  )
  }
}

@Composable
private fun OnlineRoomTopBar(
  roomCode: String,
  phase: RoomPhase,
  currentQuestionNumber: Int,
  totalQuestions: Int,
  answeredCount: Int,
  totalPlayers: Int,
  isLightTheme: Boolean,
  isSoundEnabled: Boolean,
  onToggleTheme: () -> Unit,
  onToggleSound: () -> Unit,
  onLeaveRoom: () -> Unit
) {
  val extendedColors = QuizTheme.colors
  val showProgress = phase == RoomPhase.QUESTION || phase == RoomPhase.REVEAL
  val targetProgress = if (totalQuestions > 0) {
    currentQuestionNumber.toFloat() / totalQuestions.toFloat()
  } else {
    0f
  }
  val animatedProgress by animateFloatAsState(
    targetValue = targetProgress,
    animationSpec = tween(durationMillis = 400),
    label = "online_progress_bar"
  )

  Surface(
    color = extendedColors.cardBackground,
    tonalElevation = 2.dp,
    shadowElevation = 4.dp,
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = extendedColors.accentBadgeBg,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
          ) {
            Text(
              text = "PIN $roomCode",
              style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
              ),
              color = extendedColors.accentBadgeText,
              modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("online_topbar_pin")
            )
          }

          if (showProgress) {
            Text(
              text = "Q$currentQuestionNumber/$totalQuestions",
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = onToggleSound,
            modifier = Modifier
              .size(48.dp)
              .testTag("online_sound_toggle")
          ) {
            Icon(
              imageVector = if (isSoundEnabled) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
              contentDescription = stringResource(R.string.toggle_sound_desc),
              tint = MaterialTheme.colorScheme.onSurface
            )
          }
          IconButton(
            onClick = onToggleTheme,
            modifier = Modifier
              .size(48.dp)
              .testTag("online_theme_toggle")
          ) {
            Icon(
              imageVector = if (isLightTheme) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
              contentDescription = stringResource(R.string.toggle_theme_desc),
              tint = MaterialTheme.colorScheme.onSurface
            )
          }
          IconButton(
            onClick = onLeaveRoom,
            modifier = Modifier
              .size(48.dp)
              .testTag("online_leave_icon_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ExitToApp,
              contentDescription = stringResource(R.string.online_leave_room),
              tint = MaterialTheme.colorScheme.error
            )
          }
        }
      }

      if (showProgress) {
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
          progress = { animatedProgress },
          modifier = Modifier
            .fillMaxWidth()
            .height(7.dp)
            .clip(RoundedCornerShape(4.dp)),
          color = MaterialTheme.colorScheme.primary,
          trackColor = extendedColors.surfaceElevated,
          strokeCap = StrokeCap.Round
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "Questão $currentQuestionNumber de $totalQuestions",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = "$answeredCount/$totalPlayers responderam",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary
          )
        }
      }
    }
  }
}

@Composable
private fun OnlineLobbyContent(
  room: QuizRoom,
  players: List<OnlinePlayer>,
  currentUserId: String,
  isHost: Boolean,
  isSubmitting: Boolean,
  onCopyPin: () -> Unit,
  onStartGame: () -> Unit,
  onLeaveRoom: () -> Unit
) {
  val extendedColors = QuizTheme.colors

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Surface(
        shape = RoundedCornerShape(22.dp),
        color = extendedColors.cardBackground,
        border = BorderStroke(1.dp, extendedColors.border),
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = "PIN DA SALA",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Text(
              text = room.roomCode,
              style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp
              ),
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.testTag("lobby_pin_code")
            )
            Surface(
              shape = CircleShape,
              color = extendedColors.accentBadgeBg,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            ) {
              IconButton(
                onClick = onCopyPin,
                modifier = Modifier
                  .size(48.dp)
                  .testTag("copy_pin_button")
              ) {
                Icon(
                  imageVector = Icons.Rounded.ContentCopy,
                  contentDescription = stringResource(R.string.online_copy_pin_desc),
                  tint = MaterialTheme.colorScheme.primary
                )
              }
            }
          }
          Spacer(modifier = Modifier.height(14.dp))
          QrCodeImage(
            content = "modalsquiz://join?pin=${room.roomCode}",
            contentDescription = "QR Code para entrar na sala ${room.roomCode}",
            size = 156.dp,
            modifier = Modifier.testTag("lobby_qr_code")
          )
          Spacer(modifier = Modifier.height(10.dp))
          val diffLabel = if (room.parsedDifficulty() == QuizDifficulty.EASY) "Fácil" else "Normal"
          Text(
            text = "${room.questionIds.size} questões • Dificuldade $diffLabel",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Escaneie o QR Code ou compartilhe o PIN com seus amigos para entrarem na sala.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
          )
        }
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(
            imageVector = Icons.Rounded.Groups,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
          )
          Text(
            text = "PARTICIPANTES (${players.size})",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    itemsIndexed(players, key = { _, p -> p.id.ifEmpty { p.userId } }) { index, player ->
      val isMe = player.userId == currentUserId || player.id == currentUserId
      val isRoomHost = player.userId == room.hostId
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = extendedColors.cardBackground,
        border = BorderStroke(
          width = if (isMe) 1.5.dp else 1.dp,
          color = if (isMe) MaterialTheme.colorScheme.primary else extendedColors.border
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Surface(
              shape = CircleShape,
              color = extendedColors.accentBadgeBg,
              modifier = Modifier.size(34.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(
                  text = "${index + 1}",
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                  color = extendedColors.accentBadgeText
                )
              }
            }
            Column {
              Text(
                text = if (isMe) "${player.displayName} (Você)" else player.displayName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
              )
              if (isRoomHost) {
                Text(
                  text = "Anfitrião",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary
                )
              }
            }
          }
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = extendedColors.successBg
          ) {
            Text(
              text = "Pronto",
              style = MaterialTheme.typography.labelSmall,
              color = extendedColors.successText,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(6.dp))
      if (isHost) {
        Button(
          onClick = onStartGame,
          enabled = !isSubmitting && players.isNotEmpty(),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("online_start_match_button")
        ) {
          if (isSubmitting) {
            CircularProgressIndicator(
              modifier = Modifier.size(22.dp),
              color = Color.White,
              strokeWidth = 2.5.dp
            )
          } else {
            Icon(
              imageVector = Icons.Rounded.PlayArrow,
              contentDescription = null,
              modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = stringResource(R.string.online_start_match),
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
          }
        }
      } else {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = extendedColors.accentBadgeBg,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Icon(
              imageVector = Icons.Rounded.HourglassTop,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Aguardando ${room.hostName} iniciar a partida…",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
              color = extendedColors.accentBadgeText
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      OutlinedButton(
        onClick = onLeaveRoom,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, extendedColors.border),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("lobby_leave_button")
      ) {
        Text(
          text = stringResource(R.string.online_leave_room),
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurface
        )
      }
    }
  }
}

@Composable
private fun OnlineQuestionContent(
  room: QuizRoom,
  question: QuizQuestion,
  hasAnswered: Boolean,
  myLastAnswer: String,
  currentStreak: Int,
  typingInput: String,
  answeredCount: Int,
  totalPlayers: Int,
  isHost: Boolean,
  isSubmitting: Boolean,
  onUpdateTyping: (String) -> Unit,
  onSubmitAnswer: (String, Int) -> Unit,
  onRevealAnswer: () -> Unit
) {
  val extendedColors = QuizTheme.colors
  val showCategory = room.parsedDifficulty() == QuizDifficulty.EASY
  val totalSeconds = OnlineQuizViewModel.ONLINE_QUESTION_SECONDS
  var remainingSeconds by remember(room.currentQuestionIndex) { mutableIntStateOf(totalSeconds) }

  LaunchedEffect(room.currentQuestionIndex, hasAnswered) {
    if (!hasAnswered) {
      while (remainingSeconds > 0) {
        kotlinx.coroutines.delay(1000L)
        remainingSeconds--
      }
    }
  }

  val isTimeUp = remainingSeconds <= 0 && !hasAnswered
  val timerColor = when {
    remainingSeconds <= 5 -> extendedColors.errorBorder
    remainingSeconds <= 12 -> extendedColors.maybeBorder
    else -> extendedColors.successBorder
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = extendedColors.cardBackground,
        border = BorderStroke(1.dp, extendedColors.border),
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // Kahoot-style countdown timer + streak row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(
                imageVector = Icons.Rounded.Timer,
                contentDescription = null,
                tint = timerColor,
                modifier = Modifier.size(18.dp)
              )
              Text(
                text = if (isTimeUp) "Tempo esgotado!" else "${remainingSeconds}s • Bônus rápido",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = timerColor,
                modifier = Modifier.testTag("online_timer_label")
              )
            }
            if (currentStreak >= 2) {
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = extendedColors.maybeBg,
                border = BorderStroke(1.dp, extendedColors.maybeBorder)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                  Icon(
                    imageVector = Icons.Rounded.LocalFireDepartment,
                    contentDescription = null,
                    tint = extendedColors.maybeBorder,
                    modifier = Modifier.size(14.dp)
                  )
                  Text(
                    text = "${currentStreak}x",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = extendedColors.maybeText
                  )
                }
              }
            }
          }

          LinearProgressIndicator(
            progress = { (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f) },
            modifier = Modifier
              .fillMaxWidth()
              .height(7.dp)
              .clip(RoundedCornerShape(4.dp)),
            color = timerColor,
            trackColor = extendedColors.surfaceElevated,
            strokeCap = StrokeCap.Round,
            drawStopIndicator = {}
          )

          if (showCategory) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = extendedColors.categoryBadgeBg
            ) {
              Text(
                text = question.category,
                style = MaterialTheme.typography.labelMedium,
                color = extendedColors.categoryBadgeText,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
              )
            }
          }

          Surface(
            shape = RoundedCornerShape(12.dp),
            color = extendedColors.surfaceElevated,
            border = BorderStroke(1.dp, extendedColors.border),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = question.context,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(12.dp)
            )
          }

          Text(
            text = formatSentenceWithBlank(
              sentence = question.sentence,
              highlightColor = MaterialTheme.colorScheme.primary
            ),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )

          if (hasAnswered) {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = extendedColors.accentBadgeBg,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Icon(
                  imageVector = Icons.Rounded.CheckCircle,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(28.dp)
                )
                Text(
                  text = "Resposta enviada: \"$myLastAnswer\"",
                  style = MaterialTheme.typography.titleSmall,
                  color = extendedColors.accentBadgeText
                )
                Text(
                  text = "Aguardando o anfitrião revelar o gabarito ($answeredCount/$totalPlayers)…",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = TextAlign.Center
                )
              }
            }
          } else if (isTimeUp) {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = extendedColors.errorBg,
              border = BorderStroke(1.dp, extendedColors.errorBorder),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = "Tempo esgotado! Aguardando o anfitrião revelar o gabarito…",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = extendedColors.errorText,
                textAlign = TextAlign.Center,
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(16.dp)
              )
            }
          } else {
            when (question.type) {
              QuestionType.CHOICE -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                  question.options.chunked(2).forEach { pair ->
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                      pair.forEach { option ->
                        Surface(
                          onClick = { onSubmitAnswer(option, remainingSeconds) },
                          enabled = !isSubmitting,
                          shape = RoundedCornerShape(14.dp),
                          color = extendedColors.surfaceElevated,
                          border = BorderStroke(1.5.dp, extendedColors.border),
                          modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("online_option_$option")
                        ) {
                          Box(
                            modifier = Modifier
                              .fillMaxWidth()
                              .padding(horizontal = 12.dp, vertical = 18.dp),
                            contentAlignment = Alignment.Center
                          ) {
                            Text(
                              text = option,
                              style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                              ),
                              color = MaterialTheme.colorScheme.onSurface,
                              textAlign = TextAlign.Center
                            )
                          }
                        }
                      }
                    }
                  }
                }
              }

              QuestionType.TYPING -> {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  OutlinedTextField(
                    value = typingInput,
                    onValueChange = onUpdateTyping,
                    placeholder = {
                      Text(
                        text = stringResource(R.string.typing_placeholder),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                      focusedContainerColor = extendedColors.inputBackground,
                      unfocusedContainerColor = extendedColors.inputBackground,
                      focusedBorderColor = MaterialTheme.colorScheme.primary,
                      unfocusedBorderColor = extendedColors.border
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                      onDone = { onSubmitAnswer(typingInput, remainingSeconds) }
                    ),
                    modifier = Modifier
                      .weight(1f)
                      .testTag("online_typing_input")
                  )

                  Button(
                    onClick = { onSubmitAnswer(typingInput, remainingSeconds) },
                    enabled = !isSubmitting && typingInput.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                      .height(56.dp)
                      .testTag("online_typing_submit_button")
                  ) {
                    Text(
                      text = "Enviar",
                      style = MaterialTheme.typography.labelLarge
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    if (isHost) {
      item {
        Button(
          onClick = onRevealAnswer,
          enabled = !isSubmitting,
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("online_reveal_button")
        ) {
          Icon(
            imageVector = Icons.Rounded.Visibility,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = stringResource(R.string.online_reveal_answer),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
        }
      }
    }
  }
}

@Composable
private fun OnlineRevealContent(
  room: QuizRoom,
  question: QuizQuestion,
  players: List<OnlinePlayer>,
  currentPlayer: OnlinePlayer?,
  currentUserId: String,
  lastPointsAwarded: Long,
  currentStreak: Int,
  quizRepository: QuizRepository,
  isHost: Boolean,
  isSubmitting: Boolean,
  isLastQuestion: Boolean,
  onNextOrFinish: () -> Unit
) {
  val extendedColors = QuizTheme.colors
  val answeredThisRound = currentPlayer?.lastAnsweredIndex == room.currentQuestionIndex
  val roundOutcome = if (answeredThisRound) {
    question.evaluateAnswer(currentPlayer?.lastAnswer.orEmpty())
  } else {
    AnswerOutcome.WRONG
  }
  val primaryAnswer = question.acceptedAnswers.firstOrNull().orEmpty()
  val definition = quizRepository.getModalDefinition(primaryAnswer, question.category)
  val shownCorrectPts = if (lastPointsAwarded > 0L) lastPointsAwarded else 100L
  val shownMaybePts = if (lastPointsAwarded > 0L) lastPointsAwarded else 50L
  val streakSuffix = if (currentStreak >= 2) " • 🔥 ${currentStreak}x" else ""

  val bannerBg = when (roundOutcome) {
    AnswerOutcome.CORRECT -> extendedColors.successBg
    AnswerOutcome.MAYBE -> extendedColors.maybeBg
    AnswerOutcome.WRONG -> extendedColors.errorBg
  }
  val bannerBorder = when (roundOutcome) {
    AnswerOutcome.CORRECT -> extendedColors.successBorder
    AnswerOutcome.MAYBE -> extendedColors.maybeBorder
    AnswerOutcome.WRONG -> extendedColors.errorBorder
  }
  val bannerText = when (roundOutcome) {
    AnswerOutcome.CORRECT -> extendedColors.successText
    AnswerOutcome.MAYBE -> extendedColors.maybeText
    AnswerOutcome.WRONG -> extendedColors.errorText
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = bannerBg,
        border = BorderStroke(
          width = 1.5.dp,
          color = bannerBorder
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Icon(
            imageVector = when (roundOutcome) {
              AnswerOutcome.CORRECT -> Icons.Rounded.CheckCircle
              AnswerOutcome.MAYBE -> Icons.Rounded.Help
              AnswerOutcome.WRONG -> Icons.Rounded.Cancel
            },
            contentDescription = null,
            tint = bannerText,
            modifier = Modifier.size(32.dp)
          )
          Column {
            Text(
              text = when {
                roundOutcome == AnswerOutcome.CORRECT -> "Você acertou! +${shownCorrectPts} pts$streakSuffix"
                roundOutcome == AnswerOutcome.MAYBE -> "Talvez! Depende do contexto (+${shownMaybePts} pts)"
                answeredThisRound -> "Resposta incorreta (${currentPlayer?.lastAnswer})"
                else -> "Tempo encerrado — sem resposta"
              },
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = bannerText
            )
            Text(
              text = "Gabarito ideal: ${question.acceptedAnswers.joinToString(" / ")}",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }
    }

    item {
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = extendedColors.cardBackground,
        border = BorderStroke(1.dp, extendedColors.border),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = question.sentence.replace("___", primaryAnswer),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
          )
          if (roundOutcome == AnswerOutcome.MAYBE && question.maybeReason.isNotBlank()) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = extendedColors.maybeBg,
              border = BorderStroke(1.dp, extendedColors.maybeBorder),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = "Por que \"${currentPlayer?.lastAnswer}\" pode estar certo: ${question.maybeReason}",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = extendedColors.maybeText,
                modifier = Modifier.padding(10.dp)
              )
            }
          }
          Text(
            text = question.reason,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
          )
          HorizontalDivider(color = extendedColors.border)
          Text(
            text = "${definition.name}: ${definition.role}",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Estrutura: ${definition.structure}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          if (definition.contrast.isNotBlank()) {
            Text(
              text = "Dica: ${definition.contrast}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }

    item {
      Text(
        text = "PLACAR AO VIVO",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }

    itemsIndexed(players, key = { _, p -> p.id.ifEmpty { p.userId } }) { index, player ->
      OnlineLeaderboardRow(
        rank = index + 1,
        player = player,
        isCurrentUser = player.userId == currentUserId || player.id == currentUserId,
        currentQuestionIndex = room.currentQuestionIndex
      )
    }

    item {
      Spacer(modifier = Modifier.height(6.dp))
      if (isHost) {
        Button(
          onClick = onNextOrFinish,
          enabled = !isSubmitting,
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("online_next_question_button")
        ) {
          Text(
            text = stringResource(
              if (isLastQuestion) R.string.online_view_podium else R.string.online_next_question
            ),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Icon(
            imageVector = if (isLastQuestion) Icons.Rounded.EmojiEvents else Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
          )
        }
      } else {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = extendedColors.accentBadgeBg,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "Aguardando ${room.hostName} avançar para a próxima questão…",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = extendedColors.accentBadgeText,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(14.dp)
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnlinePodiumContent(
  players: List<OnlinePlayer>,
  currentUserId: String,
  onBackHome: () -> Unit
) {
  val extendedColors = QuizTheme.colors
  var showRankingSheet by remember { mutableStateOf(false) }
  val top3 = players.take(3)
  val first = top3.getOrNull(0)
  val second = top3.getOrNull(1)
  val third = top3.getOrNull(2)

  val myIndex = players.indexOfFirst { it.userId == currentUserId || it.id == currentUserId }
  val myPlayer = players.getOrNull(myIndex)
  val myRank = if (myIndex >= 0) myIndex + 1 else null

  Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(top = 20.dp, bottom = 96.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      item {
        Surface(
          shape = RoundedCornerShape(22.dp),
          color = extendedColors.cardBackground,
          border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
          shadowElevation = 8.dp,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 18.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              imageVector = Icons.Rounded.EmojiEvents,
              contentDescription = null,
              tint = Color(0xFFF59E0B),
              modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Pódio dos Vencedores",
              style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
              color = MaterialTheme.colorScheme.primary,
              textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Top 3 melhores pontuações da sala",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(22.dp))

            // 3-step visual podium: 2nd (left) | 1st (center) | 3rd (right)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalAlignment = Alignment.Bottom
            ) {
              PodiumColumn(
                rank = 2,
                player = second,
                barHeightDp = 96,
                medalColor = Color(0xFF94A3B8),
                isCurrentUser = second != null && (second.userId == currentUserId || second.id == currentUserId),
                modifier = Modifier.weight(1f)
              )
              PodiumColumn(
                rank = 1,
                player = first,
                barHeightDp = 132,
                medalColor = Color(0xFFF59E0B),
                isCurrentUser = first != null && (first.userId == currentUserId || first.id == currentUserId),
                modifier = Modifier.weight(1.15f)
              )
              PodiumColumn(
                rank = 3,
                player = third,
                barHeightDp = 76,
                medalColor = Color(0xFFD97706),
                isCurrentUser = third != null && (third.userId == currentUserId || third.id == currentUserId),
                modifier = Modifier.weight(1f)
              )
            }
          }
        }
      }

      if (myPlayer != null && myRank != null) {
        item {
          Surface(
            onClick = { showRankingSheet = true },
            shape = RoundedCornerShape(16.dp),
            color = extendedColors.accentBadgeBg,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("online_my_rank_summary_card")
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column {
                Text(
                  text = "Sua posição: ${myRank}º lugar",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = extendedColors.accentBadgeText
                )
                Text(
                  text = "${myPlayer.score} pts • ${myPlayer.correctCount} acertos • ${myPlayer.incorrectCount} erros",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Icon(
                imageVector = Icons.Rounded.Leaderboard,
                contentDescription = "Ver ranking completo",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
              )
            }
          }
        }
      }

      item {
        Button(
          onClick = onBackHome,
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("online_podium_home_button")
        ) {
          Text(
            text = stringResource(R.string.online_back_home),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
        }
      }

      item {
        BookReferenceFooter()
      }
    }

    // Floating Ranking icon button on the right side
    FloatingActionButton(
      onClick = { showRankingSheet = true },
      containerColor = MaterialTheme.colorScheme.primary,
      contentColor = Color.White,
      shape = CircleShape,
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(bottom = 20.dp, end = 4.dp)
        .testTag("online_podium_ranking_fab")
    ) {
      Icon(
        imageVector = Icons.Rounded.Leaderboard,
        contentDescription = "Abrir ranking completo"
      )
    }

    ConfettiOverlay(modifier = Modifier.fillMaxSize())
  }

  if (showRankingSheet) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
      onDismissRequest = { showRankingSheet = false },
      sheetState = sheetState,
      containerColor = extendedColors.cardBackground
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp)
          .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "Ranking Completo",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
          color = MaterialTheme.colorScheme.onSurface
        )

        if (myPlayer != null && myRank != null) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = extendedColors.accentBadgeBg,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = "Você está em ${myRank}º de ${players.size}",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = extendedColors.accentBadgeText
                )
                Text(
                  text = "${myPlayer.correctCount} acertos • ${myPlayer.incorrectCount} erros",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Text(
                text = "${myPlayer.score} pts",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.primary
              )
            }
          }
        }

        LazyColumn(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          itemsIndexed(players, key = { _, p -> p.id.ifEmpty { p.userId } }) { index, player ->
            OnlineLeaderboardRow(
              rank = index + 1,
              player = player,
              isCurrentUser = player.userId == currentUserId || player.id == currentUserId,
              currentQuestionIndex = null
            )
          }
        }
      }
    }
  }
}

@Composable
private fun PodiumColumn(
  rank: Int,
  player: OnlinePlayer?,
  barHeightDp: Int,
  medalColor: Color,
  isCurrentUser: Boolean,
  modifier: Modifier = Modifier
) {
  val extendedColors = QuizTheme.colors
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Bottom
  ) {
    if (player != null) {
      Text(
        text = if (isCurrentUser) "${player.displayName} (Você)" else player.displayName,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        maxLines = 2
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = "${player.score} pts",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.primary
      )
      Spacer(modifier = Modifier.height(8.dp))
    } else {
      Text(
        text = "—",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(8.dp))
    }

    Surface(
      shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 6.dp, bottomEnd = 6.dp),
      color = if (player != null) medalColor.copy(alpha = 0.22f) else extendedColors.surfaceElevated,
      border = BorderStroke(
        width = if (rank == 1 && player != null) 2.dp else 1.dp,
        color = if (player != null) medalColor else extendedColors.border
      ),
      modifier = Modifier
        .fillMaxWidth()
        .height(barHeightDp.dp)
    ) {
      Box(contentAlignment = Alignment.Center) {
        Text(
          text = "${rank}º",
          style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
          color = if (player != null) medalColor else MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
private fun OnlineLeaderboardRow(
  rank: Int,
  player: OnlinePlayer,
  isCurrentUser: Boolean,
  currentQuestionIndex: Long?
) {
  val extendedColors = QuizTheme.colors
  val rankBadgeColor = when (rank) {
    1 -> MaterialTheme.colorScheme.primary
    2 -> extendedColors.categoryBadgeText
    else -> extendedColors.surfaceElevated
  }
  val rankTextColor = if (rank <= 2) Color.White else MaterialTheme.colorScheme.onSurface

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = extendedColors.cardBackground,
    border = BorderStroke(
      width = if (isCurrentUser) 1.5.dp else 1.dp,
      color = if (isCurrentUser) MaterialTheme.colorScheme.primary else extendedColors.border
    ),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Surface(
          shape = CircleShape,
          color = rankBadgeColor,
          modifier = Modifier.size(34.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Text(
              text = "${rank}º",
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
              color = rankTextColor
            )
          }
        }
        Column {
          Text(
            text = if (isCurrentUser) "${player.displayName} (Você)" else player.displayName,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
          )
          val roundTag = if (currentQuestionIndex != null && player.lastAnsweredIndex == currentQuestionIndex) {
            if (player.lastAnswerCorrect) "Acertou nesta rodada" else "Errou nesta rodada"
          } else {
            "${player.correctCount} acertos • ${player.incorrectCount} erros"
          }
          Text(
            text = roundTag,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Text(
        text = "${player.score} pts",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
        color = MaterialTheme.colorScheme.primary
      )
    }
  }
}

private fun formatSentenceWithBlank(sentence: String, highlightColor: Color) = buildAnnotatedString {
  val parts = sentence.split("___")
  parts.forEachIndexed { index, part ->
    append(part)
    if (index < parts.lastIndex) {
      withStyle(
        SpanStyle(
          color = highlightColor,
          fontWeight = FontWeight.ExtraBold
        )
      ) {
        append(" _____ ")
      }
    }
  }
}
