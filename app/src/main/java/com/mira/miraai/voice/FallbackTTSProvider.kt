package com.mira.miraai.voice

/**
 * Composes a primary [TTSProvider] with an explicit fallback — build-architecture.md Section
 * 2.1: "Piper as primary, Android system TTS as explicit fallback (not silent default)." If
 * [primary] throws, [onFallback] is invoked (for logging — so the fallback is observable, never
 * silent) and [fallback] speaks the same line instead. Pure Kotlin/JUnit-testable: no Android
 * imports, both providers are injected.
 */
class FallbackTTSProvider(
    private val primary: TTSProvider,
    private val fallback: TTSProvider,
    private val onFallback: (Throwable) -> Unit = {},
) : TTSProvider, TTSShutdown {

    override fun speak(text: String, lang: Lang) {
        try {
            primary.speak(text, lang)
        } catch (error: Exception) {
            onFallback(error)
            fallback.speak(text, lang)
        }
    }

    override fun shutdown() {
        (primary as? TTSShutdown)?.shutdown()
        (fallback as? TTSShutdown)?.shutdown()
    }
}
