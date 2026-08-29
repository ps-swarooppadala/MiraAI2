package com.mira.miraai.memory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fixed session-history fixtures per this phase's instruction — no randomness, every call feeds
 * a hand-authored [SessionData].
 */
class ConsolidationTest {

    private fun sessionWithKneeIssue(sessionId: String, endedAtMs: Long, holdSeconds: Int = 20) = SessionData(
        sessionId = sessionId,
        endedAtMs = endedAtMs,
        poseAttempts = listOf(
            PoseAttemptRecord(
                pose = "warrior_ii",
                holdSeconds = holdSeconds,
                issuesDetected = listOf("FRONT_KNEE_PAST_ANKLE"),
                improved = false,
            ),
        ),
    )

    @Test
    fun `a new issue is inserted as a low-confidence Fact`() {
        val facts = consolidate(sessionWithKneeIssue("s1", endedAtMs = 1000L), existingFacts = emptyList())

        val fact = facts.single { it.predicate == "struggles_with" }
        assertEquals("user.warrior_ii", fact.subject)
        assertEquals("front_knee_past_ankle", fact.objectValue)
        assertEquals(ConsolidationThresholds.NEW_FACT_CONFIDENCE, fact.confidence)
        assertEquals("s1", fact.sourceSessionId)
    }

    @Test
    fun `the same issue across 3 sessions raises confidence each time`() {
        var facts = consolidate(sessionWithKneeIssue("s1", 1000L), emptyList())
        val afterOne = facts.single { it.predicate == "struggles_with" }.confidence

        facts = consolidate(sessionWithKneeIssue("s2", 2000L), facts)
        val afterTwo = facts.single { it.predicate == "struggles_with" }.confidence

        facts = consolidate(sessionWithKneeIssue("s3", 3000L), facts)
        val afterThree = facts.single { it.predicate == "struggles_with" }.confidence

        assertTrue("confidence should rise session over session", afterTwo > afterOne)
        assertTrue("confidence should rise session over session", afterThree > afterTwo)
        assertEquals("s3", facts.single { it.predicate == "struggles_with" }.sourceSessionId)
    }

    @Test
    fun `a different issue on the same pose produces a separate Fact, not a merge`() {
        val kneeSession = sessionWithKneeIssue("s1", 1000L)
        val armSession = SessionData(
            sessionId = "s2",
            endedAtMs = 2000L,
            poseAttempts = listOf(
                PoseAttemptRecord(pose = "warrior_ii", holdSeconds = 20, issuesDetected = listOf("ARMS_NOT_LEVEL"), improved = false),
            ),
        )

        val facts = consolidate(armSession, consolidate(kneeSession, emptyList()))

        val struggleFacts = facts.filter { it.predicate == "struggles_with" }
        assertEquals(2, struggleFacts.size)
        assertTrue(struggleFacts.any { it.objectValue == "front_knee_past_ankle" })
        assertTrue(struggleFacts.any { it.objectValue == "arms_not_level" })
    }

    @Test
    fun `average hold time is tracked as a running Fact`() {
        val facts = consolidate(sessionWithKneeIssue("s1", 1000L, holdSeconds = 18), emptyList())

        val avgHoldFact = facts.single { it.predicate == "avg_hold_time" }
        assertEquals("user", avgHoldFact.subject)
        assertEquals("18s", avgHoldFact.objectValue)
    }

    @Test
    fun `a hold-time drop of at least the threshold inserts a shorter_holds preference`() {
        val first = consolidate(sessionWithKneeIssue("s1", 1000L, holdSeconds = 20), emptyList())
        val second = consolidate(sessionWithKneeIssue("s2", 2000L, holdSeconds = 15), first)

        val preference = second.singleOrNull { it.predicate == "prefers" }
        assertEquals("shorter_holds", preference?.objectValue)
    }

    @Test
    fun `a small hold-time drop under the threshold does not insert a preference`() {
        val first = consolidate(sessionWithKneeIssue("s1", 1000L, holdSeconds = 20), emptyList())
        val second = consolidate(sessionWithKneeIssue("s2", 2000L, holdSeconds = 19), first)

        assertNull(second.singleOrNull { it.predicate == "prefers" })
    }

    @Test
    fun `confidence never exceeds the configured maximum`() {
        var facts = emptyList<Fact>()
        repeat(20) { i ->
            facts = consolidate(sessionWithKneeIssue("s$i", i.toLong()), facts)
        }

        val fact = facts.single { it.predicate == "struggles_with" }
        assertEquals(ConsolidationThresholds.MAX_CONFIDENCE, fact.confidence)
    }

    @Test
    fun `a session with no issues and no holds returns existingFacts unchanged in content`() {
        val emptySession = SessionData(sessionId = "s1", endedAtMs = 1000L, poseAttempts = emptyList())

        val facts = consolidate(emptySession, existingFacts = emptyList())

        assertTrue(facts.isEmpty())
    }
}
