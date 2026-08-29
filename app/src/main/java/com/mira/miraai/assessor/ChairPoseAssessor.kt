package com.mira.miraai.assessor

import com.mira.miraai.perception.BodyJoint
import com.mira.miraai.perception.PoseFrame
import kotlin.math.abs

/** Single-frame Chair Pose assessment result — same contract as [WarriorIIVerdict]. */
data class ChairPoseVerdict(
    override val verdictCode: VerdictCode,
    override val hasCriticalIssue: Boolean,
    override val confidence: Float,
) : Verdict

/**
 * Deterministic Chair Pose rule engine, pure Kotlin (CLAUDE.md). Unlike Warrior II/Tree Pose,
 * Chair Pose is symmetric (`side = NONE` in content.json — content/RoutineStep) so [assess] takes
 * no [com.mira.miraai.perception.Side]; both legs are checked independently and either one
 * tripping a tolerance is enough to flag the issue.
 *
 * No issue here is treated as critical (unlike Warrior II's knee-past-ankle or Tree's bent
 * standing leg) — a two-footed squat carries no comparable fall risk, so `hasCriticalIssue` is
 * only ever true for [VerdictCode.INSUFFICIENT_VISIBILITY]. A design decision made where the spec
 * is silent, flagged here rather than guessed silently.
 */
class ChairPoseAssessor {

    fun assess(frame: PoseFrame): ChairPoseVerdict {
        val keyJoints = listOf(
            BodyJoint.LEFT_HIP, BodyJoint.RIGHT_HIP,
            BodyJoint.LEFT_KNEE, BodyJoint.RIGHT_KNEE,
            BodyJoint.LEFT_ANKLE, BodyJoint.RIGHT_ANKLE,
            BodyJoint.LEFT_SHOULDER, BodyJoint.RIGHT_SHOULDER,
            BodyJoint.LEFT_ELBOW, BodyJoint.RIGHT_ELBOW,
            BodyJoint.LEFT_WRIST, BodyJoint.RIGHT_WRIST,
        )
        val confidence = keyJoints.map { frame.landmark(it)?.visibility ?: 0f }.average().toFloat()

        if (keyJoints.any { (frame.landmark(it)?.visibility ?: 0f) < ChairPoseThresholds.MIN_LANDMARK_VISIBILITY }) {
            return ChairPoseVerdict(VerdictCode.INSUFFICIENT_VISIBILITY, hasCriticalIssue = true, confidence = confidence)
        }

        val leftHip = position(frame, BodyJoint.LEFT_HIP)
        val rightHip = position(frame, BodyJoint.RIGHT_HIP)
        val leftKnee = position(frame, BodyJoint.LEFT_KNEE)
        val rightKnee = position(frame, BodyJoint.RIGHT_KNEE)
        val leftAnkle = position(frame, BodyJoint.LEFT_ANKLE)
        val rightAnkle = position(frame, BodyJoint.RIGHT_ANKLE)
        val leftShoulder = position(frame, BodyJoint.LEFT_SHOULDER)
        val rightShoulder = position(frame, BodyJoint.RIGHT_SHOULDER)
        val leftElbow = position(frame, BodyJoint.LEFT_ELBOW)
        val rightElbow = position(frame, BodyJoint.RIGHT_ELBOW)
        val leftWrist = position(frame, BodyJoint.LEFT_WRIST)
        val rightWrist = position(frame, BodyJoint.RIGHT_WRIST)

        val leftKneeAngle = angleDegrees(leftHip, leftKnee, leftAnkle)
        val rightKneeAngle = angleDegrees(rightHip, rightKnee, rightAnkle)
        val avgKneeAngle = (leftKneeAngle + rightKneeAngle) / 2f

        val issues = buildList {
            if (avgKneeAngle > ChairPoseThresholds.KNEE_TARGET_DEG + ChairPoseThresholds.KNEE_TOLERANCE_DEG) {
                add(VerdictCode.CHAIR_NOT_LOW_ENOUGH)
            }
            val leftKneePastToe = abs(leftKnee.x - leftAnkle.x) > ChairPoseThresholds.KNEES_PAST_TOES_TOLERANCE
            val rightKneePastToe = abs(rightKnee.x - rightAnkle.x) > ChairPoseThresholds.KNEES_PAST_TOES_TOLERANCE
            if (leftKneePastToe || rightKneePastToe) add(VerdictCode.CHAIR_KNEES_PAST_TOES)

            val shoulderMidX = (leftShoulder.x + rightShoulder.x) / 2f
            val hipMidX = (leftHip.x + rightHip.x) / 2f
            if (abs(shoulderMidX - hipMidX) > ChairPoseThresholds.TORSO_FORWARD_TOLERANCE) {
                add(VerdictCode.CHAIR_TORSO_TOO_FORWARD)
            }

            val leftArmRaised = leftWrist.y < leftShoulder.y - ChairPoseThresholds.ARMS_RAISED_TOLERANCE
            val rightArmRaised = rightWrist.y < rightShoulder.y - ChairPoseThresholds.ARMS_RAISED_TOLERANCE
            if (!leftArmRaised || !rightArmRaised) add(VerdictCode.CHAIR_ARMS_NOT_RAISED)

            val leftArmAngle = angleDegrees(leftShoulder, leftElbow, leftWrist)
            val rightArmAngle = angleDegrees(rightShoulder, rightElbow, rightWrist)
            if (leftArmAngle < ChairPoseThresholds.ARM_STRAIGHT_MIN_DEG || rightArmAngle < ChairPoseThresholds.ARM_STRAIGHT_MIN_DEG) {
                add(VerdictCode.CHAIR_ARMS_NOT_STRAIGHT)
            }
        }

        val verdictCode = issues.firstOrNull() ?: VerdictCode.GOOD_FORM
        return ChairPoseVerdict(verdictCode, hasCriticalIssue = false, confidence = confidence)
    }

    private fun position(frame: PoseFrame, joint: BodyJoint): Point2D = frame.landmarks.getValue(joint).position
}
