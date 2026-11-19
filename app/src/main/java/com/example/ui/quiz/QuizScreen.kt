package com.example.ui.quiz

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.QuizTheme

@Composable
fun QuizScreen(
  uiState: QuizUiState,
  listState: LazyListState,
  onEvent: (QuizEvent) -> Unit,
  modifier: Modifier = Modifier
) {
  BackHandler {
    onEvent(QuizEvent.ResetToSetup)
  }

  val flashAlpha = remember { Animatable(0f) }
  var flashOutcome by remember { mutableStateOf<AnswerOutcome?>(null) }
  val latestAnswer = uiState.answersByQuestionId.values.lastOrNull()

  LaunchedEffect(uiState.answersByQuestionId.size) {
    if (uiState.answersByQuestionId.isNotEmpty() && latestAnswer != null) {
      flashOutcome = latestAnswer.outcome
      flashAlpha.snapTo(0f)
      flashAlpha.animateTo(1f, animationSpec = tween(durationMillis = 220))
      kotlinx.coroutines.delay(180L)
      flashAlpha.animateTo(0f, animationSpec = tween(durationMillis = 1200))
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    Column(
      modifier = Modifier.fillMaxSize()
    ) {
    StickyQuizHeader(
      uiState = uiState,
      onEvent = onEvent
    )

    LazyColumn(
      state = listState,
      modifier = Modifier
        .fillMaxSize()
        .navigationBarsPadding(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      itemsIndexed(
        items = uiState.activeQuestions,
        key = { _, question -> question.id }
      ) { index, question ->
        QuestionCard(
          question = question,
          index = index,
          totalCount = uiState.totalActiveQuestions,
          showContext = uiState.showContext,
          showCategory = uiState.showCategory,
          answerRecord = uiState.answersByQuestionId[question.id],
          onSubmitAnswer = { answer ->
            onEvent(
              QuizEvent.SubmitAnswer(
                questionId = question.id,
                answer = answer
              )
            )
          },
          onAskAiTutor = { onEvent(QuizEvent.AskAiTutor(question.id)) },
          modifier = Modifier.widthIn(max = 760.dp)
        )
      }

      item(key = "summary_footer") {
        QuizSummaryFooter(
          uiState = uiState,
          onViewResults = { onEvent(QuizEvent.OpenResults) },
          onNewQuiz = { onEvent(QuizEvent.ResetToSetup) },
          modifier = Modifier.widthIn(max = 760.dp)
        )
      }
    }
    }

    AnswerEdgeFlashOverlay(
      outcome = flashOutcome,
      alpha = flashAlpha.value
    )

    if (uiState.aiTutorState.isVisible) {
      AiTutorSheet(
        tutorState = uiState.aiTutorState,
        onDismiss = { onEvent(QuizEvent.DismissAiTutor) }
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StickyQuizHeader(
  uiState: QuizUiState,
  onEvent: (QuizEvent) -> Unit
) {
  val extendedColors = QuizTheme.colors

  Surface(
    color = extendedColors.cardBackground,
    border = BorderStroke(1.dp, extendedColors.border),
    shadowElevation = 6.dp,
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 760.dp)
      ) {
        // Top header bar
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = stringResource(R.string.quiz_title),
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
              color = MaterialTheme.colorScheme.primary
            )
            VersionBadge()
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            if (uiState.currentStreak >= 2) {
              Surface(
                shape = RoundedCornerShape(20.dp),
                color = extendedColors.maybeBg,
                border = BorderStroke(1.dp, extendedColors.maybeBorder),
                modifier = Modifier.testTag("streak_pill")
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
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
                    text = "${uiState.currentStreak}x",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = extendedColors.maybeText
                  )
                }
              }
            }

            Surface(
              shape = RoundedCornerShape(20.dp),
              color = extendedColors.accentBadgeBg,
              modifier = Modifier.testTag("progress_pill")
            ) {
              Text(
                text = "${uiState.answeredCount} / ${uiState.totalActiveQuestions}",
                style = MaterialTheme.typography.labelLarge,
                color = extendedColors.accentBadgeText,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
              )
            }

            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (uiState.isSettingsDrawerOpen) {
                extendedColors.accentBadgeBg
              } else {
                extendedColors.surfaceElevated
              },
              border = BorderStroke(
                1.dp,
                if (uiState.isSettingsDrawerOpen) {
                  MaterialTheme.colorScheme.primary
                } else {
                  extendedColors.border
                }
              )
            ) {
              IconButton(
                onClick = { onEvent(QuizEvent.ToggleSettingsDrawer) },
                modifier = Modifier
                  .size(44.dp)
                  .testTag("settings_gear_button")
              ) {
                Icon(
                  imageVector = Icons.Outlined.Settings,
                  contentDescription = stringResource(R.string.open_settings_desc),
                  tint = if (uiState.isSettingsDrawerOpen) {
                    MaterialTheme.colorScheme.primary
                  } else {
                    MaterialTheme.colorScheme.onSurface
                  }
                )
              }
            }
          }
        }

        // Visual progress bar with remaining questions indicator
        if (uiState.totalActiveQuestions > 0) {
          val remainingCount = (uiState.totalActiveQuestions - uiState.answeredCount).coerceAtLeast(0)
          val targetProgress = (uiState.answeredCount.toFloat() / uiState.totalActiveQuestions.toFloat())
            .coerceIn(0f, 1f)
          val animatedProgress by animateFloatAsState(
            targetValue = targetProgress,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "quiz_progress_animation"
          )
          val remainingLabel = when (remainingCount) {
            0 -> "Todas concluídas!"
            1 -> "Resta 1 de ${uiState.totalActiveQuestions}"
            else -> "Restam $remainingCount de ${uiState.totalActiveQuestions}"
          }

          Spacer(modifier = Modifier.height(10.dp))

          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(5.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = remainingLabel,
                style = MaterialTheme.typography.labelMedium,
                color = if (uiState.isQuizCompleted) {
                  extendedColors.successBorder
                } else {
                  MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.testTag("remaining_questions_label")
              )
              Text(
                text = "${(targetProgress * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = if (uiState.isQuizCompleted) {
                  extendedColors.successBorder
                } else {
                  MaterialTheme.colorScheme.primary
                }
              )
            }

            LinearProgressIndicator(
              progress = { animatedProgress },
              modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .testTag("quiz_progress_bar"),
              color = if (uiState.isQuizCompleted) {
                extendedColors.successBorder
              } else {
                MaterialTheme.colorScheme.primary
              },
              trackColor = extendedColors.surfaceElevated,
              strokeCap = StrokeCap.Round,
              drawStopIndicator = {}
            )
          }
        }

        // Scoreboard bar
        if (uiState.players.isNotEmpty()) {
          Spacer(modifier = Modifier.height(10.dp))
          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            uiState.players.forEachIndexed { idx, player ->
              val isTurn = uiState.players.size > 1 && idx == uiState.currentTurnIndex
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isTurn) extendedColors.accentBadgeBg else extendedColors.surfaceElevated,
                border = BorderStroke(
                  width = 1.dp,
                  color = if (isTurn) MaterialTheme.colorScheme.primary else extendedColors.border
                ),
                modifier = Modifier.testTag("score_card_${player.name}")
              ) {
                Row(
                  modifier = Modifier.padding(
                    start = 10.dp,
                    end = if (uiState.players.size > 1) 4.dp else 10.dp,
                    top = 6.dp,
                    bottom = 6.dp
                  ),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  if (isTurn) {
                    Icon(
                      imageVector = Icons.Rounded.MyLocation,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(16.dp)
                    )
                  }
                  Text(
                    text = buildAnnotatedString {
                      if (isTurn) append("Vez: ")
                      withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(player.name)
                      }
                      append(": ${player.score} XP")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                  )

                  if (uiState.players.size > 1) {
                    IconButton(
                      onClick = { onEvent(QuizEvent.RemovePlayer(player.id)) },
                      modifier = Modifier
                        .size(28.dp)
                        .testTag("remove_scoreboard_player_${player.name}")
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.remove_player_desc),
                        tint = extendedColors.errorBorder,
                        modifier = Modifier.size(15.dp)
                      )
                    }
                  }
                }
              }
            }
          }
        }

        // Expandable Settings Drawer
        AnimatedVisibility(
          visible = uiState.isSettingsDrawerOpen,
          enter = expandVertically() + fadeIn(),
          exit = shrinkVertically() + fadeOut()
        ) {
          Column {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = extendedColors.surfaceElevated,
              border = BorderStroke(1.dp, extendedColors.border),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_drawer")
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = stringResource(R.string.settings_theme_label),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Surface(
                    shape = CircleShape,
                    color = extendedColors.cardBackground,
                    border = BorderStroke(1.dp, extendedColors.border)
                  ) {
                    IconButton(
                      onClick = { onEvent(QuizEvent.ToggleTheme) },
                      modifier = Modifier
                        .size(44.dp)
                        .testTag("drawer_theme_toggle_button")
                    ) {
                      Icon(
                        imageVector = if (uiState.isLightTheme) {
                          Icons.Outlined.DarkMode
                        } else {
                          Icons.Outlined.LightMode
                        },
                        contentDescription = stringResource(R.string.toggle_theme_desc),
                        tint = MaterialTheme.colorScheme.onSurface
                      )
                    }
                  }
                }

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = stringResource(R.string.settings_sound_label),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Surface(
                    shape = CircleShape,
                    color = if (uiState.isSoundEnabled) {
                      extendedColors.accentBadgeBg
                    } else {
                      extendedColors.cardBackground
                    },
                    border = BorderStroke(
                      1.dp,
                      if (uiState.isSoundEnabled) {
                        MaterialTheme.colorScheme.primary
                      } else {
                        extendedColors.border
                      }
                    )
                  ) {
                    IconButton(
                      onClick = { onEvent(QuizEvent.ToggleSound) },
                      modifier = Modifier
                        .size(44.dp)
                        .testTag("drawer_sound_toggle_button")
                    ) {
                      Icon(
                        imageVector = if (uiState.isSoundEnabled) {
                          Icons.Rounded.VolumeUp
                        } else {
                          Icons.Rounded.VolumeOff
                        },
                        contentDescription = stringResource(R.string.toggle_sound_desc),
                        tint = if (uiState.isSoundEnabled) {
                          MaterialTheme.colorScheme.primary
                        } else {
                          MaterialTheme.colorScheme.onSurfaceVariant
                        }
                      )
                    }
                  }
                }

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  OutlinedButton(
                    onClick = { onEvent(QuizEvent.ShuffleActiveQuestions) },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, extendedColors.border),
                    colors = ButtonDefaults.outlinedButtonColors(
                      containerColor = extendedColors.cardBackground,
                      contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                      .weight(1f)
                      .height(48.dp)
                      .testTag("shuffle_questions_button")
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Shuffle,
                      contentDescription = null,
                      modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = stringResource(R.string.action_shuffle),
                      style = MaterialTheme.typography.labelLarge
                    )
                  }

                  Button(
                    onClick = { onEvent(QuizEvent.ResetToSetup) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                      containerColor = extendedColors.optionWrongBg,
                      contentColor = Color.White
                    ),
                    modifier = Modifier
                      .weight(1f)
                      .height(48.dp)
                      .testTag("drawer_new_quiz_button")
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Refresh,
                      contentDescription = null,
                      modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = stringResource(R.string.action_new_quiz),
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
  }
}

