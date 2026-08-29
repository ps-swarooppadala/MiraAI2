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
    BodyJoint.LEFT_SHOULDER to BodyJoint.RIGHT_SHOULDER,
    BodyJoint.LEFT_SHOULDER to BodyJoint.LEFT_HIP,
    BodyJoint.RIGHT_SHOULDER to BodyJoint.RIGHT_HIP,
    BodyJoint.LEFT_HIP to BodyJoint.RIGHT_HIP,
    BodyJoint.LEFT_HIP to BodyJoint.LEFT_KNEE,
    BodyJoint.LEFT_KNEE to BodyJoint.LEFT_ANKLE,
    BodyJoint.RIGHT_HIP to BodyJoint.RIGHT_KNEE,
    BodyJoint.RIGHT_KNEE to BodyJoint.RIGHT_ANKLE,
    BodyJoint.LEFT_SHOULDER to BodyJoint.LEFT_WRIST,
    BodyJoint.RIGHT_SHOULDER to BodyJoint.RIGHT_WRIST,
)

/**
 * Transparent live trust layer (F11): thin skeleton lines over the camera stage, plus an
 * optional angle readout for the joint currently being assessed (Warrior II's front knee).
 * [frame]'s landmark coordinates are normalized (0..1, MediaPipe image space) and already
 * mirrored to match the front-camera preview (see [com.mira.miraai.perception.MediaPipePoseEstimator]),
 * so they map directly onto this composable's bounds with no extra flip.
 */
@Composable
fun PoseOverlay(
    frame: PoseFrame?,
    highlightJoint: BodyJoint? = null,
    angleDeg: Float? = null,
    modifier: Modifier = Modifier,
) {
    if (frame == null) return

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
                        color = MiraColors.accent,
                        start = toOffset(landmarkA.position.x, landmarkA.position.y),
                        end = toOffset(landmarkB.position.x, landmarkB.position.y),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }

            frame.landmarks.forEach { (joint, landmark) ->
                if (landmark.visibility >= WarriorIIThresholds.MIN_LANDMARK_VISIBILITY) {
                    val isHighlighted = joint == highlightJoint
                    drawCircle(
                        color = if (isHighlighted) MiraColors.tertiaryContainer else MiraColors.accent,
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
