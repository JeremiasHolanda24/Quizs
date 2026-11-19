package com.example.ui.quiz

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Help
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.QuizTheme

@Composable
fun QuestionCard(
  question: QuizQuestion,
  index: Int,
  totalCount: Int,
  showContext: Boolean,
  showCategory: Boolean,
  answerRecord: AnswerRecord?,
  onSubmitAnswer: (String) -> Unit,
  onAskAiTutor: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val extendedColors = QuizTheme.colors
  val isAnswered = answerRecord != null

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .testTag("question_card_${question.id}"),
    shape = RoundedCornerShape(18.dp),
    color = extendedColors.cardBackground,
    border = BorderStroke(1.dp, extendedColors.border),
    tonalElevation = 1.dp,
    shadowElevation = 4.dp
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp)
    ) {
      // Meta row: question number + optional category tag
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Questão ${index + 1} de $totalCount",
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.primary
        )

        AnimatedVisibility(
          visible = showCategory,
          enter = fadeIn(),
          exit = fadeOut()
        ) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = extendedColors.categoryBadgeBg
          ) {
            Text(
              text = question.category,
              style = MaterialTheme.typography.labelSmall,
              color = extendedColors.categoryBadgeText,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
          }
        }
      }

      // Context callout box
      AnimatedVisibility(
        visible = showContext,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
      ) {
        Column {
          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .height(IntrinsicSize.Min)
              .clip(RoundedCornerShape(10.dp))
              .background(extendedColors.surfaceElevated)
          ) {
            Box(
              modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary)
            )
            Text(
              text = buildAnnotatedString {
                withStyle(
                  SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                ) {
                  append("Situação: ")
                }
                append(question.context)
              },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Question sentence with highlighted blank or filled answer
      HighlightedQuestionSentence(
        sentence = question.sentence,
        answerRecord = answerRecord
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Options grid or typing input
      when (question.type) {
        QuestionType.CHOICE -> {
          ChoiceOptionsSection(
            question = question,
            answerRecord = answerRecord,
            onOptionClick = onSubmitAnswer
          )
        }

        QuestionType.TYPING -> {
          TypingAnswerSection(
            questionId = question.id,
            isAnswered = isAnswered,
            submittedAnswer = answerRecord?.submittedAnswer,
            onSubmit = onSubmitAnswer
          )
        }
      }

      // Detailed grammar feedback box
      AnimatedVisibility(
        visible = answerRecord != null,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
      ) {
        if (answerRecord != null) {
          Column {
            Spacer(modifier = Modifier.height(14.dp))
            FeedbackBox(
              question = question,
              record = answerRecord,
              onAskAiTutor = onAskAiTutor
            )
          }
        }
      }
    }
  }
}

@Composable
private fun HighlightedQuestionSentence(
  sentence: String,
  answerRecord: AnswerRecord?
) {
  val extendedColors = QuizTheme.colors
  val primaryColor = MaterialTheme.colorScheme.primary

  val annotated = buildAnnotatedString {
    val blank = "___"
    val idx = sentence.indexOf(blank)
    if (idx >= 0) {
      append(sentence.substring(0, idx))
      if (answerRecord != null) {
        val highlightColor = when (answerRecord.outcome) {
          AnswerOutcome.CORRECT -> extendedColors.successBorder
          AnswerOutcome.MAYBE -> extendedColors.maybeBorder
          AnswerOutcome.WRONG -> extendedColors.errorBorder
        }
        withStyle(
          SpanStyle(
            color = highlightColor,
            fontWeight = FontWeight.ExtraBold
          )
        ) {
          append(answerRecord.submittedAnswer)
        }
      } else {
        withStyle(
          SpanStyle(
            color = primaryColor,
            fontWeight = FontWeight.ExtraBold
          )
        ) {
          append("______")
        }
      }
      append(sentence.substring(idx + blank.length))
    } else {
      append(sentence)
    }
  }

  Text(
    text = annotated,
    style = MaterialTheme.typography.titleLarge,
    color = MaterialTheme.colorScheme.onSurface
  )
}

