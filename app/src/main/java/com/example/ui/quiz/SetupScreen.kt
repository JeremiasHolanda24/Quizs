package com.example.ui.quiz

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.QuizTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetupScreen(
  uiState: QuizUiState,
  onlineUiState: OnlineQuizUiState,
  onEvent: (QuizEvent) -> Unit,
  onSelectOnlineSubMode: (OnlineGroupSubMode) -> Unit,
  onUpdateOnlineDisplayName: (String) -> Unit,
  onUpdateOnlinePin: (String) -> Unit,
  onCreateOnlineRoom: () -> Unit,
  onJoinOnlineRoom: () -> Unit,
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier
) {
  val extendedColors = QuizTheme.colors
  val scrollState = rememberScrollState()
  var showQuickGuide by remember { mutableStateOf(false) }
  val showQuizConfig = uiState.gameMode == GameMode.SOLO ||
    onlineUiState.onlineSubMode == OnlineGroupSubMode.CREATE

  val backgroundBrush = Brush.verticalGradient(
    colors = listOf(
      MaterialTheme.colorScheme.background,
      MaterialTheme.colorScheme.background,
      MaterialTheme.colorScheme.primary.copy(alpha = if (uiState.isLightTheme) 0.06f else 0.10f)
    )
  )

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(backgroundBrush)
      .statusBarsPadding()
      .navigationBarsPadding()
      .verticalScroll(scrollState)
      .padding(horizontal = 16.dp, vertical = 20.dp),
    contentAlignment = Alignment.Center
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 560.dp),
      shape = RoundedCornerShape(22.dp),
      color = extendedColors.cardBackground,
      border = BorderStroke(1.dp, extendedColors.border),
      tonalElevation = 2.dp,
      shadowElevation = if (uiState.isLightTheme) 6.dp else 12.dp
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp)
      ) {
        // Top bar: version badge on top-left, sign-out + theme toggle on top-right
        Box(modifier = Modifier.fillMaxWidth()) {
          VersionBadge(
            modifier = Modifier
              .align(Alignment.TopStart)
              .testTag("setup_version_badge")
          )

          Row(
            modifier = Modifier.align(Alignment.TopEnd),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = CircleShape,
              color = extendedColors.surfaceElevated,
              border = BorderStroke(1.dp, extendedColors.border)
            ) {
              IconButton(
                onClick = onSignOut,
                modifier = Modifier
                  .size(48.dp)
                  .testTag("setup_sign_out_button")
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.Logout,
                  contentDescription = stringResource(R.string.sign_out_desc),
                  tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Surface(
              shape = CircleShape,
              color = extendedColors.surfaceElevated,
              border = BorderStroke(1.dp, extendedColors.border)
            ) {
              IconButton(
                onClick = { onEvent(QuizEvent.ToggleTheme) },
                modifier = Modifier
                  .size(48.dp)
                  .testTag("setup_theme_toggle_button")
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
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = stringResource(R.string.quiz_title),
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.primary,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = stringResource(R.string.setup_subtitle),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Section 1: Game mode
        Text(
          text = stringResource(R.string.section_game_mode),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          ModeTabButton(
            label = stringResource(R.string.mode_solo),
            icon = Icons.Rounded.Person,
            selected = uiState.gameMode == GameMode.SOLO,
            onClick = { onEvent(QuizEvent.SelectGameMode(GameMode.SOLO)) },
            modifier = Modifier
              .weight(1f)
              .testTag("mode_solo_tab")
          )
          ModeTabButton(
            label = stringResource(R.string.mode_group),
            icon = Icons.Rounded.Groups,
            selected = uiState.gameMode == GameMode.GROUP,
            onClick = { onEvent(QuizEvent.SelectGameMode(GameMode.GROUP)) },
            modifier = Modifier
              .weight(1f)
              .testTag("mode_group_tab")
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        AnimatedContent(
          targetState = uiState.gameMode,
          transitionSpec = { fadeIn() togetherWith fadeOut() },
          label = "game_mode_input"
        ) { mode ->
          when (mode) {
            GameMode.SOLO -> {
              OutlinedTextField(
                value = uiState.soloNameInput,
                onValueChange = { onEvent(QuizEvent.UpdateSoloName(it)) },
                placeholder = {
                  Text(
                    text = stringResource(R.string.solo_name_placeholder),
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
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("solo_name_input")
              )
            }

            GameMode.GROUP -> {
              Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  ModeTabButton(
                    label = stringResource(R.string.online_create_room_tab),
                    icon = Icons.Rounded.AddCircleOutline,
                    selected = onlineUiState.onlineSubMode == OnlineGroupSubMode.CREATE,
                    onClick = { onSelectOnlineSubMode(OnlineGroupSubMode.CREATE) },
                    modifier = Modifier
                      .weight(1f)
                      .testTag("online_create_tab")
                  )
                  ModeTabButton(
                    label = stringResource(R.string.online_join_room_tab),
                    icon = Icons.Rounded.Pin,
                    selected = onlineUiState.onlineSubMode == OnlineGroupSubMode.JOIN,
                    onClick = { onSelectOnlineSubMode(OnlineGroupSubMode.JOIN) },
                    modifier = Modifier
                      .weight(1f)
                      .testTag("online_join_tab")
                  )
                }

                OutlinedTextField(
                  value = onlineUiState.displayNameInput,
                  onValueChange = onUpdateOnlineDisplayName,
                  placeholder = {
                    Text(
                      text = stringResource(R.string.online_name_placeholder),
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
                  keyboardOptions = KeyboardOptions(
                    imeAction = if (onlineUiState.onlineSubMode == OnlineGroupSubMode.JOIN) {
                      ImeAction.Next
                    } else {
                      ImeAction.Done
                    }
                  ),
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("online_display_name_input")
                )

                if (onlineUiState.onlineSubMode == OnlineGroupSubMode.JOIN) {
                  OutlinedTextField(
                    value = onlineUiState.pinInput,
                    onValueChange = onUpdateOnlinePin,
                    placeholder = {
                      Text(
                        text = stringResource(R.string.online_pin_placeholder),
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
                    keyboardOptions = KeyboardOptions(
                      capitalization = KeyboardCapitalization.Characters,
                      imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                      onDone = { onJoinOnlineRoom() }
                    ),
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("online_pin_input")
                  )
                }

                if (onlineUiState.errorMessage != null) {
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = extendedColors.errorBg,
                    border = BorderStroke(1.dp, extendedColors.errorBorder),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Text(
                      text = onlineUiState.errorMessage,
                      style = MaterialTheme.typography.bodySmall,
                      color = extendedColors.errorText,
                      modifier = Modifier.padding(12.dp),
                      textAlign = TextAlign.Center
                    )
                  }
                }
              }
            }
          }
        }

        if (showQuizConfig) {
          Spacer(modifier = Modifier.height(22.dp))

          // Section 2: Modal verbs filter
          Text(
            text = stringResource(R.string.section_modal_filters),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(10.dp))

          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            ModalGroup.entries.forEach { group ->
              val isSelected = group in uiState.selectedGroups
              FilterChip(
                selected = isSelected,
                onClick = { onEvent(QuizEvent.ToggleModalGroup(group)) },
                label = {
                  Text(
                    text = group.label,
                    style = MaterialTheme.typography.labelLarge.copy(
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                    )
                  )
                },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                  containerColor = extendedColors.surfaceElevated,
                  labelColor = MaterialTheme.colorScheme.onSurface,
                  selectedContainerColor = MaterialTheme.colorScheme.primary,
                  selectedLabelColor = Color.White
                ),
                border = FilterChipDefaults.filterChipBorder(
                  enabled = true,
                  selected = isSelected,
                  borderColor = extendedColors.border,
                  selectedBorderColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("modal_filter_${group.id}")
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          val count = uiState.filteredQuestionCount
          val countText = if (count == 1) {
            "1 exercício selecionado"
          } else {
            "$count exercícios selecionados"
          }
          Text(
            text = countText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
          )

          Spacer(modifier = Modifier.height(22.dp))

          // Section 3: Difficulty selector
          Text(
            text = stringResource(R.string.section_difficulty),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            DifficultyOptionCard(
              title = stringResource(R.string.difficulty_easy),
              description = stringResource(R.string.difficulty_easy_desc),
              icon = Icons.Rounded.AutoAwesome,
              selected = uiState.difficulty == QuizDifficulty.EASY,
              onClick = { onEvent(QuizEvent.SelectDifficulty(QuizDifficulty.EASY)) },
              modifier = Modifier
                .weight(1f)
                .testTag("difficulty_easy_card")
            )
            DifficultyOptionCard(
              title = stringResource(R.string.difficulty_normal),
              description = stringResource(R.string.difficulty_normal_desc),
              icon = Icons.Rounded.Tune,
              selected = uiState.difficulty == QuizDifficulty.NORMAL,
              onClick = { onEvent(QuizEvent.SelectDifficulty(QuizDifficulty.NORMAL)) },
              modifier = Modifier
                .weight(1f)
                .testTag("difficulty_normal_card")
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
          onClick = { showQuickGuide = true },
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, extendedColors.border),
          colors = ButtonDefaults.outlinedButtonColors(
            containerColor = extendedColors.surfaceElevated,
            contentColor = MaterialTheme.colorScheme.onSurface
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("quick_guide_button")
        ) {
          Icon(
            imageVector = Icons.Rounded.AutoStories,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = stringResource(R.string.action_quick_guide),
            style = MaterialTheme.typography.labelLarge
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        val isOnlineMode = uiState.gameMode == GameMode.GROUP
        val isLoading = isOnlineMode && onlineUiState.isLoading
        val ctaLabel = when {
          !isOnlineMode -> stringResource(R.string.start_quiz)
          onlineUiState.onlineSubMode == OnlineGroupSubMode.CREATE ->
            stringResource(R.string.online_create_room_cta)
          else -> stringResource(R.string.online_join_room_cta)
        }
        val ctaIcon = when {
          !isOnlineMode -> Icons.Rounded.PlayArrow
          onlineUiState.onlineSubMode == OnlineGroupSubMode.CREATE -> Icons.Rounded.Groups
          else -> Icons.AutoMirrored.Rounded.Login
        }

        Button(
          onClick = {
            when {
              !isOnlineMode -> onEvent(QuizEvent.StartQuiz)
              onlineUiState.onlineSubMode == OnlineGroupSubMode.CREATE -> onCreateOnlineRoom()
              else -> onJoinOnlineRoom()
            }
          },
          enabled = !isLoading,
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("start_quiz_button")
        ) {
          if (isLoading) {
            CircularProgressIndicator(
              modifier = Modifier.size(22.dp),
              color = Color.White,
              strokeWidth = 2.5.dp
            )
          } else {
            Icon(
              imageVector = ctaIcon,
              contentDescription = null,
              modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = ctaLabel,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        BookReferenceFooter()
      }
    }
  }

  if (showQuickGuide) {
    ModalQuickGuideSheet(onDismiss = { showQuickGuide = false })
  }
}

private const val BOOK_REFERENCE_URL = "https://www.cambridge.org/9781108457651"

@Composable
internal fun BookReferenceFooter(modifier: Modifier = Modifier) {
  val extendedColors = QuizTheme.colors
  val uriHandler = LocalUriHandler.current

  Surface(
    onClick = { runCatching { uriHandler.openUri(BOOK_REFERENCE_URL) } },
    shape = RoundedCornerShape(12.dp),
    color = extendedColors.surfaceElevated.copy(alpha = 0.7f),
    border = BorderStroke(1.dp, extendedColors.border),
    modifier = modifier
      .fillMaxWidth()
      .minimumInteractiveComponentSize()
      .testTag("book_reference_footer")
  ) {
    Text(
      text = buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) {
          append("Observação: ")
        }
        append("Esse quiz foi feito via IA usando de base esse livro: ")
        withStyle(
          SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            textDecoration = TextDecoration.Underline
          )
        ) {
          append("English Grammar in Use (5th Edition — Raymond Murphy, Cambridge)")
        }
      },
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 12.dp)
    )
  }
}

@Composable
internal fun VersionBadge(modifier: Modifier = Modifier) {
  val extendedColors = QuizTheme.colors
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(12.dp),
    color = extendedColors.accentBadgeBg
  ) {
    Text(
      text = stringResource(R.string.version_badge),
      style = MaterialTheme.typography.labelSmall,
      color = extendedColors.accentBadgeText,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
    )
  }
}

@Composable
private fun ModeTabButton(
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val extendedColors = QuizTheme.colors
  Surface(
    onClick = onClick,
    modifier = modifier.minimumInteractiveComponentSize(),
    shape = RoundedCornerShape(12.dp),
    color = if (selected) MaterialTheme.colorScheme.primary else extendedColors.surfaceElevated,
    border = BorderStroke(
      width = 1.dp,
      color = if (selected) MaterialTheme.colorScheme.primary else extendedColors.border
    )
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 13.dp),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(18.dp)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
      )
    }
  }
}

@Composable
private fun DifficultyOptionCard(
  title: String,
  description: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val extendedColors = QuizTheme.colors
  Surface(
    onClick = onClick,
    modifier = modifier.minimumInteractiveComponentSize(),
    shape = RoundedCornerShape(14.dp),
    color = if (selected) extendedColors.accentBadgeBg else extendedColors.surfaceElevated,
    border = BorderStroke(
      width = if (selected) 1.5.dp else 1.dp,
      color = if (selected) MaterialTheme.colorScheme.primary else extendedColors.border
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (selected) {
            MaterialTheme.colorScheme.primary
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant
          },
          modifier = Modifier.size(18.dp)
        )
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall,
          color = if (selected) {
            MaterialTheme.colorScheme.primary
          } else {
            MaterialTheme.colorScheme.onSurface
          }
        )
      }
      Text(
        text = description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )
    }
  }
}
