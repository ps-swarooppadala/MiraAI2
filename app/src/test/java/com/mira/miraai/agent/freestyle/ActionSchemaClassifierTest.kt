package com.mira.miraai.agent.freestyle

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * One test per intent phrase (feature-spec.md Section 4.5's 8 fixed [ActionSchema] values)
 * plus an explicit out-of-schema fallback test, per this phase's instruction.
 */
class ActionSchemaClassifierTest {

    private val classifier = ActionSchemaClassifier()

    @Test
    fun `classifies start workout phrase`() {
        assertEquals(ActionSchema.START_WORKOUT, classifier.classify("let's start my workout"))
    }

    @Test
    fun `classifies suggest warmup phrase`() {
        assertEquals(ActionSchema.SUGGEST_WARMUP, classifier.classify("can we warm up first"))
    }

    @Test
    fun `classifies pause phrase`() {
        assertEquals(ActionSchema.PAUSE, classifier.classify("pause please"))
    }

    @Test
    fun `classifies resume phrase`() {
        assertEquals(ActionSchema.RESUME, classifier.classify("okay let's continue"))
    }

    @Test
    fun `classifies switch pose phrase`() {
        assertEquals(ActionSchema.SWITCH_POSE, classifier.classify("can we do a different pose"))
    }

    @Test
    fun `classifies end session phrase`() {
        assertEquals(ActionSchema.END_SESSION, classifier.classify("i'm done for today"))
    }

    @Test
    fun `classifies show memory phrase`() {
        assertEquals(ActionSchema.SHOW_MEMORY, classifier.classify("what have you learned about me"))
    }

    @Test
    fun `classifies smalltalk phrase`() {
        assertEquals(ActionSchema.ANSWER_SMALLTALK, classifier.classify("how's the weather today"))
    }

    @Test
    fun `out-of-schema utterance falls back to ANSWER_SMALLTALK`() {
        assertEquals(ActionSchema.ANSWER_SMALLTALK, classifier.classify("tell me a joke about penguins"))
    }

    @Test
    fun `complete returns AgentResponse matching the classified action`() = runTest {
        val prompt = AgentPrompt(
            userUtterance = "let's start my workout",
            allowedActions = ActionSchema.entries,
            context = SessionContext(timeOfDay = "evening", lastRoutineId = null, relevantFacts = emptyList()),
        )
        val response = classifier.complete(prompt)
        assertEquals(ActionSchema.START_WORKOUT, response.action)
    }
}
