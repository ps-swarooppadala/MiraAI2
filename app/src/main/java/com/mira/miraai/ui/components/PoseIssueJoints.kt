package com.mira.miraai.ui.components

import com.mira.miraai.assessor.VerdictCode
import com.mira.miraai.perception.BodyJoint
import com.mira.miraai.perception.Side

/**
 * Maps a live [VerdictCode] to the joints responsible for it, so [PoseOverlay] can color the
 * specific joints at fault (orange) instead of tinting the whole skeleton on any single issue.
 * [side] is the front/standing leg for Warrior II/Tree Pose (null for the symmetric Chair Pose)
 * — same parameter the assessors themselves take.
 *
 * Presentation-layer grouping, not assessor logic — deliberately kept out of assessor/ (CLAUDE.md
 * bars Compose/Android imports there) since "which joints to color" is a UI concern, not a rule.
 */
object PoseIssueJoints {

    fun forVerdict(code: VerdictCode, side: Side?): Set<BodyJoint> = when (code) {
        VerdictCode.GOOD_FORM -> emptySet()
        // No single joint is "wrong" here — the problem is the camera can't see enough of the
        // body to judge anything, so every tracked joint reads as unverified/incorrect.
        VerdictCode.INSUFFICIENT_VISIBILITY -> BodyJoint.entries.toSet()

        VerdictCode.FRONT_KNEE_TOO_STRAIGHT,
        VerdictCode.FRONT_KNEE_OVER_BENT,
        VerdictCode.FRONT_KNEE_PAST_ANKLE -> side.legJoints()

        VerdictCode.BACK_LEG_BENT -> side?.opposite().legJoints()

        VerdictCode.ARMS_NOT_LEVEL,
        VerdictCode.ARMS_NOT_STRAIGHT,
        VerdictCode.CHAIR_ARMS_NOT_RAISED,
        VerdictCode.CHAIR_ARMS_NOT_STRAIGHT -> ARM_JOINTS

        VerdictCode.TORSO_LEANING,
        VerdictCode.CHAIR_TORSO_TOO_FORWARD -> TORSO_JOINTS

        VerdictCode.TREE_STANDING_LEG_BENT -> side.legJoints()
        VerdictCode.TREE_LIFTED_FOOT_TOO_LOW -> side?.opposite()?.let { setOf(ankleJoint(it)) } ?: emptySet()
        VerdictCode.TREE_HIPS_NOT_LEVEL -> setOf(BodyJoint.LEFT_HIP, BodyJoint.RIGHT_HIP)
        VerdictCode.TREE_ARMS_NOT_AT_CHEST -> setOf(
            BodyJoint.LEFT_SHOULDER, BodyJoint.RIGHT_SHOULDER,
            BodyJoint.LEFT_ELBOW, BodyJoint.RIGHT_ELBOW,
            BodyJoint.LEFT_WRIST, BodyJoint.RIGHT_WRIST,
        )
        VerdictCode.TREE_HEAD_TILTED -> setOf(BodyJoint.LEFT_EAR, BodyJoint.RIGHT_EAR)

        VerdictCode.CHAIR_NOT_LOW_ENOUGH -> setOf(
            BodyJoint.LEFT_HIP, BodyJoint.RIGHT_HIP,
            BodyJoint.LEFT_KNEE, BodyJoint.RIGHT_KNEE,
            BodyJoint.LEFT_ANKLE, BodyJoint.RIGHT_ANKLE,
        )
        VerdictCode.CHAIR_KNEES_PAST_TOES -> setOf(
            BodyJoint.LEFT_KNEE, BodyJoint.RIGHT_KNEE,
            BodyJoint.LEFT_ANKLE, BodyJoint.RIGHT_ANKLE,
        )
    }

    private val ARM_JOINTS = setOf(
        BodyJoint.LEFT_SHOULDER, BodyJoint.RIGHT_SHOULDER,
        BodyJoint.LEFT_ELBOW, BodyJoint.RIGHT_ELBOW,
        BodyJoint.LEFT_WRIST, BodyJoint.RIGHT_WRIST,
    )
    private val TORSO_JOINTS = setOf(
        BodyJoint.LEFT_SHOULDER, BodyJoint.RIGHT_SHOULDER,
        BodyJoint.LEFT_HIP, BodyJoint.RIGHT_HIP,
    )

    private fun Side?.legJoints(): Set<BodyJoint> = this?.let { setOf(hipJoint(it), kneeJoint(it), ankleJoint(it)) } ?: emptySet()
    private fun Side.opposite(): Side = if (this == Side.LEFT) Side.RIGHT else Side.LEFT
    private fun hipJoint(side: Side) = if (side == Side.LEFT) BodyJoint.LEFT_HIP else BodyJoint.RIGHT_HIP
    private fun kneeJoint(side: Side) = if (side == Side.LEFT) BodyJoint.LEFT_KNEE else BodyJoint.RIGHT_KNEE
    private fun ankleJoint(side: Side) = if (side == Side.LEFT) BodyJoint.LEFT_ANKLE else BodyJoint.RIGHT_ANKLE
}