@Composable
private fun QuizSummaryFooter(
  uiState: QuizUiState,
  onViewResults: () -> Unit,
  onNewQuiz: () -> Unit,
  modifier: Modifier = Modifier
) {
  val extendedColors = QuizTheme.colors

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(top = 6.dp, bottom = 24.dp),
    shape = RoundedCornerShape(18.dp),
    color = extendedColors.cardBackground,
    border = BorderStroke(1.dp, extendedColors.border),
    tonalElevation = 1.dp
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      if (uiState.isQuizCompleted) {
        Icon(
          imageVector = Icons.Rounded.EmojiEvents,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(36.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        val topScore = uiState.players.maxOfOrNull { it.score } ?: 0
        val leaders = uiState.players.filter { it.score == topScore }
        val resultTitle = if (uiState.players.size == 1) {
          "Rodada concluída! ${uiState.players.first().score} / ${uiState.totalActiveQuestions} acertos"
        } else if (leaders.size == 1) {
          "Vencedor: ${leaders.first().name} ($topScore pts)"
        } else {
          "Empate: ${leaders.joinToString(" & ") { it.name }} ($topScore pts)"
        }
        Text(
          text = resultTitle,
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.primary,
          textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
      } else {
        Text(
          text = stringResource(R.string.footer_title),
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
      }

      Text(
        text = stringResource(R.string.footer_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(18.dp))

      if (uiState.isQuizCompleted) {
        Button(
          onClick = onViewResults,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
          ),
          modifier = Modifier
            .widthIn(min = 200.dp, max = 280.dp)
            .height(50.dp)
            .testTag("footer_view_results_button")
        ) {
          Icon(
            imageVector = Icons.Rounded.EmojiEvents,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = stringResource(R.string.action_view_results),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
          onClick = onNewQuiz,
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, extendedColors.border),
          colors = ButtonDefaults.outlinedButtonColors(
            containerColor = extendedColors.surfaceElevated,
            contentColor = MaterialTheme.colorScheme.onSurface
          ),
          modifier = Modifier
            .widthIn(min = 200.dp, max = 280.dp)
            .height(48.dp)
            .testTag("footer_new_quiz_button")
        ) {
          Icon(
            imageVector = Icons.Rounded.Refresh,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = stringResource(R.string.action_new_quiz),
            style = MaterialTheme.typography.labelLarge
          )
        }
      } else {
        Button(
          onClick = onNewQuiz,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
          ),
          modifier = Modifier
            .widthIn(min = 200.dp, max = 280.dp)
            .height(50.dp)
            .testTag("footer_new_quiz_button")
        ) {
          Icon(
            imageVector = Icons.Rounded.Refresh,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = stringResource(R.string.action_new_quiz),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
        }
      }
    }
  }
}
