package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.OnlineQuizRepository
import com.example.data.QuizRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOutUser
import com.example.ui.quiz.GameMode
import com.example.ui.quiz.OnlineGroupSubMode
import com.example.ui.quiz.OnlineQuizEffect
import com.example.ui.quiz.OnlineQuizViewModel
import com.example.ui.quiz.OnlineRoomScreen
import com.example.ui.quiz.QuizEffect
import com.example.ui.quiz.QuizEvent
import com.example.ui.quiz.QuizScreen
import com.example.ui.quiz.QuizScreenMode
import com.example.ui.quiz.QuizSoundPlayer
import com.example.ui.quiz.QuizViewModel
import com.example.ui.quiz.ResultsScreen
import com.example.ui.quiz.SetupScreen
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    configureAppCheck(this, intent)
    enableEdgeToEdge()
    val deepLinkPin = intent?.data?.getQueryParameter("pin")
      ?.uppercase()
      ?.filter { it.isLetterOrDigit() }
      ?.take(8)
      .orEmpty()
    setContent {
      val quizViewModel: QuizViewModel = viewModel()
      val uiState by quizViewModel.uiState.collectAsStateWithLifecycle()
      val isDarkTheme = !uiState.isLightTheme
      val quizListState = rememberLazyListState()
      val soundPlayer = remember { QuizSoundPlayer() }
      val context = LocalContext.current
      val scope = rememberCoroutineScope()
      val credentialManager = remember(context) { CredentialManager.create(context) }
      val prefs = remember(context) {
        context.getSharedPreferences("modals_quiz_prefs", MODE_PRIVATE)
      }

      LaunchedEffect(prefs) {
        val savedHighScore = prefs.getInt("high_score", 0)
        if (savedHighScore > 0) {
          quizViewModel.onEvent(QuizEvent.LoadHighScore(savedHighScore))
        }
      }

      LaunchedEffect(uiState.highScore) {
        if (uiState.highScore > prefs.getInt("high_score", 0)) {
          prefs.edit().putInt("high_score", uiState.highScore).apply()
        }
      }

      val currentUser by remember { Firebase.auth.authStateFlow() }
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

      DisposableEffect(soundPlayer) {
        onDispose {
          soundPlayer.release()
        }
      }

      SideEffect {
        val barStyle = if (isDarkTheme) {
          SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
          SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
      }

      LaunchedEffect(quizViewModel) {
        quizViewModel.uiEffect.collect { effect ->
          when (effect) {
            QuizEffect.ScrollToTop -> {
              quizListState.scrollToItem(0)
            }
            is QuizEffect.PlayAnswerSound -> {
              soundPlayer.playAnswerFeedback(effect.outcome)
            }
            is QuizEffect.ShowMessage -> Unit
          }
        }
      }

      MyApplicationTheme(darkTheme = isDarkTheme) {
        val user = currentUser
        if (user == null) {
          AuthScreen(
            isLightTheme = uiState.isLightTheme,
            onToggleTheme = { quizViewModel.onEvent(QuizEvent.ToggleTheme) },
            onAuthSuccess = {}
          )
        } else {
          key(user.uid) {
            val onlineViewModel: OnlineQuizViewModel = viewModel(
              key = user.uid,
              factory = viewModelFactory {
                initializer {
                  val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                  OnlineQuizViewModel(
                    onlineRepository = OnlineQuizRepository(app),
                    quizRepository = QuizRepository(),
                    currentUserId = user.uid
                  )
                }
              }
            )
            val onlineUiState by onlineViewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(user.uid, user.displayName) {
              onlineViewModel.prefillDisplayNameIfBlank(user.displayName)
              if (uiState.soloNameInput.isBlank() && !user.displayName.isNullOrBlank()) {
                quizViewModel.onEvent(QuizEvent.UpdateSoloName(user.displayName.orEmpty()))
              }
            }

            LaunchedEffect(deepLinkPin) {
              if (deepLinkPin.length >= 4) {
                quizViewModel.onEvent(QuizEvent.SelectGameMode(GameMode.GROUP))
                onlineViewModel.selectSubMode(OnlineGroupSubMode.JOIN)
                onlineViewModel.updatePinInput(deepLinkPin)
              }
            }

            LaunchedEffect(onlineViewModel) {
              onlineViewModel.effects.collect { effect ->
                when (effect) {
                  is OnlineQuizEffect.PlayAnswerSound -> {
                    soundPlayer.playAnswerFeedback(effect.outcome)
                  }
                }
              }
            }

            if (onlineUiState.activeRoomCode != null) {
              OnlineRoomScreen(
                uiState = onlineUiState,
                currentUserId = user.uid,
                quizRepository = onlineViewModel.quizRepository,
                isLightTheme = uiState.isLightTheme,
                isSoundEnabled = uiState.isSoundEnabled,
                onToggleTheme = { quizViewModel.onEvent(QuizEvent.ToggleTheme) },
                onToggleSound = { quizViewModel.onEvent(QuizEvent.ToggleSound) },
                onStartGame = onlineViewModel::startGame,
                onUpdateTyping = onlineViewModel::updateTypingInput,
                onSubmitAnswer = { answer, remainingSeconds ->
                  onlineViewModel.submitAnswer(answer, uiState.isSoundEnabled, remainingSeconds)
                },
                onRevealAnswer = onlineViewModel::revealAnswer,
                onNextOrFinish = onlineViewModel::nextQuestionOrFinish,
                onLeaveRoom = onlineViewModel::leaveRoom
              )
            } else {
              Crossfade(
                targetState = uiState.screenMode,
                modifier = Modifier.fillMaxSize(),
                label = "quiz_screen_transition"
              ) { screenMode ->
                when (screenMode) {
                  QuizScreenMode.SETUP -> {
                    SetupScreen(
                      uiState = uiState,
                      onlineUiState = onlineUiState,
                      onEvent = quizViewModel::onEvent,
                      onSelectOnlineSubMode = onlineViewModel::selectSubMode,
                      onUpdateOnlineDisplayName = onlineViewModel::updateDisplayName,
                      onUpdateOnlinePin = onlineViewModel::updatePinInput,
                      onCreateOnlineRoom = {
                        onlineViewModel.createRoom(
                          selectedGroups = uiState.selectedGroups,
                          difficulty = uiState.difficulty
                        )
                      },
                      onJoinOnlineRoom = onlineViewModel::joinRoom,
                      onSignOut = {
                        onlineViewModel.leaveRoom()
                        signOutUser(
                          credentialManager = credentialManager,
                          scope = scope
                        )
                      }
                    )
                  }

                  QuizScreenMode.QUIZ -> {
                    QuizScreen(
                      uiState = uiState,
                      listState = quizListState,
                      onEvent = quizViewModel::onEvent
                    )
                  }

                  QuizScreenMode.RESULTS -> {
                    ResultsScreen(
                      uiState = uiState,
                      onEvent = quizViewModel::onEvent
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
