package com.mira.miraai.voice

import com.mira.miraai.agent.freestyle.ActionSchema
import com.mira.miraai.agent.freestyle.AgentPrompt
import com.mira.miraai.agent.freestyle.AgentResponse
import com.mira.miraai.agent.freestyle.SessionContext
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProviderFakesTest {

    @Test
    fun `FakeLLMProvider returns the configured response and records the prompt`() = runTest {
        val response = AgentResponse(ActionSchema.START_WORKOUT, "Let's begin.", "foundations_full_body")
        val provider: LLMProvider = FakeLLMProvider(response)
        val prompt = AgentPrompt(
            userUtterance = "let's start",
            allowedActions = ActionSchema.entries,
            context = SessionContext("evening", null, emptyList()),
        )

        val result = provider.complete(prompt)

        assertEquals(response, result)
        assertEquals(prompt, (provider as FakeLLMProvider).lastPromptSeen)
    }

    @Test
    fun `FakeTTSProvider records spoken lines instead of speaking`() {
        val provider: TTSProvider = FakeTTSProvider()

        provider.speak("Straighten your knee.", Lang.EN)

        assertEquals(
            listOf(FakeTTSProvider.SpokenLine("Straighten your knee.", Lang.EN)),
            (provider as FakeTTSProvider).spokenLines,
        )
    }

    @Test
    fun `FakeSTTProvider emits to the last registered listener`() {
        val provider = FakeSTTProvider()
        var heard: String? = null
        provider.startListening { heard = it }

        provider.emit("start my workout")

        assertEquals("start my workout", heard)
    }

    @Test
    fun `FakeSTTProvider without a listener does not crash on emit`() {
        val provider = FakeSTTProvider()

        provider.emit("nobody listening")

        assertNull(null) // reaching here without throwing is the assertion
    }

    @Test
    fun `StubLLMProvider always falls back to ANSWER_SMALLTALK`() = runTest {
        val provider: LLMProvider = StubLLMProvider()
        val prompt = AgentPrompt("anything", ActionSchema.entries, SessionContext("morning", null, emptyList()))

        val result = provider.complete(prompt)

        assertEquals(ActionSchema.ANSWER_SMALLTALK, result.action)
        assertNull(result.resolvedRoutineId)
    }
}
