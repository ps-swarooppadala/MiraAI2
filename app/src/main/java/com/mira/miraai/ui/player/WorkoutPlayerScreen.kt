package com.mira.miraai.ui.player

import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.Canvas
import com.mira.miraai.agent.FramingReframeState
import com.mira.miraai.assessor.VerdictCode
import com.mira.miraai.perception.BodyJoint
import com.mira.miraai.perception.PoseFrame
import com.mira.miraai.perception.Side
import com.mira.miraai.ui.components.PoseOverlay
import com.mira.miraai.ui.components.PoseReferenceDiagram
import com.mira.miraai.ui.components.ReframedCameraStage
import com.mira.miraai.ui.theme.MiraColors
import com.mira.miraai.ui.theme.MiraSpacing
import com.mira.miraai.ui.theme.MiraType

/** How long the golden celebration glow takes to fade in/out — slow on purpose (2026-08-29
 *  feedback: losing the pose shouldn't snap the glow away abruptly). */
private const val GOLD_GLOW_FADE_MS = 3000

/**
 * Workout Mode Player — US-6 / feature-spec.md Section 8.1's layout, top to bottom: top bar
 * (step progress, title, pause), camera stage with confidence dot, hold-progress ring, cue
 * caption bar, bottom pause/end controls. A non-blocking confidence-recovery banner (Section
 * 8.4) overlays the stage when tracking confidence drops mid-workout.
 *
 * Purely presentational — [com.mira.miraai.agent.WorkoutSessionEngine] and [com.mira.miraai.agent.CoachAgent]
 * own all the state this screen renders; the skeleton/angle overlay named in Section 8.1 item 2
 * is not drawn here (no landmark stream is plumbed to the UI layer yet) — flagged in
 * docs/PROGRESS.md as a known gap, not silently dropped.
 */
@Composable
fun WorkoutPlayerScreen(
    hasPermission: Boolean,
    routineTitle: String,
    stepNumber: Int,
    totalSteps: Int,
    poseId: String,
    poseDisplayName: String,
    sideLabel: String?,
    targetHoldSec: Int,
    elapsedHoldSec: Int,
    confidenceScore: Float,
    isPaused: Boolean,
    isResting: Boolean,
    restLabel: String?,
    cueCaption: String?,
    showConfidenceRecoveryBanner: Boolean,
    poseFrame: PoseFrame? = null,
    highlightJoint: BodyJoint? = null,
    currentAngleDeg: Float? = null,
    verdictCode: VerdictCode? = null,
    poseSide: Side? = null,
    reframe: FramingReframeState = FramingReframeState(),
    onGrantPermission: () -> Unit,
    onPreviewViewReady: (PreviewView) -> Unit,
    onPauseToggle: () -> Unit,
    onEndWorkoutClick: () -> Unit,
) {
    // Keep the screen from dimming/locking mid-hold — a paused-on-black-screen phone is a
    // broken workout, and the user has no free hand to keep tapping it awake.
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val isGoodForm = verdictCode == VerdictCode.GOOD_FORM

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasPermission) {
            ReframedCameraStage(reframe = reframe, modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx -> PreviewView(ctx).also { onPreviewViewReady(it) } },
                )
                PoseOverlay(
                    frame = poseFrame,
                    highlightJoint = highlightJoint,
                    angleDeg = currentAngleDeg,
                    verdictCode = verdictCode,
                    side = poseSide,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (poseDisplayName.isNotBlank()) {
                PoseReferenceDiagram(
                    poseId = poseId,
                    poseDisplayName = poseDisplayName,
                    modifier = Modifier
                        .padding(top = 72.dp, end = MiraSpacing.containerPadding)
                        .align(Alignment.TopEnd),
                )
            }
            GoldenFormGlow(visible = isGoodForm && !isResting && !isPaused, modifier = Modifier.fillMaxSize())
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(MiraSpacing.containerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Mira needs camera access to coach you.", color = Color.White)
                Spacer(Modifier.height(MiraSpacing.stackSm))
                Button(onClick = onGrantPermission) { Text("Grant camera permission") }
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(routineTitle, stepNumber, totalSteps, isPaused, onPauseToggle, confidenceScore)

            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (isResting) {
                    RestOverlay(restLabel)
                } else {
                    HoldRing(elapsedHoldSec = elapsedHoldSec, targetHoldSec = targetHoldSec, poseDisplayName = poseDisplayName, sideLabel = sideLabel)
                }
                if (showConfidenceRecoveryBanner) {
                    ConfidenceRecoveryBanner(modifier = Modifier.align(Alignment.TopCenter).padding(top = MiraSpacing.stackMd))
                }
            }

            CueCaptionBar(cueCaption)

            BottomControls(isPaused = isPaused, onPauseToggle = onPauseToggle, onEndWorkoutClick = onEndWorkoutClick)
        }
    }
}

