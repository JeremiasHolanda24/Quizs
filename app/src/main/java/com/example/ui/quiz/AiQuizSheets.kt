package com.example.ui.quiz

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.QuizTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiTutorSheet(
  tutorState: AiTutorState,
  onDismiss: () -> Unit
) {
  val extendedColors = QuizTheme.colors
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = extendedColors.cardBackground,
    modifier = Modifier.testTag("ai_tutor_sheet")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp)
        .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Icon(
          imageVector = Icons.Rounded.School,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(26.dp)
        )
        Column {
          Text(
            text = "Tutor IA — Explicação Extra",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onSurface
          )
          if (tutorState.questionSentence.isNotBlank()) {
            Text(
              text = tutorState.questionSentence,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      when {
        tutorState.isLoading && tutorState.text.isBlank() -> {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
          ) {
            CircularProgressIndicator(
              color = MaterialTheme.colorScheme.primary,
              strokeWidth = 2.5.dp,
              modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = "Preparando exemplos extras…",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        !tutorState.errorMessage.isNullOrBlank() -> {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = extendedColors.errorBg,
            border = BorderStroke(1.dp, extendedColors.errorBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = tutorState.errorMessage,
              style = MaterialTheme.typography.bodySmall,
              color = extendedColors.errorText,
              modifier = Modifier.padding(14.dp)
            )
          }
        }

        else -> {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = extendedColors.surfaceElevated,
            border = BorderStroke(1.dp, extendedColors.border),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = tutorState.text,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.padding(14.dp)
            )
          }
        }
      }
    }
  }
}
