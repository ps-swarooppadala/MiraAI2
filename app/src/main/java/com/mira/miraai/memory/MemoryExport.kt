package com.mira.miraai.memory

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Phase 11 — Memory inspector, JSON export fallback (build-architecture.md Section 5): "Export
 * memory" dumps [Fact] rows to JSON for a laptop script (Python/networkx or an HTML artifact) to
 * render offline, as the recorded backup if on-device WiFi (the live-server path) is unreliable.
 *
 * Shaped as D3 force-graph nodes/links rather than a raw [Fact] list so this same JSON also
 * feeds Section 5's primary embedded-server D3 page later without a second format.
 */
@Serializable
data class MemoryGraphNode(val id: String)

@Serializable
data class MemoryGraphLink(
    val source: String,
    val target: String,
    val predicate: String,
    val confidence: Float,
    val lastUpdated: Long,
    val sourceSessionId: String,
)

@Serializable
data class MemoryGraphExport(
    val nodes: List<MemoryGraphNode>,
    val links: List<MemoryGraphLink>,
) {
    fun toJson(): String = Json.encodeToString(serializer(), this)

    companion object {
        fun fromJson(json: String): MemoryGraphExport = Json.decodeFromString(serializer(), json)
    }
}

fun buildMemoryGraphExport(facts: List<Fact>): MemoryGraphExport {
    val nodeIds = LinkedHashSet<String>()
    val links = facts.map { fact ->
        nodeIds.add(fact.subject)
        nodeIds.add(fact.objectValue)
        MemoryGraphLink(
            source = fact.subject,
            target = fact.objectValue,
            predicate = fact.predicate,
            confidence = fact.confidence,
            lastUpdated = fact.lastUpdated,
            sourceSessionId = fact.sourceSessionId,
        )
    }
    return MemoryGraphExport(nodes = nodeIds.map { MemoryGraphNode(it) }, links = links)
}
