package com.mira.miraai.memory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 11 — Memory inspector, JSON export fallback (build-architecture.md Section 5).
 * Given a list of Facts, when exported, then the result is a D3 force-graph-shaped
 * node/link JSON document that a laptop script can render offline.
 */
class MemoryExportTest {

    private val fact1 = Fact(
        id = "f1",
        subject = "user.warrior_ii",
        predicate = "struggles_with",
        objectValue = "front_knee_past_ankle",
        confidence = 0.8f,
        lastUpdated = 1_000L,
        sourceSessionId = "s3",
    )
    private val fact2 = Fact(
        id = "f2",
        subject = "user",
        predicate = "prefers",
        objectValue = "shorter_holds",
        confidence = 0.4f,
        lastUpdated = 2_000L,
        sourceSessionId = "s2",
    )

    @Test
    fun `given no facts, when exported, then nodes and links are both empty`() {
        val export = buildMemoryGraphExport(emptyList())
        assertTrue(export.nodes.isEmpty())
        assertTrue(export.links.isEmpty())
    }

    @Test
    fun `given facts with distinct subjects, when exported, then one node per distinct subject-or-object plus one link per fact`() {
        val export = buildMemoryGraphExport(listOf(fact1, fact2))

        // subjects: user.warrior_ii, user ; objects: front_knee_past_ankle, shorter_holds -> 4 distinct nodes
        assertEquals(4, export.nodes.size)
        assertEquals(2, export.links.size)
    }

    @Test
    fun `given two facts sharing a subject, when exported, then that subject is a single deduplicated node`() {
        val sharedSubjectFact = fact2.copy(id = "f3", subject = "user", objectValue = "avg_hold_time_18s")
        val export = buildMemoryGraphExport(listOf(fact2, sharedSubjectFact))

        val userNodes = export.nodes.filter { it.id == "user" }
        assertEquals(1, userNodes.size)
        assertEquals(2, export.links.size)
    }

    @Test
    fun `given a fact, when exported, then its link carries predicate, confidence, lastUpdated and sourceSessionId`() {
        val export = buildMemoryGraphExport(listOf(fact1))
        val link = export.links.single()

        assertEquals("user.warrior_ii", link.source)
        assertEquals("front_knee_past_ankle", link.target)
        assertEquals("struggles_with", link.predicate)
        assertEquals(0.8f, link.confidence)
        assertEquals(1_000L, link.lastUpdated)
        assertEquals("s3", link.sourceSessionId)
    }

    @Test
    fun `given facts, when serialized to JSON, then it round-trips through kotlinx serialization`() {
        val export = buildMemoryGraphExport(listOf(fact1, fact2))
        val json = export.toJson()
        val parsed = MemoryGraphExport.fromJson(json)

        assertEquals(export, parsed)
    }

    @Test
    fun `given facts, when serialized, then the JSON contains readable node and link keys`() {
        val json = buildMemoryGraphExport(listOf(fact1)).toJson()
        assertTrue(json.contains("\"nodes\""))
        assertTrue(json.contains("\"links\""))
        assertTrue(json.contains("struggles_with"))
    }
}
