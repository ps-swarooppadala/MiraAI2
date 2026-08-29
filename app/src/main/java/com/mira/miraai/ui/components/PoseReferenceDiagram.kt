package com.mira.miraai.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mira.miraai.ui.theme.MiraColors
import com.mira.miraai.ui.theme.MiraType

/**
 * Static target-pose joint positions for Warrior II, normalized (0..1) in the same x/y space
 * [com.mira.miraai.ui.components.PoseOverlay] draws live landmarks in — front leg bent to
 * [com.mira.miraai.assessor.WarriorIIThresholds.FRONT_KNEE_TARGET_DEG], back leg straight, arms
 * level, torso upright, matching the front-leg-left textbook fixture the Assessor is tested
 * against. This is a hand-authored reference stick figure, not derived from a photo/illustration
 * asset (none exist yet — see docs/PROGRESS.md's placeholder-art notes from Phase 5).
 */
private val WARRIOR_II_LEFT_FRONT_JOINTS: Map<String, Offset> = mapOf(
    "leftShoulder" to Offset(0.40f, 0.30f),
    "rightShoulder" to Offset(0.60f, 0.30f),
    "leftWrist" to Offset(0.18f, 0.30f),
    "rightWrist" to Offset(0.82f, 0.30f),
    "leftHip" to Offset(0.46f, 0.55f),
    "rightHip" to Offset(0.58f, 0.55f),
    "leftKnee" to Offset(0.34f, 0.72f),
    "leftAnkle" to Offset(0.30f, 0.92f),
    "rightKnee" to Offset(0.74f, 0.80f),
    "rightAnkle" to Offset(0.86f, 0.92f),
)

private val REFERENCE_CONNECTIONS = listOf(
    "leftShoulder" to "rightShoulder",
    "leftShoulder" to "leftHip",
    "rightShoulder" to "rightHip",
    "leftHip" to "rightHip",
    "leftHip" to "leftKnee",
    "leftKnee" to "leftAnkle",
    "rightHip" to "rightKnee",
    "rightKnee" to "rightAnkle",
    "leftShoulder" to "leftWrist",
    "rightShoulder" to "rightWrist",
)

/**
 * Small "what to do" reference card — a static stick-figure diagram of the target pose, shown
 * alongside the live camera skeleton so a first-time user can see the shape they're aiming for
 * without guessing from the coaching cues alone.
 */
@Composable
fun PoseReferenceDiagram(
    poseDisplayName: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(8.dp),
    ) {
        Canvas(modifier = Modifier.size(84.dp)) {
            REFERENCE_CONNECTIONS.forEach { (a, b) ->
                val pointA = WARRIOR_II_LEFT_FRONT_JOINTS.getValue(a)
                val pointB = WARRIOR_II_LEFT_FRONT_JOINTS.getValue(b)
                drawLine(
                    color = MiraColors.accent,
                    start = Offset(pointA.x * size.width, pointA.y * size.height),
                    end = Offset(pointB.x * size.width, pointB.y * size.height),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            WARRIOR_II_LEFT_FRONT_JOINTS.values.forEach { point ->
                drawCircle(
                    color = MiraColors.accent,
                    radius = 3.dp.toPx(),
                    center = Offset(point.x * size.width, point.y * size.height),
                )
            }
        }
        Text(
            text = "Target: $poseDisplayName",
            style = MiraType.labelSm,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
