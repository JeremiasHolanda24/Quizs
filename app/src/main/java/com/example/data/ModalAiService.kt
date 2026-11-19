package com.example.data

import com.example.ui.quiz.AnswerOutcome
import com.example.ui.quiz.QuizQuestion
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

class ModalAiService {

  private val tutorModel by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.8-flash"
    )
  }

  fun explainQuestionStream(
    question: QuizQuestion,
    userAnswer: String,
    outcome: AnswerOutcome
  ): Flow<String> = flow {
    val outcomeLabel = when (outcome) {
      AnswerOutcome.CORRECT -> "acertou"
      AnswerOutcome.MAYBE -> "escolheu uma opção aceitável dependendo do contexto (Talvez)"
      AnswerOutcome.WRONG -> "errou"
    }
    val prompt = """
      Você é um tutor amigável de inglês baseado em "English Grammar in Use (Raymond Murphy)".
      Um aluno brasileiro acabou de responder a esta questão e $outcomeLabel:
      - Situação: ${question.context}
      - Frase: ${question.sentence}
      - Resposta do aluno: $userAnswer
      - Gabarito ideal: ${question.acceptedAnswers.joinToString(" / ")}
      - Categoria: ${question.category}

      Escreva uma explicação curta e super clara em português (máximo 180 palavras) com:
      1. Por que "${question.acceptedAnswers.first()}" é a melhor escolha aqui (e, se o aluno errou ou respondeu Talvez, qual é a diferença prática em relação a "$userAnswer").
      2. 3 exemplos novos do dia a dia em inglês com tradução rápida.
      3. 1 dica de memorização em uma frase.
      Não use blocos de código nem títulos exagerados.
    """.trimIndent()

    withTimeout(60_000L) {
      tutorModel.generateContentStream(prompt).collect { chunk ->
        val text = chunk.text
        if (!text.isNullOrEmpty()) {
          emit(text)
        }
      }
    }
  }
}