@Composable
private fun ChoiceOptionsSection(
  question: QuizQuestion,
  answerRecord: AnswerRecord?,
  onOptionClick: (String) -> Unit
) {
  val extendedColors = QuizTheme.colors
  val isAnswered = answerRecord != null

  BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
    val columns = if (maxWidth < 340.dp) 1 else 2
    val rows = question.options.chunked(columns)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      rows.forEach { rowOptions ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          rowOptions.forEach { option ->
            val isCorrectOption = question.acceptedAnswers.any {
              it.equals(option, ignoreCase = true)
            }
            val isUserChoice = answerRecord?.submittedAnswer.equals(option, ignoreCase = true)
            val isUserMaybe = isUserChoice && answerRecord?.isMaybe == true

            val containerColor = when {
              !isAnswered -> extendedColors.surfaceElevated
              isCorrectOption -> extendedColors.optionCorrectBg
              isUserMaybe -> extendedColors.optionMaybeBg
              isUserChoice -> extendedColors.optionWrongBg
              else -> extendedColors.surfaceElevated.copy(alpha = 0.65f)
            }

            val borderColor = when {
              !isAnswered -> extendedColors.border
              isCorrectOption -> extendedColors.successBorder
              isUserMaybe -> extendedColors.maybeBorder
              isUserChoice -> extendedColors.errorBorder
              else -> extendedColors.border.copy(alpha = 0.5f)
            }

            val contentColor = when {
              !isAnswered -> MaterialTheme.colorScheme.onSurface
              isCorrectOption || isUserChoice -> Color.White
              else -> MaterialTheme.colorScheme.onSurfaceVariant
            }

            Surface(
              onClick = { if (!isAnswered) onOptionClick(option) },
              enabled = !isAnswered,
              shape = RoundedCornerShape(12.dp),
              color = containerColor,
              border = BorderStroke(1.dp, borderColor),
              modifier = Modifier
                .weight(1f)
                .minimumInteractiveComponentSize()
                .testTag("option_${question.id}_$option")
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = option,
                  style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isAnswered && (isCorrectOption || isUserMaybe)) {
                      FontWeight.Bold
                    } else {
                      FontWeight.Medium
                    }
                  ),
                  color = contentColor
                )

                if (isAnswered && isCorrectOption) {
                  Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                  )
                } else if (isAnswered && isUserMaybe) {
                  Icon(
                    imageVector = Icons.Rounded.Help,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                  )
                } else if (isAnswered && isUserChoice) {
                  Icon(
                    imageVector = Icons.Rounded.Cancel,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                  )
                }
              }
            }
          }

          // Balance odd row when 2 columns
          if (columns == 2 && rowOptions.size == 1) {
            Spacer(modifier = Modifier.weight(1f))
          }
        }
      }
    }
  }
}

@Composable
private fun TypingAnswerSection(
  questionId: Int,
  isAnswered: Boolean,
  submittedAnswer: String?,
  onSubmit: (String) -> Unit
) {
  val extendedColors = QuizTheme.colors
  var inputValue by rememberSaveable(questionId) { mutableStateOf(submittedAnswer.orEmpty()) }
  val displayedValue = if (isAnswered && submittedAnswer != null) submittedAnswer else inputValue

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    OutlinedTextField(
      value = displayedValue,
      onValueChange = { if (!isAnswered) inputValue = it },
      enabled = !isAnswered,
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
        disabledContainerColor = extendedColors.surfaceElevated.copy(alpha = 0.6f),
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = extendedColors.border
      ),
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
      keyboardActions = KeyboardActions(
        onDone = {
          if (!isAnswered && inputValue.isNotBlank()) {
            onSubmit(inputValue)
          }
        }
      ),
      modifier = Modifier
        .weight(1f)
        .testTag("typing_input_$questionId")
    )

    Button(
      onClick = { onSubmit(inputValue) },
      enabled = !isAnswered && inputValue.isNotBlank(),
      shape = RoundedCornerShape(12.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = Color.White
      ),
      contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
      modifier = Modifier
        .height(56.dp)
        .testTag("verify_button_$questionId")
    ) {
      Text(
        text = stringResource(R.string.action_verify),
        style = MaterialTheme.typography.labelLarge
      )
    }
  }
}

