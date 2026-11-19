package com.example.ui.quiz

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Synthesizes pleasant, low-latency PCM audio feedback chimes for quiz answers
 * without requiring external media files or permissions.
 */
class QuizSoundPlayer {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val sampleRate = 22050

  private val correctPcm: ShortArray by lazy {
    buildSequence(
      notes = listOf(
        NoteSpec(frequencyHz = 523.25, durationMs = 95, gain = 0.35),  // C5
        NoteSpec(frequencyHz = 659.25, durationMs = 95, gain = 0.38),  // E5
        NoteSpec(frequencyHz = 783.99, durationMs = 170, gain = 0.42)  // G5
      )
    )
  }

  private val maybePcm: ShortArray by lazy {
    buildSequence(
      notes = listOf(
        NoteSpec(frequencyHz = 440.00, durationMs = 120, gain = 0.40), // A4
        NoteSpec(frequencyHz = 554.37, durationMs = 190, gain = 0.42)  // C#5
      )
    )
  }

  private val incorrectPcm: ShortArray by lazy {
    buildSequence(
      notes = listOf(
        NoteSpec(frequencyHz = 369.99, durationMs = 155, gain = 0.56, isErrorTone = true), // F#4
        NoteSpec(frequencyHz = 277.18, durationMs = 260, gain = 0.60, isErrorTone = true)  // C#4
      )
    )
  }

  fun playAnswerFeedback(outcome: AnswerOutcome) {
    val samples = when (outcome) {
      AnswerOutcome.CORRECT -> correctPcm
      AnswerOutcome.MAYBE -> maybePcm
      AnswerOutcome.WRONG -> incorrectPcm
    }
    scope.launch {
      playPcm(samples)
    }
  }

  fun release() {
    scope.cancel()
  }

  private fun playPcm(samples: ShortArray) {
    runCatching {
      val bufferBytes = samples.size * 2
      val track = AudioTrack.Builder()
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        )
        .setAudioFormat(
          AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        )
        .setBufferSizeInBytes(bufferBytes)
        .setTransferMode(AudioTrack.MODE_STATIC)
        .build()

      if (track.state == AudioTrack.STATE_INITIALIZED) {
        track.write(samples, 0, samples.size)
        track.play()
        val durationMs = (samples.size * 1000L) / sampleRate + 60L
        Thread.sleep(durationMs)
        track.stop()
      }
      track.release()
    }
  }

  private fun buildSequence(notes: List<NoteSpec>): ShortArray {
    val totalSamples = notes.sumOf { (sampleRate * it.durationMs) / 1000 }
    val output = ShortArray(totalSamples)
    var offset = 0

    for (note in notes) {
      val count = (sampleRate * note.durationMs) / 1000
      val fadeInSamples = (sampleRate * 0.008).toInt().coerceAtLeast(1)
      val decayFactor = if (note.isErrorTone) 2.2 else 4.2
      for (i in 0 until count) {
        val t = i.toDouble() / sampleRate
        val attack = if (i < fadeInSamples) i.toDouble() / fadeInSamples else 1.0
        val decay = exp(-decayFactor * i.toDouble() / count)
        val fundamental = sin(2.0 * PI * note.frequencyHz * t)
        val harmonic2 = (if (note.isErrorTone) 0.42 else 0.22) * sin(2.0 * PI * (note.frequencyHz * 2.0) * t)
        val harmonic3 = if (note.isErrorTone) 0.24 * sin(2.0 * PI * (note.frequencyHz * 3.0) * t) else 0.0
        val value = (fundamental + harmonic2 + harmonic3) * note.gain * attack * decay
        val sample = (value * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
        output[offset + i] = sample.toShort()
      }
      offset += count
    }
    return output
  }

  private data class NoteSpec(
    val frequencyHz: Double,
    val durationMs: Int,
    val gain: Double,
    val isErrorTone: Boolean = false
  )
}
