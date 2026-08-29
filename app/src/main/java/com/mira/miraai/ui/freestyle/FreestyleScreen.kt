package com.mira.miraai.ui.freestyle

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mira.miraai.ui.theme.MiraColors
import com.mira.miraai.ui.theme.MiraSpacing
import com.mira.miraai.ui.theme.MiraType

/**
 * US-8 Freestyle Conversation orb/caption screen (feature-spec.md Section 5).
 *
 * **Doc gap, flagged not silently guessed:** the task instructions point at
 * `docs/ux/freestyle.md`, but no such file exists in `docs/ux/` (same gap Phase 5 flagged for
 * Routine Detail) — only named Stitch export folders for other screens plus `DESIGN.md`. Built
 * against `DESIGN.md`'s "Coach Speaking Indicator" component spec instead: "a subtle, breathing
 * pulse animation using the Soft Gold color... a single glowing ring." Whoever runs a docs pass
 * next should add a real `freestyle.md` export or fold this note into feature-spec.md.
 *
 * Per US-8's acceptance criteria: "show only the orb + live caption... no buttons except a
 * small mic-mute affordance" — this screen deliberately has no other chrome besides a back
 * affordance (needed to actually leave the screen, not spec-forbidden — every other screen in
 * this app has one) and the mic-mute toggle.
 */
enum class OrbState { GREETING, LISTENING, THINKING, SPEAKING }

@Composable
fun FreestyleScreen(
    captionText: String,
    orbState: OrbState,
    isMicMuted: Boolean,
    onMicMuteToggle: () -> Unit,
    onBackClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MiraColors.primary),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MiraSpacing.containerPadding),
            horizontalArrangement = Arrangement.Start,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Go back",
                tint = MiraColors.background,
                modifier = Modifier.clickable(onClick = onBackClick),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(MiraSpacing.containerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            FreestyleOrb(orbState = orbState)

            Spacer(Modifier.height(MiraSpacing.stackLg))

            Text(
                text = captionText,
                style = MiraType.headlineMd,
                color = MiraColors.background,
                modifier = Modifier.padding(horizontal = MiraSpacing.stackMd),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = MiraSpacing.stackLg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
        ) {
            MicMuteButton(isMicMuted = isMicMuted, onClick = onMicMuteToggle)
        }
    }
}

@Composable
private fun FreestyleOrb(orbState: OrbState) {
    val transition = rememberInfiniteTransition(label = "orbPulse")
    val periodMs = when (orbState) {
        OrbState.THINKING -> 700
        OrbState.SPEAKING -> 900
        OrbState.LISTENING -> 1400
        OrbState.GREETING -> 1600
    }
    val scale by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = periodMs, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "orbScale",
    )

    Canvas(modifier = Modifier.size(180.dp)) {
        val radius = (size.minDimension / 2f) * scale
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(MiraColors.softGold, MiraColors.softGold.copy(alpha = 0f)),
                center = center,
                radius = radius,
            ),
            radius = radius,
            center = center,
        )
        drawCircle(
            color = MiraColors.softGold,
            radius = radius * 0.55f,
            center = Offset(center.x, center.y),
        )
    }
}

@Composable
private fun MicMuteButton(isMicMuted: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(if (isMicMuted) MiraColors.error.copy(alpha = 0.15f) else MiraColors.primaryContainer)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isMicMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
            contentDescription = if (isMicMuted) "Unmute microphone" else "Mute microphone",
            tint = if (isMicMuted) MiraColors.error else Color(0xFF7AA694),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FreestyleScreenPreview() {
    FreestyleScreen(
        captionText = "Good evening — ready to unwind? Want a quick warm-up before we move into a full routine?",
        orbState = OrbState.SPEAKING,
        isMicMuted = false,
        onMicMuteToggle = {},
        onBackClick = {},
    )
}
