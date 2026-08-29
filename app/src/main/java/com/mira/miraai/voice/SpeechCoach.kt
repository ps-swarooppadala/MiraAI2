package com.mira.miraai.voice

import android.content.Context
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Android system TextToSpeech wrapper — Piper integration comes in Phase 7 per feature-spec.md Section 9.2. */
class SpeechCoach(context: Context) {
    companion object {
        // needs tuning — placeholder, not sourced from feature-spec.md (see docs/PROGRESS.md)
        const val COOLDOWN_MS = 4000L
    }

    private var isReady = false
    private var lastSpokenAtMs = 0L
    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            isReady = status == TextToSpeech.SUCCESS
            if (isReady) tts.language = Locale.US
        }
    }

    fun speak(line: String) {
        if (!isReady) return
        val now = SystemClock.uptimeMillis()
        if (now - lastSpokenAtMs < COOLDOWN_MS) return
        lastSpokenAtMs = now
        tts.speak(line, TextToSpeech.QUEUE_FLUSH, null, "mira_phase0_cue")
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
