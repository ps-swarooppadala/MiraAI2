package com.mira.miraai.ui.framing

import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mira.miraai.agent.WorkoutThresholds
import com.mira.miraai.perception.PoseFrame
import com.mira.miraai.ui.components.PoseOverlay
import com.mira.miraai.ui.theme.MiraColors
import com.mira.miraai.ui.theme.MiraSpacing
import com.mira.miraai.ui.theme.MiraType

/**
 * Framing Assistant — US-5 (feature-spec.md), the confidence-gated pre-coaching gate, matching
 * the "Fix your framing" variant in docs/ux/live_coaching_framing_guide. Purely presentational:
 * whether/when [confidence] crosses the sustained-good threshold and triggers auto-advance is
 * [com.mira.miraai.agent.FramingGate]'s job (pure Kotlin, unit-tested) — this screen just
 * reflects whatever confidence value the caller is currently feeding it.
 */
@Composable
fun FramingAssistantScreen(
    hasPermission: Boolean,
    confidence: Float,
    poseFrame: PoseFrame? = null,
    onGrantPermission: () -> Unit,
    onPreviewViewReady: (PreviewView) -> Unit,
    onEndSession: () -> Unit,
) {
    val isGoodFraming = confidence >= WorkoutThresholds.FRAMING_CONFIDENCE_THRESHOLD

    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    Box(modifier = Modifier.fillMaxSize().background(MiraColors.primary)) {
        if (hasPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx -> PreviewView(ctx).also { onPreviewViewReady(it) } },
            )
            PoseOverlay(frame = poseFrame, modifier = Modifier.fillMaxSize())
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (isGoodFraming) 0.25f else 0.55f)),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(MiraSpacing.containerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.padding(top = MiraSpacing.stackLg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (isGoodFraming) "Perfect — hold still." else "Take a step back.\nLet's get your whole body in view.",
                    style = MiraType.headlineLgMobile,
                    color = MiraColors.surfaceContainerLowest,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(MiraSpacing.stackSm))
                Text(
                    text = if (isGoodFraming) "Starting in a moment..." else "I need to see your head and feet to guide your form correctly.",
                    style = MiraType.bodyLg,
                    color = MiraColors.primaryFixedDim,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.weight(1f))

            FramingGuide(isGoodFraming = isGoodFraming)

            Spacer(Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MiraSpacing.stackMd),
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MiraColors.onSurface.copy(alpha = 0.8f))
                        .padding(horizontal = MiraSpacing.stackMd, vertical = MiraSpacing.stackSm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MiraSpacing.unit),
                ) {
                    Icon(Icons.Filled.Radar, contentDescription = null, tint = MiraColors.tertiaryContainer)
                    Text(
                        text = if (isGoodFraming) "Looking good" else "Looking for you...",
                        style = MiraType.labelMd,
                        color = MiraColors.surfaceContainerLowest,
                    )
                }

                if (!hasPermission) {
                    Button(onClick = onGrantPermission) { Text("Grant camera permission") }
                }

                Text(
                    text = "End Session",
                    style = MiraType.labelMd,
                    color = MiraColors.primaryFixedDim,
                    modifier = Modifier.clickable(onClick = onEndSession).padding(MiraSpacing.unit),
                )
            }
        }
    }
}

@Composable
private fun FramingGuide(isGoodFraming: Boolean) {
    Box(
        modifier = Modifier
            .width(220.dp)
            .height(320.dp)
            .border(
                width = 3.dp,
                color = if (isGoodFraming) MiraColors.tertiaryContainer else MiraColors.primaryFixed,
                shape = RoundedCornerShape(50),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .border(2.dp, MiraColors.primaryFixed, CircleShape),
            )
            Spacer(Modifier.height(MiraSpacing.unit))
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(160.dp)
                    .border(2.dp, MiraColors.primaryFixed, RoundedCornerShape(16.dp)),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FramingAssistantScreenPreview() {
    FramingAssistantScreen(
        hasPermission = false,
        confidence = 0.4f,
        onGrantPermission = {},
        onPreviewViewReady = {},
        onEndSession = {},
    )
}
