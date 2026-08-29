package com.mira.miraai.memory

/**
 * User-memory fact row — feature-spec.md Section 4.6. Full repository/consolidation logic is
 * Phase 9/10 scope; this shape is pulled forward to Phase 3 only because [SessionContext]
 * (agent/freestyle) needs it to satisfy the `LLMProvider` interface contract.
 */
data class Fact(
    val id: String,
    val subject: String,
    val predicate: String,
    val objectValue: String,
    val confidence: Float,
    val lastUpdated: Long,
    val sourceSessionId: String,
)
