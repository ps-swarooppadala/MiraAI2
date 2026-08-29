package com.mira.miraai.ui.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mira.miraai.agent.StepResult
import com.mira.miraai.assessor.VerdictCode
import com.mira.miraai.content.Pose
import com.mira.miraai.ui.theme.MiraColors
import com.mira.miraai.ui.theme.MiraRadius
import com.mira.miraai.ui.theme.MiraSpacing
import com.mira.miraai.ui.theme.MiraType

/**
 * Session Summary — US-7 (feature-spec.md), matching docs/ux/session_summary_a_gentle_close's
 * stat-row + closing-line layout. "See what I've learned about you" is wired as a no-op/disabled
 * CTA per this phase's explicit instruction — the Memory Graph screen it targets doesn't exist
 * until Phase 10 (build-architecture.md Section 7).
 */
@Composable
fun SessionSummaryScreen(
    routineTitle: String,
    totalElapsedMs: Long,
    stepResults: List<StepResult>,
    posesById: Map<String, Pose>,
    nextFocusVerdict: VerdictCode?,
    onDoneClick: () -> Unit,
    onSeeWhatMiraLearnedClick: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MiraColors.background)
            .verticalScroll(rememberScrollState())
            .padding(MiraSpacing.containerPadding),
    ) {
        Text(text = "Mira.ai", style = MiraType.headlineMd, color = MiraColors.primary)
        Spacer(Modifier.height(MiraSpacing.stackLg))

        Text(text = "Session complete", style = MiraType.headlineLgMobile, color = MiraColors.primary)
        Text(
            text = routineTitle,
            style = MiraType.bodyMd,
            color = MiraColors.onSurfaceVariant,
            modifier = Modifier.padding(top = MiraSpacing.unit),
        )

        Spacer(Modifier.height(MiraSpacing.stackLg))

        Row(horizontalArrangement = Arrangement.spacedBy(MiraSpacing.gutter)) {
            StatCard(label = "Poses Practiced", value = stepResults.size.toString(), modifier = Modifier.weight(1f))
            StatCard(label = "Total Hold Time", value = formatDuration(totalElapsedMs), modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(MiraSpacing.stackLg))

        Text(text = "Per-pose results", style = MiraType.bodyLg, color = MiraColors.primaryContainer)
        Spacer(Modifier.height(MiraSpacing.stackSm))
        Column(verticalArrangement = Arrangement.spacedBy(MiraSpacing.stackSm)) {
            stepResults.forEach { result -> StepResultRow(result, posesById[result.poseId]) }
        }

        Spacer(Modifier.height(MiraSpacing.stackLg))

        Text(
            text = "Next time, focus on: " + nextFocusLine(nextFocusVerdict),
            style = MiraType.bodyLg,
            color = MiraColors.primaryContainer,
        )

        Spacer(Modifier.height(MiraSpacing.stackLg))

        Button(
            onClick = onDoneClick,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = MiraColors.primary, contentColor = MiraColors.surfaceContainerLowest),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "Done", style = MiraType.labelMd)
        }

        Spacer(Modifier.height(MiraSpacing.stackSm))

        Text(
            text = "See what I've learned about you",
            style = MiraType.labelMd,
            color = if (onSeeWhatMiraLearnedClick != null) MiraColors.tertiary else MiraColors.outlineVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(MiraSpacing.stackSm)
                .clickable(enabled = onSeeWhatMiraLearnedClick != null) { onSeeWhatMiraLearnedClick?.invoke() },
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(MiraRadius.card))
            .background(MiraColors.surfaceContainerLowest)
            .padding(MiraSpacing.stackMd),
    ) {
        Text(text = label, style = MiraType.labelSm, color = MiraColors.onSurfaceVariant)
        Spacer(Modifier.height(MiraSpacing.unit))
        Text(text = value, style = MiraType.headlineMd, color = MiraColors.primary)
    }
}

@Composable
private fun StepResultRow(result: StepResult, pose: Pose?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MiraRadius.card))
            .background(MiraColors.surfaceContainerLowest)
            .padding(MiraSpacing.stackMd),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MiraSpacing.gutter),
    ) {
        Icon(
            imageVector = if (result.completed) Icons.Filled.CheckCircle else Icons.Filled.Circle,
            contentDescription = null,
            tint = if (result.completed) MiraColors.tertiaryContainer else MiraColors.outlineVariant,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = (pose?.displayName ?: result.poseId) + (result.side?.let { " · $it" } ?: ""),
                style = MiraType.labelMd,
                color = MiraColors.primary,
            )
            Text(
                text = if (result.mostFrequentVerdict == VerdictCode.GOOD_FORM) "Good form" else nextFocusLine(result.mostFrequentVerdict),
                style = MiraType.bodyMd,
                color = MiraColors.onSurfaceVariant,
            )
        }
    }
}

private fun formatDuration(totalElapsedMs: Long): String {
    val totalSeconds = totalElapsedMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes}m ${seconds}s"
}

private fun nextFocusLine(verdict: VerdictCode?): String = when (verdict) {
    null, VerdictCode.GOOD_FORM -> "keep it up — form was solid throughout."
    VerdictCode.FRONT_KNEE_TOO_STRAIGHT -> "bending your front knee more deeply."
    VerdictCode.FRONT_KNEE_OVER_BENT -> "not over-bending your front knee."
    VerdictCode.FRONT_KNEE_PAST_ANKLE -> "keeping your front knee over your ankle."
    VerdictCode.BACK_LEG_BENT -> "keeping your back leg straight."
    VerdictCode.ARMS_NOT_LEVEL -> "keeping your arms level."
    VerdictCode.TORSO_LEANING -> "keeping your torso upright."
    VerdictCode.INSUFFICIENT_VISIBILITY -> "staying fully in frame."
}

@Preview(showBackground = true)
@Composable
private fun SessionSummaryScreenPreview() {
    SessionSummaryScreen(
        routineTitle = "Foundations — Full Body Wake-Up",
        totalElapsedMs = 185_000L,
        stepResults = listOf(
            StepResult("warrior_ii", com.mira.miraai.perception.Side.LEFT, VerdictCode.FRONT_KNEE_PAST_ANKLE, true),
            StepResult("warrior_ii", com.mira.miraai.perception.Side.RIGHT, VerdictCode.GOOD_FORM, true),
        ),
        posesById = emptyMap(),
        nextFocusVerdict = VerdictCode.FRONT_KNEE_PAST_ANKLE,
        onDoneClick = {},
        onSeeWhatMiraLearnedClick = null,
    )
}