@Composable
private fun FeedbackBox(
  question: QuizQuestion,
  record: AnswerRecord,
  onAskAiTutor: () -> Unit
) {
  val extendedColors = QuizTheme.colors
  val outcome = record.outcome
  val bgColor = when (outcome) {
    AnswerOutcome.CORRECT -> extendedColors.successBg
    AnswerOutcome.MAYBE -> extendedColors.maybeBg
    AnswerOutcome.WRONG -> extendedColors.errorBg
  }
  val borderColor = when (outcome) {
    AnswerOutcome.CORRECT -> extendedColors.successBorder
    AnswerOutcome.MAYBE -> extendedColors.maybeBorder
    AnswerOutcome.WRONG -> extendedColors.errorBorder
  }
  val textColor = when (outcome) {
    AnswerOutcome.CORRECT -> extendedColors.successText
    AnswerOutcome.MAYBE -> extendedColors.maybeText
    AnswerOutcome.WRONG -> extendedColors.errorText
  }

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = bgColor,
    border = BorderStroke(1.dp, borderColor),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("feedback_box_${question.id}")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(
          imageVector = when (outcome) {
            AnswerOutcome.CORRECT -> Icons.Rounded.CheckCircle
            AnswerOutcome.MAYBE -> Icons.Rounded.Help
            AnswerOutcome.WRONG -> Icons.Rounded.Cancel
          },
          contentDescription = null,
          tint = borderColor,
          modifier = Modifier.size(20.dp)
        )
        val xpSuffix = if (record.xpEarned > 0) " • +${record.xpEarned} XP" else ""
        Text(
          text = when (outcome) {
            AnswerOutcome.CORRECT ->
              "Resposta Correta!$xpSuffix (${question.acceptedAnswers.joinToString(" / ")})"
            AnswerOutcome.MAYBE ->
              "Talvez! Depende do contexto$xpSuffix (${record.submittedAnswer}) — Ideal: ${question.acceptedAnswers.joinToString(" / ")}"
            AnswerOutcome.WRONG ->
              "Resposta Incorreta — Gabarito: ${question.acceptedAnswers.joinToString(" / ")}"
          },
          style = MaterialTheme.typography.titleSmall,
          color = textColor
        )
      }

      FeedbackBullet(
        lead = "Classificação da frase: ",
        body = "${question.category}.",
        color = textColor
      )

      when (outcome) {
        AnswerOutcome.CORRECT -> {
          FeedbackBullet(
            lead = "Por que está certo: ",
            body = question.reason,
            color = textColor
          )
          FeedbackBullet(
            lead = "Função gramatical de \"${record.correctModal.name}\": ",
            body = "${record.correctModal.role}.",
            color = textColor
          )
          val extraContrast = if (record.correctModal.contrast.isNotBlank()) {
            " ${record.correctModal.contrast}"
          } else {
            ""
          }
          FeedbackBullet(
            lead = "Estrutura e uso: ",
            body = "${record.correctModal.structure}.$extraContrast",
            color = textColor
          )
        }

        AnswerOutcome.MAYBE -> {
          if (question.maybeReason.isNotBlank()) {
            FeedbackBullet(
              lead = "Por que \"${record.submittedAnswer}\" pode estar certo: ",
              body = question.maybeReason,
              color = textColor
            )
          }
          FeedbackBullet(
            lead = "Melhor opção para esta situação (${question.acceptedAnswers.joinToString(" / ")}): ",
            body = question.reason,
            color = textColor
          )
          val extraContrast = if (record.correctModal.contrast.isNotBlank()) {
            " ${record.correctModal.contrast}"
          } else {
            ""
          }
          FeedbackBullet(
            lead = "Regra de \"${record.correctModal.name}\": ",
            body = "${record.correctModal.role}.$extraContrast",
            color = textColor
          )
        }

        AnswerOutcome.WRONG -> {
          FeedbackBullet(
            lead = "Explicação do gabarito: ",
            body = question.reason,
            color = textColor
          )

          val submittedModal = record.submittedModal
          if (submittedModal != null) {
            Text(
              text = buildAnnotatedString {
                append("• ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                  append("Por que \"${record.submittedAnswer}\" não se encaixa: ")
                }
                append("\"${submittedModal.name}\" expressa ")
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                  append(submittedModal.role.lowercase())
                }
                append(", o que muda o sentido exigido por esta situação.")
              },
              style = MaterialTheme.typography.bodySmall,
              color = textColor
            )
          } else {
            FeedbackBullet(
              lead = "Sobre sua resposta (\"${record.submittedAnswer}\"): ",
              body = "Esta palavra não corresponde a um verbo modal válido ou adequado para completar a estrutura desta frase.",
              color = textColor
            )
          }

          val extraContrast = if (record.correctModal.contrast.isNotBlank()) {
            " ${record.correctModal.contrast}"
          } else {
            ""
          }
          FeedbackBullet(
            lead = "Regra de \"${record.correctModal.name}\": ",
            body = "${record.correctModal.role}.$extraContrast",
            color = textColor
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))
      OutlinedButton(
        onClick = onAskAiTutor,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, borderColor),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = Modifier.testTag("ask_ai_tutor_button_${question.id}")
      ) {
        Icon(
          imageVector = Icons.Rounded.AutoAwesome,
          contentDescription = null,
          tint = textColor,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Explicar com mais exemplos (Tutor IA)",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          color = textColor
        )
      }
    }
  }
}

@Composable
private fun FeedbackBullet(
  lead: String,
  body: String,
  color: Color
) {
  Text(
    text = buildAnnotatedString {
      append("• ")
      withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
        append(lead)
      }
      append(body)
    },
    style = MaterialTheme.typography.bodySmall,
    color = color
  )
}
