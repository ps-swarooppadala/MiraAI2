package com.mira.miraai.ui.setup

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mira.miraai.ui.theme.MiraColors
import com.mira.miraai.ui.theme.MiraSpacing
import com.mira.miraai.ui.theme.MiraType

private data class SetupTip(val icon: ImageVector, val title: String, val description: String)

private val setupTips = listOf(
    SetupTip(Icons.Filled.OpenWith, "Clear a 6ft space", "Ensure you have room to move freely."),
    SetupTip(Icons.Filled.LightMode, "Natural lighting", "Face a light source so I can see you clearly."),
    SetupTip(Icons.Filled.StayCurrentPortrait, "Device placement", "Prop your phone at waist height, about 5ft away."),
)

/**
 * Setup Tips — US-4 (feature-spec.md), matching docs/ux/setup_preparing_your_space.
 * Built now (Phase 5) even though it isn't reached in navigation until Phase 6 wires
 * Language → Setup → Framing → Player, per this phase's explicit instruction to make it a real
 * screen rather than a placeholder nav target. Shown once per feature-spec.md US-4 note;
 * "shown once, skippable thereafter via local flag" — the local flag/persistence itself is
 * Phase 6+ wiring (WorkoutSessionState / navigation), not this screen's concern.
 */
@Composable
fun SetupTipsScreen(
    onBackClick: () -> Unit,
    onReadyClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MiraColors.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MiraSpacing.containerPadding),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Go back",
                    tint = MiraColors.primary,
                    modifier = Modifier.clickable(onClick = onBackClick),
                )
                Text(text = "Mira.ai", style = MiraType.headlineMd, color = MiraColors.primary)
                Spacer(Modifier.size(24.dp))
            }
            Text(
                text = "Prepare your space",
                style = MiraType.headlineLgMobile,
                color = MiraColors.primary,
                modifier = Modifier.fillMaxWidth().padding(top = MiraSpacing.stackMd),
            )

            Spacer(Modifier.height(MiraSpacing.stackLg))

            Column(verticalArrangement = Arrangement.spacedBy(MiraSpacing.stackMd)) {
                setupTips.forEach { tip -> SetupTipRow(tip) }
            }

            Spacer(Modifier.height(MiraSpacing.stackLg))

            Button(
                onClick = onReadyClick,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = MiraColors.softGold, contentColor = MiraColors.primary),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "I'm ready", style = MiraType.labelMd)
                Spacer(Modifier.width(MiraSpacing.unit))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun SetupTipRow(tip: SetupTip) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(MiraSpacing.gutter)) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MiraColors.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(tip.icon, contentDescription = null, tint = MiraColors.primary)
        }
        Column {
            Text(text = tip.title, style = MiraType.labelMd, color = MiraColors.primary)
            Text(text = tip.description, style = MiraType.bodyMd, color = MiraColors.onSurfaceVariant)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SetupTipsScreenPreview() {
    SetupTipsScreen(onBackClick = {}, onReadyClick = {})
}