@Composable
private fun TopBar(
    routineTitle: String,
    stepNumber: Int,
    totalSteps: Int,
    isPaused: Boolean,
    onPauseToggle: () -> Unit,
    confidenceScore: Float,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(MiraSpacing.containerPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(text = routineTitle, style = MiraType.labelSm, color = MiraColors.surfaceContainerLowest.copy(alpha = 0.7f))
            Text(text = "Step $stepNumber of $totalSteps", style = MiraType.labelMd, color = Color.White)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MiraSpacing.gutter)) {
            ConfidenceDot(confidenceScore)
            IconButton(onClick = onPauseToggle) {
                Icon(
                    imageVector = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    contentDescription = if (isPaused) "Resume" else "Pause",
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun ConfidenceDot(confidenceScore: Float) {
    val color = when {
        confidenceScore >= 0.75f -> MiraColors.tertiaryContainer
        confidenceScore >= 0.5f -> MiraColors.softGold
        else -> MiraColors.error
    }
    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
}

@Composable
private fun HoldRing(elapsedHoldSec: Int, targetHoldSec: Int, poseDisplayName: String, sideLabel: String?) {
    val progress = if (targetHoldSec <= 0) 0f else (elapsedHoldSec.toFloat() / targetHoldSec).coerceIn(0f, 1f)
    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(200.dp)) {
            drawArc(
                color = Color.White.copy(alpha = 0.2f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 10.dp.toPx()),
                size = Size(size.width, size.height),
            )
            drawArc(
                color = Color(0xFFE6BE8A),
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = 10.dp.toPx()),
                size = Size(size.width, size.height),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "${elapsedHoldSec}s / ${targetHoldSec}s", style = MiraType.headlineMd, color = Color.White)
            Text(text = poseDisplayName + (sideLabel?.let { " · $it" } ?: ""), style = MiraType.labelMd, color = Color.White.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun RestOverlay(restLabel: String?) {
    Text(
        text = restLabel ?: "Rest",
        style = MiraType.headlineLgMobile,
        color = Color.White,
        textAlign = TextAlign.Center,
    )
}

/**
 * Full-screen "you've got it" celebration when form is correct — a pulsing gold glow around the
 * entire camera stage (including its corners), not just a subtle wash. Presence fades in/out over
 * [GOLD_GLOW_FADE_MS] rather than snapping, so losing the pose reads as a graceful fade instead of
 * an abrupt cut (2026-08-29 feedback) — gold-only per the same feedback, replacing the earlier
 * green/gold hue-drifting version.
 */
@Composable
private fun GoldenFormGlow(visible: Boolean, modifier: Modifier = Modifier) {
    val presence by animateFloatAsState(targetValue = if (visible) 1f else 0f, animationSpec = tween(GOLD_GLOW_FADE_MS), label = "goldenGlowPresence")
    if (presence <= 0f) return

    val infiniteTransition = rememberInfiniteTransition(label = "goldenGlowPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(900, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "goldenGlowPulseValue",
    )

    val goldA = Color(0xFFFFD966)
    val goldB = Color(0xFFFFB84D)
    val gold = lerp(goldA, goldB, pulse)
    val alpha = presence * pulse

    Canvas(modifier = modifier) {
        // Layered edge glow — a soft radial wash plus a thicker gradient ring right at the
        // border (and therefore the corners), so it reads as a glowing outline around the whole
        // frame, not just a tint.
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, gold.copy(alpha = 0.10f * alpha), gold.copy(alpha = 0.20f * alpha)),
                center = center,
                radius = size.maxDimension * 0.75f,
            ),
        )
        drawRect(
            color = gold.copy(alpha = 0.9f * alpha),
            style = Stroke(width = (10.dp.toPx()) * (0.6f + 0.4f * pulse)),
        )
    }
}

@Composable
private fun ConfidenceRecoveryBanner(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MiraColors.softGold)
            .padding(horizontal = MiraSpacing.stackMd, vertical = MiraSpacing.stackSm),
    ) {
        Text(text = "I can't see you clearly — step back into frame.", style = MiraType.labelMd, color = MiraColors.primary)
    }
}

@Composable
private fun CueCaptionBar(cueCaption: String?) {
    if (cueCaption.isNullOrBlank()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MiraSpacing.containerPadding, vertical = MiraSpacing.stackSm),
    ) {
        Text(
            text = cueCaption,
            style = MiraType.bodyMd,
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(MiraSpacing.stackSm),
        )
    }
}

@Composable
private fun BottomControls(isPaused: Boolean, onPauseToggle: () -> Unit, onEndWorkoutClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(MiraSpacing.containerPadding),
        horizontalArrangement = Arrangement.spacedBy(MiraSpacing.gutter),
    ) {
        Button(
            onClick = onPauseToggle,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = MiraColors.softGold, contentColor = MiraColors.primary),
            modifier = Modifier.weight(1f),
        ) {
            Text(if (isPaused) "Resume" else "Pause")
        }
        Button(
            onClick = onEndWorkoutClick,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f), contentColor = Color.White),
        ) {
            Text("End Workout")
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun WorkoutPlayerScreenPreview() {
    WorkoutPlayerScreen(
        hasPermission = false,
        routineTitle = "Foundations",
        stepNumber = 1,
        totalSteps = 4,
        poseId = "warrior_ii",
        poseDisplayName = "Warrior II",
        sideLabel = "Left side",
        targetHoldSec = 20,
        elapsedHoldSec = 8,
        confidenceScore = 0.9f,
        isPaused = false,
        isResting = false,
        restLabel = null,
        cueCaption = "Bend your front knee a bit more.",
        showConfidenceRecoveryBanner = false,
        onGrantPermission = {},
        onPreviewViewReady = {},
        onPauseToggle = {},
        onEndWorkoutClick = {},
    )
}
