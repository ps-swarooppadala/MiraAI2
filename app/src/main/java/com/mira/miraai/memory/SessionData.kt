package com.mira.miraai.memory

/**
 * Pure-Kotlin session-history shape fed into [consolidate] — deliberately decoupled from
 * `data.PoseAttemptEntity`/`data.SessionEntity` (which are Room-coupled and live in `data/`) so
 * `memory/` keeps CLAUDE.md's zero-android-import rule and `consolidate()` stays unit-testable
 * against plain fixtures with no Room/Robolectric involved. Mapping a real `SessionEntity` +
 * its `PoseAttemptEntity` rows into this shape is the caller's job (future wiring, not this
 * phase's scope — see docs/PROGRESS.md).
 */
data class SessionData(
    val sessionId: String,
    val endedAtMs: Long,
    val poseAttempts: List<PoseAttemptRecord>,
)

data class PoseAttemptRecord(
    val pose: String,
    val holdSeconds: Int,
    val issuesDetected: List<String>,
    val improved: Boolean,
)
