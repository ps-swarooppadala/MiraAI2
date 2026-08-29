package com.mira.miraai.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mira.miraai.assessor.VerdictCode
import com.mira.miraai.assessor.WarriorIIThresholds
import com.mira.miraai.perception.BodyJoint
import com.mira.miraai.perception.PoseFrame
import com.mira.miraai.perception.Side
import com.mira.miraai.ui.theme.MiraColors

/** Skeleton line segments drawn over the camera preview — feature-spec.md Section 8.1 item 2 / F11. */
private val SKELETON_CONNECTIONS = listOf(
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

/** How long a color/presence change takes to settle — deliberately slow so losing the pose or
 *  fixing an issue reads as a graceful fade, not a jarring snap (2026-08-29 feedback). */
private const val COLOR_FADE_MS = 3000

/** No pose has been assessed yet (or tracking just dropped) — the "waiting" state. */
private val NEUTRAL_COLOR = Color(0xFF9AA0A6)
/** A tracked joint that isn't implicated in the current issue. */
private val CORRECT_COLOR = Color(0xFF39FF8F)
/** A joint at fault for the current verdict. */
private val INCORRECT_COLOR = Color(0xFFFF9C4A)
/** Every joint, once the whole pose reads as correct — pulsates rather than sitting static. */
private val GOLD_COLOR = Color(0xFFFFD966)

@Composable
fun PoseOverlay(
    frame: PoseFrame?,
    highlightJoint: BodyJoint? = null,
    angleDeg: Float? = null,
    verdictCode: VerdictCode? = null,
    side: Side? = null,
    modifier: Modifier = Modifier,
) {
    // Keep drawing the last-seen pose while it fades out, instead of snapping to nothing the
    // instant tracking drops a frame (2026-08-29 feedback: losing the pose shouldn't be abrupt).
    var lastFrame by remember { mutableStateOf<PoseFrame?>(null) }
    LaunchedEffect(frame) { if (frame != null) lastFrame = frame }
    val effectiveFrame = frame ?: lastFrame ?: return

    val presence by animateFloatAsState(
        targetValue = if (frame != null) 1f else 0f,
        animationSpec = tween(COLOR_FADE_MS),
        label = "poseOverlayPresence",
    )
    if (presence <= 0f) return

    val isGoodForm = verdictCode == VerdictCode.GOOD_FORM
    val issueJoints = remember(verdictCode, side) {
        verdictCode?.let { PoseIssueJoints.forVerdict(it, side) } ?: emptySet()
    }

    val pulse by rememberGoldPulse(isGoodForm)

    fun targetColorFor(joint: BodyJoint): Color = when {
        verdictCode == null -> NEUTRAL_COLOR
        isGoodForm -> GOLD_COLOR
        joint in issueJoints -> INCORRECT_COLOR
        else -> CORRECT_COLOR
    }

    // One animated color per tracked joint — BodyJoint.entries is a fixed-size, order-stable
    // list, so calling a composable per entry here is safe (same call count every recomposition).
    val jointColors: Map<BodyJoint, Color> = BodyJoint.entries.associateWith { joint ->
        val animated by animateColorAsState(targetColorFor(joint), tween(COLOR_FADE_MS), label = joint.name)
        animated
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            SKELETON_CONNECTIONS.forEach { (a, b) ->
                val landmarkA = effectiveFrame.landmark(a)
                val landmarkB = effectiveFrame.landmark(b)
                if (landmarkA != null && landmarkB != null &&
                    landmarkA.visibility >= WarriorIIThresholds.MIN_LANDMARK_VISIBILITY &&
                    landmarkB.visibility >= WarriorIIThresholds.MIN_LANDMARK_VISIBILITY
                ) {
                    // A line between two joints of different colors blends between them, so a
                    // limb reads as transitioning smoothly rather than having a hard color seam.
                    val lineColor = lerpColor(jointColors.getValue(a), jointColors.getValue(b), 0.5f)
                    val glow = if (isGoodForm) pulse else 1f
                    drawLine(
                        color = lineColor.copy(alpha = lineColor.alpha * presence * glow),
                        start = toOffset(landmarkA.position.x, landmarkA.position.y),
                        end = toOffset(landmarkB.position.x, landmarkB.position.y),
                        strokeWidth = (if (isGoodForm) 4.dp else 3.dp).toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }

            effectiveFrame.landmarks.forEach { (joint, landmark) ->
                if (landmark.visibility >= WarriorIIThresholds.MIN_LANDMARK_VISIBILITY) {
                    val isHighlighted = joint == highlightJoint
                    val glow = if (isGoodForm) pulse else 1f
                    val baseColor = if (isHighlighted) MiraColors.tertiaryContainer else jointColors.getValue(joint)
                    drawCircle(
                        color = baseColor.copy(alpha = baseColor.alpha * presence * glow),
                        radius = (if (isHighlighted) 7.dp else 4.dp).toPx(),
                        center = toOffset(landmark.position.x, landmark.position.y),
                    )
                }
            }
        }

        if (highlightJoint != null && angleDeg != null) {
            val landmark = effectiveFrame.landmark(highlightJoint)
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

/** Pulsating brightness multiplier (0.6..1) applied to every joint/line while form is good — the
 *  same glow language as [com.mira.miraai.ui.player.WorkoutPlayerScreen]'s screen-edge glow. */
@Composable
private fun rememberGoldPulse(active: Boolean): androidx.compose.runtime.State<Float> {
    val infiniteTransition = rememberInfiniteTransition(label = "jointGoldPulse")
    val pulse = infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(900, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "jointGoldPulseValue",
    )
    return if (active) pulse else remember { mutableStateOf(1f) }
}

private fun lerpColor(a: Color, b: Color, fraction: Float): Color = Color(
    red = a.red + (b.red - a.red) * fraction,
    green = a.green + (b.green - a.green) * fraction,
    blue = a.blue + (b.blue - a.blue) * fraction,
    alpha = a.alpha + (b.alpha - a.alpha) * fraction,
)

private fun DrawScope.toOffset(normalizedX: Float, normalizedY: Float): Offset =
    Offset(normalizedX * size.width, normalizedY * size.height)
