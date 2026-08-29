package com.mira.miraai.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mira.miraai.assessor.WarriorIIThresholds
import com.mira.miraai.perception.BodyJoint
import com.mira.miraai.perception.PoseFrame
import com.mira.miraai.ui.theme.MiraColors

/** Skeleton line segments drawn over the camera preview — feature-spec.md Section 8.1 item 2 / F11. */
private val SKELETON_CONNECTIONS = listOf(
    // Neck/head — approximated from shoulders and nose/ears since BlazePose has no neck point.
    BodyJoint.LEFT_SHOULDER to BodyJoint.NOSE,
    BodyJoint.RIGHT_SHOULDER to BodyJoint.NOSE,
    // Head-tilt reference line: level when the head is upright, sloped when it's tilted.
    BodyJoint.LEFT_EAR to BodyJoint.RIGHT_EAR,
    BodyJoint.LEFT_SHOULDER to BodyJoint.RIGHT_SHOULDER,
    BodyJoint.LEFT_SHOULDER to BodyJoint.LEFT_HIP,
    BodyJoint.RIGHT_SHOULDER to BodyJoint.RIGHT_HIP,
    BodyJoint.LEFT_HIP to BodyJoint.RIGHT_HIP,
    BodyJoint.LEFT_HIP to BodyJoint.LEFT_KNEE,
    BodyJoint.LEFT_KNEE to BodyJoint.LEFT_ANKLE,
    BodyJoint.RIGHT_HIP to BodyJoint.RIGHT_KNEE,
    BodyJoint.RIGHT_KNEE to BodyJoint.RIGHT_ANKLE,
    // Arms — routed through the elbow instead of a single shoulder-to-wrist line.
    BodyJoint.LEFT_SHOULDER to BodyJoint.LEFT_ELBOW,
    BodyJoint.LEFT_ELBOW to BodyJoint.LEFT_WRIST,
    BodyJoint.RIGHT_SHOULDER to BodyJoint.RIGHT_ELBOW,
    BodyJoint.RIGHT_ELBOW to BodyJoint.RIGHT_WRIST,
)

/**
 * Transparent live trust layer (F11): thin skeleton lines over the camera stage, plus an
 * optional angle readout for the joint currently being assessed (Warrior II's front knee).
 * [frame]'s landmark coordinates are normalized (0..1, MediaPipe image space) and already
 * mirrored to match the front-camera preview (see [com.mira.miraai.perception.MediaPipePoseEstimator]),
 * so they map directly onto this composable's bounds with no extra flip.
 */
/**
 * Skeleton color when form is currently correct — the "wow" signal per 2026-08-29 user feedback.
 * Pushed toward a more saturated, higher-hue neon (was 0xFF4CFFB0) so it reads as an electric
 * glow against the camera feed rather than a flat mint green.
 */
private val GOOD_FORM_GREEN = androidx.compose.ui.graphics.Color(0xFF39FF8F)

@Composable
fun PoseOverlay(
    frame: PoseFrame?,
    highlightJoint: BodyJoint? = null,
    angleDeg: Float? = null,
    isGoodForm: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (frame == null) return

    val skeletonColor = if (isGoodForm) GOOD_FORM_GREEN else MiraColors.accent

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            SKELETON_CONNECTIONS.forEach { (a, b) ->
                val landmarkA = frame.landmark(a)
                val landmarkB = frame.landmark(b)
                if (landmarkA != null && landmarkB != null &&
                    landmarkA.visibility >= WarriorIIThresholds.MIN_LANDMARK_VISIBILITY &&
                    landmarkB.visibility >= WarriorIIThresholds.MIN_LANDMARK_VISIBILITY
                ) {
                    drawLine(
                        color = skeletonColor,
                        start = toOffset(landmarkA.position.x, landmarkA.position.y),
                        end = toOffset(landmarkB.position.x, landmarkB.position.y),
                        strokeWidth = (if (isGoodForm) 4.dp else 3.dp).toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }

            frame.landmarks.forEach { (joint, landmark) ->
                if (landmark.visibility >= WarriorIIThresholds.MIN_LANDMARK_VISIBILITY) {
                    val isHighlighted = joint == highlightJoint
                    drawCircle(
                        color = if (isHighlighted) MiraColors.tertiaryContainer else skeletonColor,
                        radius = (if (isHighlighted) 7.dp else 4.dp).toPx(),
                        center = toOffset(landmark.position.x, landmark.position.y),
                    )
                }
            }
        }

        if (highlightJoint != null && angleDeg != null) {
            val landmark = frame.landmark(highlightJoint)
            if (landmark != null) {
                Text(
                    text = "${angleDeg.toInt()}°",
                    color = MiraColors.tertiaryContainer,
                    fontSize = 16.sp,
                    modifier = Modifier.offset(
                        x = maxWidth * landmark.position.x + 12.dp,
                        y = maxHeight * landmark.position.y - 10.dp,
                    ),
                )
            }
        }
    }
}

private fun DrawScope.toOffset(normalizedX: Float, normalizedY: Float): Offset =
    Offset(normalizedX * size.width, normalizedY * size.height)
