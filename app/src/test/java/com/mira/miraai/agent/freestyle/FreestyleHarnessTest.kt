package com.mira.miraai.agent.freestyle

import com.mira.miraai.voice.FakeLLMProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FreestyleHarnessTest {

    private val context = SessionContext(timeOfDay = "evening", lastRoutineId = null, relevantFacts = emptyList())

    @Test
    fun `resolves START_WORKOUT to the default routine when context has no last routine`() = runTest {
        val fake = FakeLLMProvider(AgentResponse(ActionSchema.START_WORKOUT, "Let's start.", resolvedRoutineId = null))
        val harness = FreestyleHarness(fake)

        val response = harness.resolve("let's start my workout", context)

        assertEquals(ActionSchema.START_WORKOUT, response.action)
        assertEquals("warrior_ii_quick_practice", response.resolvedRoutineId)
    }

    @Test
    fun `resolves SUGGEST_WARMUP to the context's last routine when present`() = runTest {
        val fake = FakeLLMProvider(AgentResponse(ActionSchema.SUGGEST_WARMUP, "A quick warm-up.", resolvedRoutineId = null))
        val harness = FreestyleHarness(fake)
        val contextWithHistory = context.copy(lastRoutineId = "foundations_full_body")

        val response = harness.resolve("something easy to warm up", contextWithHistory)

        assertEquals("foundations_full_body", response.resolvedRoutineId)
    }

    @Test
    fun `leaves resolvedRoutineId null for non-workout actions`() = runTest {
        val fake = FakeLLMProvider(AgentResponse(ActionSchema.PAUSE, "Pausing.", resolvedRoutineId = null))
        val harness = FreestyleHarness(fake)

        val response = harness.resolve("pause please", context)

        assertNull(response.resolvedRoutineId)
    }

    @Test
    fun `passes the fixed 8-action allowedActions set to the provider, never a narrowed one`() = runTest {
        val fake = FakeLLMProvider(AgentResponse(ActionSchema.ANSWER_SMALLTALK, "Hm.", resolvedRoutineId = null))
        val harness = FreestyleHarness(fake)

        harness.resolve("anything", context)

        assertEquals(ActionSchema.entries.toSet(), fake.lastPromptSeen?.allowedActions?.toSet())
    }
}
