package com.example.ui.quiz

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
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

private data class GuideItem(
  val title: String,
  val modals: String,
  val summary: String,
  val example: String
)

private val quickGuideItems = listOf(
  GuideItem(
    title = "1. Regras de Ouro dos Modais",
    modals = "Estrutura Geral",
    summary = "Modais puros (can, could, may, might, must, should, would) nunca recebem '-s' na 3ª pessoa, nunca usam 'to' antes do verbo principal e fazem perguntas/negativas sem 'do/does'. Exceção: 'have to' é semi-modal e usa do/does/did.",
    example = "She can swim (não 'cans' nem 'can to'). / Do you have to work?"
  ),
  GuideItem(
    title = "2. Habilidade & Permissão Informal",
    modals = "Can / Can't",
    summary = "'Can' expressa habilidade no presente, permissão informal ou possibilidade geral. 'Can't' (ou 'cannot') expressa incapacidade no presente ou impossibilidade.",
    example = "She can speak four languages. / Can I borrow your pen?"
  ),
  GuideItem(
    title = "3. Passado & Pedidos Educados",
    modals = "Could / Couldn't",
    summary = "'Could' indica habilidade geral no passado, pedidos educados ou 'talvez' no presente/futuro. Para algo específico realizado uma única vez no passado, prefira 'was/were able to' ou 'managed to'. Já 'couldn't' funciona em qualquer negativa no passado.",
    example = "When I was five, I could swim. / Could you help me? / I couldn't find my keys."
  ),
  GuideItem(
    title = "4. Possibilidade ('Talvez')",
    modals = "May / Might / Could",
    summary = "Em situações reais no presente ou futuro, 'may', 'might' e 'could' têm o mesmo sentido ('talvez'). Nunca use 'can' para 'talvez' em uma situação específica! Em situações irreais/hipotéticas, use 'might' (não 'may').",
    example = "Take an umbrella — it might/may/could rain. / If they paid better, I might stay."
  ),
  GuideItem(
    title = "5. Escala de Formalidade (Permissão)",
    modals = "Can → Could → May",
    summary = "'Can I...?' é informal (amigos e família). 'Could I...?' é educado e funciona bem quase sempre. 'May I...?' é a forma mais formal (professores, chefes, desconhecidos ou ambiente oficial).",
    example = "Can I sit here? → Could I open the window? → May I leave early, professor?"
  ),
  GuideItem(
    title = "6. Obrigação Interna vs. Externa",
    modals = "Must vs. Have to",
    summary = "'Must' indica obrigação pessoal/interna (o falante sente que é necessário) ou regra escrita/aviso. 'Have to' indica obrigação externa (leis, escola, trabalho). Atenção: no passado, use sempre 'had to' (não existe passado de 'must' para obrigação).",
    example = "I must call my mom. / Drivers have to wear seatbelts. / Yesterday I had to study."
  ),
  GuideItem(
    title = "7. Pegadinha Clássica: Proibição vs. Opcional",
    modals = "Mustn't vs. Don't have to",
    summary = "'Mustn't' = PROIBIÇÃO estrita (não faça de jeito nenhum!). 'Don't have to' = AUSÊNCIA DE OBRIGAÇÃO (é opcional — você não precisa fazer, mas pode se quiser).",
    example = "Visitors mustn't smoke here (proibido) ≠ You don't have to wake up early (opcional)."
  ),
  GuideItem(
    title = "8. Dedução Lógica (Certeza vs. Impossibilidade)",
    modals = "Must / Can't",
    summary = "Para concluir que algo é quase certo com base em evidências, use 'must'. Para concluir que algo é logicamente impossível, use 'can't' — nunca use 'mustn't' para dedução!",
    example = "The lights are on — he must be home. / He just ate — he can't be hungry."
  ),
  GuideItem(
    title = "9. Conselhos, Recomendações & Expectativa",
    modals = "Should / Shouldn't / Ought to",
    summary = "'Should' (ou 'ought to') dá conselhos, recomendações ou expressa expectativa lógica, sendo mais suave que 'must'. 'Shouldn't' indica que algo não é boa ideia (sem ser proibição legal).",
    example = "You should see a doctor. / You shouldn't stay up so late. / The train should arrive at 8."
  ),
  GuideItem(
    title = "10. Cortesia, Ofertas & Situações Hipotéticas",
    modals = "Would",
    summary = "Muito usado em oferecimentos e pedidos bem educados ('Would you like...', 'Would you mind...'), preferências ('I would rather...') e situações imaginárias na 2ª condicional.",
    example = "Would you like some coffee? / Would you mind closing the door? / I would travel the world."
  )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModalQuickGuideSheet(
  onDismiss: () -> Unit
) {
  val extendedColors = QuizTheme.colors
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = extendedColors.cardBackground,
    modifier = Modifier.testTag("modal_quick_guide_sheet")
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
          imageVector = Icons.Rounded.AutoStories,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(26.dp)
        )
        Column {
          Text(
            text = "Guia Rápido dos Modais",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Resumo baseado em English Grammar in Use (Murphy, 5ª ed.)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(2.dp))

      quickGuideItems.forEach { item ->
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
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
            }
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = extendedColors.accentBadgeBg
            ) {
              Text(
                text = item.modals,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = extendedColors.accentBadgeText,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
              )
            }
            Text(
              text = item.summary,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = "Ex.: ${item.example}",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.primary
            )
          }
        }
      }
    }
  }
}
