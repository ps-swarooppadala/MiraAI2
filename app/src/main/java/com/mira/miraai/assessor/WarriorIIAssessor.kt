package com.mira.miraai.assessor

import com.mira.miraai.perception.BodyJoint
import com.mira.miraai.perception.PoseFrame
import com.mira.miraai.perception.Side
import kotlin.math.abs

/** Single-frame Warrior II assessment result — feature-spec.md Section 10.1 Assessor contract. */
data class WarriorIIVerdict(
    val verdictCode: VerdictCode,
    val hasCriticalIssue: Boolean,
    val confidence: Float
)

/**
 * Deterministic Warrior II rule engine. Pure Kotlin — no Android/CameraX/Compose imports
 * (CLAUDE.md). Wired to the real camera pipeline in Phase 4 (build-architecture.md Section 7),
 * replacing the Phase 0 `ElbowCheck` placeholder (removed that phase).
 */
class WarriorIIAssessor {

    private val criticalCodes = setOf(VerdictCode.FRONT_KNEE_PAST_ANKLE, VerdictCode.INSUFFICIENT_VISIBILITY)

    fun assess(frame: PoseFrame, frontLeg: Side): WarriorIIVerdict {
        val backLeg = if (frontLeg == Side.LEFT) Side.RIGHT else Side.LEFT
        val keyJoints = keyJoints(frontLeg, backLeg)

        val confidence = keyJoints.map { frame.landmark(it)?.visibility ?: 0f }.average().toFloat()

        if (keyJoints.any { (frame.landmark(it)?.visibility ?: 0f) < WarriorIIThresholds.MIN_LANDMARK_VISIBILITY }) {
            return WarriorIIVerdict(VerdictCode.INSUFFICIENT_VISIBILITY, hasCriticalIssue = true, confidence = confidence)
        }

        val frontHip = position(frame, hipJoint(frontLeg))
        val frontKnee = position(frame, kneeJoint(frontLeg))
        val frontAnkle = position(frame, ankleJoint(frontLeg))
        val backHip = position(frame, hipJoint(backLeg))
        val backKnee = position(frame, kneeJoint(backLeg))
        val backAnkle = position(frame, ankleJoint(backLeg))
        val leftShoulder = position(frame, BodyJoint.LEFT_SHOULDER)
        val rightShoulder = position(frame, BodyJoint.RIGHT_SHOULDER)
        val leftWrist = position(frame, BodyJoint.LEFT_WRIST)
        val rightWrist = position(frame, BodyJoint.RIGHT_WRIST)

        val frontKneeAngle = angleDegrees(frontHip, frontKnee, frontAnkle)
        val backLegAngle = angleDegrees(backHip, backKnee, backAnkle)

        // Positive when the knee has drifted past the ankle in the forward-lunge direction
        // implied by which leg is front (mirrored for the opposite side).
        val pastAnkleSign = if (frontLeg == Side.LEFT) 1f else -1f
        val kneePastAnkle = pastAnkleSign * (frontAnkle.x - frontKnee.x) > WarriorIIThresholds.FRONT_KNEE_PAST_ANKLE_TOLERANCE

        val issues = buildList {
            if (kneePastAnkle) add(VerdictCode.FRONT_KNEE_PAST_ANKLE)
            if (frontKneeAngle > WarriorIIThresholds.FRONT_KNEE_TARGET_DEG + WarriorIIThresholds.FRONT_KNEE_TOLERANCE_DEG) {
                add(VerdictCode.FRONT_KNEE_TOO_STRAIGHT)
            } else if (frontKneeAngle < WarriorIIThresholds.FRONT_KNEE_TARGET_DEG - WarriorIIThresholds.FRONT_KNEE_TOLERANCE_DEG) {
                add(VerdictCode.FRONT_KNEE_OVER_BENT)
            }
            if (backLegAngle < WarriorIIThresholds.BACK_LEG_STRAIGHT_MIN_DEG) add(VerdictCode.BACK_LEG_BENT)
            val leftArmOffset = abs(leftWrist.y - leftShoulder.y)
            val rightArmOffset = abs(rightWrist.y - rightShoulder.y)
            if (leftArmOffset > WarriorIIThresholds.ARM_LEVEL_TOLERANCE || rightArmOffset > WarriorIIThresholds.ARM_LEVEL_TOLERANCE) {
                add(VerdictCode.ARMS_NOT_LEVEL)
            }
            val shoulderMidX = (leftShoulder.x + rightShoulder.x) / 2f
            val hipMidX = (frontHip.x + backHip.x) / 2f
            if (abs(shoulderMidX - hipMidX) > WarriorIIThresholds.TORSO_UPRIGHT_TOLERANCE) add(VerdictCode.TORSO_LEANING)
        }

        // Critical issues are appended first, so the first entry is always the highest priority.
        val verdictCode = issues.firstOrNull() ?: VerdictCode.GOOD_FORM
        return WarriorIIVerdict(verdictCode, hasCriticalIssue = verdictCode in criticalCodes, confidence = confidence)
    }

    /**
     * The front-knee angle alone, for the live overlay (feature-spec.md F11) — the one joint
     * angle the overlay highlights while Warrior II is being assessed. Returns null when the
     * required landmarks aren't present, same visibility contract as [assess].
     */
    fun frontKneeAngleDeg(frame: PoseFrame, frontLeg: Side): Float? {
        val hip = frame.landmark(hipJoint(frontLeg)) ?: return null
        val knee = frame.landmark(kneeJoint(frontLeg)) ?: return null
        val ankle = frame.landmark(ankleJoint(frontLeg)) ?: return null
        return angleDegrees(hip.position, knee.position, ankle.position)
    }

    private fun keyJoints(frontLeg: Side, backLeg: Side) = listOf(
        hipJoint(frontLeg), kneeJoint(frontLeg), ankleJoint(frontLeg),
        hipJoint(backLeg), kneeJoint(backLeg), ankleJoint(backLeg),
        BodyJoint.LEFT_SHOULDER, BodyJoint.RIGHT_SHOULDER,
        BodyJoint.LEFT_WRIST, BodyJoint.RIGHT_WRIST,
    )

    private fun hipJoint(side: Side) = if (side == Side.LEFT) BodyJoint.LEFT_HIP else BodyJoint.RIGHT_HIP
    private fun kneeJoint(side: Side) = if (side == Side.LEFT) BodyJoint.LEFT_KNEE else BodyJoint.RIGHT_KNEE
    private fun ankleJoint(side: Side) = if (side == Side.LEFT) BodyJoint.LEFT_ANKLE else BodyJoint.RIGHT_ANKLE

    private fun position(frame: PoseFrame, joint: BodyJoint): Point2D = frame.landmarks.getValue(joint).position
}
