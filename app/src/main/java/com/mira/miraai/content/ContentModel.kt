package com.mira.miraai.content

/** Canonical content data model — feature-spec.md Section 4.1-4.3. */

data class Category(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconRes: Int,
    val routineIds: List<String>,
)

data class Routine(
    val id: String,
    val title: String,
    val categoryIds: List<String>,
    val level: RoutineLevel,
    val estimatedDurationSec: Int,
    val coverImageRes: Int,
    val poseSequence: List<RoutineStep>,
    val isCoachingSupported: Boolean,
)

data class RoutineStep(
    val poseId: String,
    val side: PoseSide,
    val targetHoldSec: Int?,
    val targetReps: Int?,
    val order: Int,
)

enum class RoutineLevel { BEGINNER, INTERMEDIATE, ADVANCED }
enum class PoseSide { NONE, LEFT, RIGHT, BOTH }

data class Pose(
    val id: String,
    val displayName: String,
    val sanskritName: String?,
    val thumbnailRes: Int,
    val instructionSteps: List<String>,
    val defaultHoldSec: Int,
    val trackingMode: TrackingMode,
    val hasAssessorRuleSet: Boolean,
)

enum class TrackingMode { HOLD, REP_COUNT }

/** Everything a screen needs, resolved from bundled JSON. */
data class ContentBundle(
    val categories: List<Category>,
    val routines: List<Routine>,
    val poses: List<Pose>,
)
