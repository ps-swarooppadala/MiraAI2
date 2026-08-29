package com.mira.miraai.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entities — build-architecture.md Section 4, transcribed field-for-field. Lives in
 * `data/` (not `memory/`) because Room is Android/SQLite-coupled; CLAUDE.md's zero-android-import
 * rule applies to `assessor/`, `agent/`, and `memory/` only, per Section 9.3's package doc
 * ("data/ — Room/DataStore for local session history + Fact table").
 *
 * `posesPracticed` and `issuesDetected` are stored as comma-joined strings rather than a Room
 * `TypeConverter`-backed `List<String>` — the simplest thing that satisfies the spec's plain
 * `String` field type without adding converter machinery this phase doesn't need yet.
 */
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val startedAt: Long,
    val endedAt: Long?,
    val posesPracticed: String,
)

@Entity(tableName = "pose_attempts")
data class PoseAttemptEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val pose: String,
    val holdSeconds: Int,
    val maxAngleDeviation: Float,
    val issuesDetected: String,
    val improved: Boolean,
)

@Entity(tableName = "cues")
data class CueEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val timestamp: Long,
    val intent: String,
    val text: String,
    val issueCode: String?,
)
