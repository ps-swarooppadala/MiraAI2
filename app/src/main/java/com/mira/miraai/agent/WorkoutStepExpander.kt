package com.mira.miraai.agent

import com.mira.miraai.content.PoseSide
import com.mira.miraai.content.Routine
import com.mira.miraai.perception.Side

/**
 * One concrete player step. A content [com.mira.miraai.content.RoutineStep] with
 * `side == BOTH` expands into two of these — feature-spec.md Section 4.2's RoutineStep doc
 * comment ("renders as 2 steps if BOTH").
 */
data class ExpandedStep(
    val poseId: String,
    val side: Side?,
    val targetHoldSec: Int?,
    val targetReps: Int?,
)

fun expandRoutineSteps(routine: Routine): List<ExpandedStep> {
    val expanded = mutableListOf<ExpandedStep>()
    routine.poseSequence.sortedBy { it.order }.forEach { step ->
        when (step.side) {
            PoseSide.BOTH -> {
                expanded += ExpandedStep(step.poseId, Side.LEFT, step.targetHoldSec, step.targetReps)
                expanded += ExpandedStep(step.poseId, Side.RIGHT, step.targetHoldSec, step.targetReps)
            }
            PoseSide.LEFT -> expanded += ExpandedStep(step.poseId, Side.LEFT, step.targetHoldSec, step.targetReps)
            PoseSide.RIGHT -> expanded += ExpandedStep(step.poseId, Side.RIGHT, step.targetHoldSec, step.targetReps)
            PoseSide.NONE -> expanded += ExpandedStep(step.poseId, null, step.targetHoldSec, step.targetReps)
        }
    }
    return expanded
}
