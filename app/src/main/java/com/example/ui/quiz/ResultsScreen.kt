package com.example.ui.quiz

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Help
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.QuizTheme

@Composable
fun ResultsScreen(
  uiState: QuizUiState,
  onEvent: (QuizEvent) -> Unit,
  modifier: Modifier = Modifier
) {
  BackHandler {
    onEvent(QuizEvent.ReviewAnswers)
  }

  val extendedColors = QuizTheme.colors
  val scrollState = rememberScrollState()
  val total = uiState.totalActiveQuestions.coerceAtLeast(1)
  val correct = uiState.correctCount
  val maybe = uiState.maybeCount
  val incorrect = uiState.incorrectCount
  val accuracyPercent = ((correct.toFloat() / total.toFloat()) * 100f).toInt()

  val backgroundBrush = Brush.verticalGradient(
    colors = listOf(
      MaterialTheme.colorScheme.background,
      MaterialTheme.colorScheme.background,
      MaterialTheme.colorScheme.primary.copy(alpha = if (uiState.isLightTheme) 0.08f else 0.14f)
    )
  )

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(backgroundBrush)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .verticalScroll(scrollState)
        .padding(horizontal = 16.dp, vertical = 20.dp),
      contentAlignment = Alignment.Center
    ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 560.dp)
        .testTag("results_screen_card"),
      shape = RoundedCornerShape(24.dp),
      color = extendedColors.cardBackground,
      border = BorderStroke(1.dp, extendedColors.border),
      tonalElevation = 2.dp,
      shadowElevation = if (uiState.isLightTheme) 6.dp else 14.dp
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Top bar with version badge and theme toggle
        Box(modifier = Modifier.fillMaxWidth()) {
          VersionBadge(
            modifier = Modifier.align(Alignment.TopStart)
          )

          Surface(
            modifier = Modifier.align(Alignment.TopEnd),
            shape = CircleShape,
            color = extendedColors.surfaceElevated,
            border = BorderStroke(1.dp, extendedColors.border)
          ) {
            IconButton(
              onClick = { onEvent(QuizEvent.ToggleTheme) },
              modifier = Modifier
                .size(48.dp)
                .testTag("results_theme_toggle_button")
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

        // Trophy emblem
        Surface(
          shape = CircleShape,
          color = extendedColors.accentBadgeBg,
          border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
          modifier = Modifier.size(72.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Rounded.EmojiEvents,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(38.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = stringResource(R.string.results_title),
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        val performanceSubtitle = when {
          accuracyPercent >= 90 -> "Excelente desempenho! Domínio impressionante dos modais."
          accuracyPercent >= 70 -> "Muito bem! Você acertou a maior parte das questões."
          accuracyPercent >= 50 -> "Bom progresso! Vale revisar os pontos em que houve dúvida."
          else -> "Continue praticando! Revise as explicações para fixar as regras."
        }
        Text(
          text = performanceSubtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(22.dp))

        // Total score highlight card
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = extendedColors.surfaceElevated,
          border = BorderStroke(1.dp, extendedColors.border),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("results_total_score_card")
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 18.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = stringResource(R.string.results_total_score_label).uppercase(),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "${uiState.totalScore} XP",
              style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 36.sp,
                lineHeight = 42.sp,
                fontWeight = FontWeight.ExtraBold
              ),
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.testTag("results_total_score_value")
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Surface(
                shape = RoundedCornerShape(20.dp),
                color = extendedColors.accentBadgeBg
              ) {
                Text(
                  text = "$correct/${uiState.totalActiveQuestions} acertos • $accuracyPercent%",
                  style = MaterialTheme.typography.labelLarge,
                  color = extendedColors.accentBadgeText,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
              }

              if (uiState.isNewHighScore) {
                Surface(
                  shape = RoundedCornerShape(20.dp),
                  color = extendedColors.successBg,
                  border = BorderStroke(1.dp, extendedColors.successBorder),
                  modifier = Modifier.testTag("new_high_score_badge")
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.AutoAwesome,
                      contentDescription = null,
                      tint = extendedColors.successBorder,
                      modifier = Modifier.size(14.dp)
                    )
                    Text(
                      text = "Novo Recorde!",
                      style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                      color = extendedColors.successText
                    )
                  }
                }
              }
            }

            if (uiState.bestStreak >= 2) {
              Spacer(modifier = Modifier.height(6.dp))
              Surface(
                shape = RoundedCornerShape(20.dp),
                color = extendedColors.maybeBg,
                border = BorderStroke(1.dp, extendedColors.maybeBorder),
                modifier = Modifier.testTag("results_best_streak_badge")
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Icon(
                    imageVector = Icons.Rounded.LocalFireDepartment,
                    contentDescription = null,
                    tint = extendedColors.maybeBorder,
                    modifier = Modifier.size(14.dp)
                  )
                  Text(
                    text = "Melhor sequência: ${uiState.bestStreak}x",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = extendedColors.maybeText
                  )
                }
              }
            }

            if (!uiState.isNewHighScore && uiState.highScore > 0) {
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "Recorde atual: ${uiState.highScore} XP",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Correct / Maybe / Incorrect stat cards
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          ResultStatCard(
            label = stringResource(R.string.results_correct_label),
            count = correct,
            icon = Icons.Rounded.CheckCircle,
            containerColor = extendedColors.successBg,
            borderColor = extendedColors.successBorder,
            contentColor = extendedColors.successText,
            iconTint = extendedColors.successBorder,
            modifier = Modifier
              .weight(1f)
              .testTag("results_correct_card")
          )
          if (maybe > 0) {
            ResultStatCard(
              label = "Talvez",
              count = maybe,
              icon = Icons.Rounded.Help,
              containerColor = extendedColors.maybeBg,
              borderColor = extendedColors.maybeBorder,
              contentColor = extendedColors.maybeText,
              iconTint = extendedColors.maybeBorder,
              modifier = Modifier
                .weight(1f)
                .testTag("results_maybe_card")
            )
          }
          ResultStatCard(
            label = stringResource(R.string.results_incorrect_label),
            count = incorrect,
            icon = Icons.Rounded.Cancel,
            containerColor = extendedColors.errorBg,
            borderColor = extendedColors.errorBorder,
            contentColor = extendedColors.errorText,
            iconTint = extendedColors.errorBorder,
            modifier = Modifier
              .weight(1f)
              .testTag("results_incorrect_card")
          )
        }

        // Group leaderboard when playing with multiple players
        if (uiState.players.size > 1) {
          Spacer(modifier = Modifier.height(20.dp))
          val rankedPlayers = uiState.players.sortedByDescending { it.score }
          val topScore = rankedPlayers.firstOrNull()?.score ?: 0

          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = stringResource(R.string.results_leaderboard_label),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            rankedPlayers.forEachIndexed { idx, player ->
              val isLeader = player.score == topScore && topScore > 0
              val ptsLabel = if (player.score == 1) "pt" else "pts"
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isLeader) extendedColors.accentBadgeBg else extendedColors.surfaceElevated,
                border = BorderStroke(
                  1.dp,
                  if (isLeader) MaterialTheme.colorScheme.primary else extendedColors.border
                ),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                  ) {
                    Text(
                      text = "#${idx + 1}",
                      style = MaterialTheme.typography.titleSmall,
                      color = if (isLeader) {
                        MaterialTheme.colorScheme.primary
                      } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                      }
                    )
                    Text(
                      text = player.name,
                      style = MaterialTheme.typography.titleSmall,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                  }
                  Text(
                    text = "${player.score} $ptsLabel",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isLeader) {
                      MaterialTheme.colorScheme.primary
                    } else {
                      MaterialTheme.colorScheme.onSurfaceVariant
                    }
                  )
                }
              }
            }
          }
        }

        // Modal Verbs X-Ray breakdown
        if (uiState.modalStats.isNotEmpty()) {
          Spacer(modifier = Modifier.height(18.dp))
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .testTag("results_modal_xray_section"),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = stringResource(R.string.results_modal_xray_label),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = extendedColors.surfaceElevated,
              border = BorderStroke(1.dp, extendedColors.border),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                uiState.modalStats.forEach { stat ->
                  val barColor = when {
                    stat.accuracyPercent >= 80 -> extendedColors.successBorder
                    stat.accuracyPercent >= 50 -> extendedColors.maybeBorder
                    else -> extendedColors.errorBorder
                  }
                  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Text(
                        text = stat.group.label,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                      )
                      val maybeTag = if (stat.maybe > 0) " • ${stat.maybe} talvez" else ""
                      Text(
                        text = "${stat.correct}/${stat.total} (${stat.accuracyPercent}%)$maybeTag",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = barColor
                      )
                    }
                    LinearProgressIndicator(
                      progress = { (stat.accuracyPercent / 100f).coerceIn(0f, 1f) },
                      modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(4.dp)),
                      color = barColor,
                      trackColor = extendedColors.cardBackground,
                      strokeCap = StrokeCap.Round,
                      drawStopIndicator = {}
                    )
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Solo-only CTA: Retry mistakes & maybe questions
        if (uiState.gameMode == GameMode.SOLO && uiState.reviewableMistakesCount > 0) {
          Button(
            onClick = { onEvent(QuizEvent.RetryMistakes) },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = extendedColors.optionMaybeBg,
              contentColor = Color.White
            ),
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("retry_mistakes_button")
          ) {
            Icon(
              imageVector = Icons.Rounded.MyLocation,
              contentDescription = null,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "${stringResource(R.string.action_retry_mistakes)} (${uiState.reviewableMistakesCount})",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
          }
          Spacer(modifier = Modifier.height(10.dp))
        }

        // Primary CTA: Play Again
        Button(
          onClick = { onEvent(QuizEvent.PlayAgain) },
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("play_again_button")
        ) {
          Icon(
            imageVector = Icons.Rounded.Replay,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = stringResource(R.string.action_play_again),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Secondary actions: Review Answers & New Quiz
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedButton(
            onClick = { onEvent(QuizEvent.ReviewAnswers) },
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, extendedColors.border),
            colors = ButtonDefaults.outlinedButtonColors(
              containerColor = extendedColors.surfaceElevated,
              contentColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("review_answers_button")
          ) {
            Icon(
              imageVector = Icons.Rounded.Visibility,
              contentDescription = null,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = stringResource(R.string.action_review_answers),
              style = MaterialTheme.typography.labelLarge
            )
          }

          OutlinedButton(
            onClick = { onEvent(QuizEvent.ResetToSetup) },
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, extendedColors.border),
            colors = ButtonDefaults.outlinedButtonColors(
              containerColor = extendedColors.surfaceElevated,
              contentColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("results_new_quiz_button")
          ) {
            Icon(
              imageVector = Icons.Rounded.Tune,
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

        Spacer(modifier = Modifier.height(16.dp))

        BookReferenceFooter()
      }
    }
    }

    if (uiState.isNewHighScore) {
      ConfettiOverlay(
        modifier = Modifier.fillMaxSize()
      )
    }
  }
}

@Composable
private fun ResultStatCard(
  label: String,
  count: Int,
  icon: ImageVector,
  containerColor: Color,
  borderColor: Color,
  contentColor: Color,
  iconTint: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(16.dp),
    color = containerColor,
    border = BorderStroke(1.dp, borderColor)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = iconTint,
        modifier = Modifier.size(24.dp)
      )
      Text(
        text = count.toString(),
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
        color = contentColor
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = contentColor
      )
    }
  }
}
