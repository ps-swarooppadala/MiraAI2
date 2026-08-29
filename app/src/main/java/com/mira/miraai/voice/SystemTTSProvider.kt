package com.mira.miraai.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * [TTSProvider] backed by Android's system TextToSpeech — the fallback path per
 * build-architecture.md Section 2's flavor table ("Piper (CPU/QNN) or system TTS fallback").
 * Piper integration is Phase 7 scope; both flavors bind this provider until then.
 */
class SystemTTSProvider(context: Context) : TTSProvider {
    private var isReady = false
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        isReady = status == TextToSpeech.SUCCESS
    }

    override fun speak(text: String, lang: Lang) {
        if (!isReady) return
        @Suppress("DEPRECATION") // Locale.of() needs API 35+; minSdk here is 26.
        tts.language = if (lang == Lang.HI) Locale("hi", "IN") else Locale.US
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "mira_tts_provider")
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
