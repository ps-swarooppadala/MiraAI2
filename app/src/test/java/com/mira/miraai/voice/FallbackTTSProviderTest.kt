package com.mira.miraai.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class ThrowingTTSProvider(private val error: Throwable) : TTSProvider {
    override fun speak(text: String, lang: Lang) = throw error
}

private class ShutdownRecordingTTSProvider : TTSProvider, TTSShutdown {
    var didShutdown = false
    override fun speak(text: String, lang: Lang) {}
    override fun shutdown() { didShutdown = true }
}

class FallbackTTSProviderTest {

    @Test
    fun `given primary speaks fine, when speak is called, then fallback is never invoked`() {
        val primary = FakeTTSProvider()
        val fallback = FakeTTSProvider()
        val provider = FallbackTTSProvider(primary, fallback)

        provider.speak("Bend your knee.", Lang.EN)

        assertEquals(listOf(FakeTTSProvider.SpokenLine("Bend your knee.", Lang.EN)), primary.spokenLines)
        assertTrue(fallback.spokenLines.isEmpty())
    }

    @Test
    fun `given primary throws, when speak is called, then fallback speaks the same line`() {
        val primary = ThrowingTTSProvider(PiperUnavailableException("no model bundled"))
        val fallback = FakeTTSProvider()
        val provider = FallbackTTSProvider(primary, fallback)

        provider.speak("Straighten your back leg.", Lang.EN)

        assertEquals(
            listOf(FakeTTSProvider.SpokenLine("Straighten your back leg.", Lang.EN)),
            fallback.spokenLines,
        )
    }

    @Test
    fun `given primary throws, when speak is called, then onFallback is notified not silently swallowed`() {
        val error = PiperUnavailableException("no model bundled")
        val primary = ThrowingTTSProvider(error)
        val fallback = FakeTTSProvider()
        var notified: Throwable? = null
        val provider = FallbackTTSProvider(primary, fallback, onFallback = { notified = it })

        provider.speak("Level your arms.", Lang.EN)

        assertEquals(error, notified)
    }

    @Test
    fun `shutdown delegates to both providers when they support it`() {
        val primary = ShutdownRecordingTTSProvider()
        val fallback = ShutdownRecordingTTSProvider()
        val provider = FallbackTTSProvider(primary, fallback)

        provider.shutdown()

        assertTrue(primary.didShutdown)
        assertTrue(fallback.didShutdown)
    }

    @Test
    fun `shutdown does not crash when a provider does not support TTSShutdown`() {
        val provider = FallbackTTSProvider(FakeTTSProvider(), FakeTTSProvider())

        provider.shutdown()
    }
}
