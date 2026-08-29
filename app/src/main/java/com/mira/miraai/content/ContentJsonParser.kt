package com.mira.miraai.content

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Pure-Kotlin parsing of the bundled content JSON into [ContentBundle] — no Android imports,
 * so it's JUnit-testable without a device (CLAUDE.md's "zero android.* imports" rule applies to
 * assessor/agent/memory only, but this stays pure anyway since it costs nothing and keeps the
 * model layer trivially testable).
 *
 * Drawable resource names in the JSON (e.g. "ic_full_body") are resolved to `@DrawableRes Int`
 * by the caller-supplied [resolveDrawable] — keeping actual Android resource lookup at the edge.
 */

@Serializable
private data class ContentJson(
    val categories: List<CategoryJson>,
    val routines: List<RoutineJson>,
    val poses: List<PoseJson>,
)

@Serializable
private data class CategoryJson(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: String,
    val routineIds: List<String>,
)

@Serializable
private data class RoutineJson(
    val id: String,
    val title: String,
    val categoryIds: List<String>,
    val level: RoutineLevel,
    val estimatedDurationSec: Int,
    val coverImage: String,
    val poseSequence: List<RoutineStepJson>,
)

@Serializable
private data class RoutineStepJson(
    val poseId: String,
    val side: PoseSide,
    val targetHoldSec: Int? = null,
    val targetReps: Int? = null,
    val order: Int,
)

@Serializable
private data class PoseJson(
    val id: String,
    val displayName: String,
    val sanskritName: String? = null,
    val thumbnail: String,
    val instructionSteps: List<String>,
    val defaultHoldSec: Int,
    val trackingMode: TrackingMode,
    val hasAssessorRuleSet: Boolean,
)

private val json = Json { ignoreUnknownKeys = true }

fun parseContentBundle(rawJson: String, resolveDrawable: (String) -> Int): ContentBundle {
    val parsed = json.decodeFromString(ContentJson.serializer(), rawJson)

    val poses = parsed.poses.map { p ->
        Pose(
            id = p.id,
            displayName = p.displayName,
            sanskritName = p.sanskritName,
            thumbnailRes = resolveDrawable(p.thumbnail),
            instructionSteps = p.instructionSteps,
            defaultHoldSec = p.defaultHoldSec,
            trackingMode = p.trackingMode,
            hasAssessorRuleSet = p.hasAssessorRuleSet,
        )
    }
    val hasRuleSetByPoseId = poses.associate { it.id to it.hasAssessorRuleSet }

    val routines = parsed.routines.map { r ->
        val steps = r.poseSequence.map { s ->
            RoutineStep(
                poseId = s.poseId,
                side = s.side,
                targetHoldSec = s.targetHoldSec,
                targetReps = s.targetReps,
                order = s.order,
            )
        }
        Routine(
            id = r.id,
            title = r.title,
            categoryIds = r.categoryIds,
            level = r.level,
            estimatedDurationSec = r.estimatedDurationSec,
            coverImageRes = resolveDrawable(r.coverImage),
            poseSequence = steps.sortedBy { it.order },
            // feature-spec.md Section 4.2: true only if every pose in the sequence has a rule set.
            isCoachingSupported = steps.isNotEmpty() && steps.all { hasRuleSetByPoseId[it.poseId] == true },
        )
    }

    val categories = parsed.categories.map { c ->
        Category(
            id = c.id,
            title = c.title,
            subtitle = c.subtitle,
            iconRes = resolveDrawable(c.icon),
            routineIds = c.routineIds,
        )
    }

    return ContentBundle(categories = categories, routines = routines, poses = poses)
}
