package com.mira.miraai.ui.routinedetail

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.mira.miraai.content.Pose
import com.mira.miraai.content.PoseSide
import com.mira.miraai.content.Routine
import com.mira.miraai.content.RoutineStep
import com.mira.miraai.ui.home.formatLevelAndDuration
import com.mira.miraai.ui.theme.MiraColors
import com.mira.miraai.ui.theme.MiraRadius
import com.mira.miraai.ui.theme.MiraSpacing
import com.mira.miraai.ui.theme.MiraType

/**
 * Routine Detail — US-3 (feature-spec.md). No dedicated Stitch export exists for this screen
 * (docs/ux only covers Home, the practice picker, and Setup Tips) — built against
 * docs/ux/DESIGN.md's general system (cards, pill CTAs, tonal surfaces) instead, matching the
 * same card language as docs/ux/workout_picker_choose_your_practice for visual consistency.
 *
 * Given/When/Then coverage:
 * - Horizontally swipeable Pose Preview carousel (one card per pose in [routine]'s sequence).
 * - "Start Workout" enabled only when `routine.isCoachingSupported`; otherwise disabled with a
 *   tooltip-equivalent caption explaining why (Compose doesn't have a native hover tooltip on
 *   touch devices, so the reason is shown as a persistent caption instead).
 */
@Composable
fun RoutineDetailScreen(
    routine: Routine,
    posesById: Map<String, Pose>,
    onBackClick: () -> Unit,
    onStartWorkoutClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MiraColors.background)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MiraSpacing.containerPadding, vertical = MiraSpacing.stackSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MiraColors.primary,
                modifier = Modifier.clickable(onClick = onBackClick),
            )
        }

        Column(modifier = Modifier.padding(horizontal = MiraSpacing.containerPadding)) {
            Text(text = routine.title, style = MiraType.headlineLgMobile, color = MiraColors.primaryContainer)
            Text(
                text = formatLevelAndDuration(routine),
                style = MiraType.bodyMd,
                color = MiraColors.onSurfaceVariant,
                modifier = Modifier.padding(top = MiraSpacing.unit, bottom = MiraSpacing.stackLg),
            )

            Text(text = "Poses in this routine", style = MiraType.bodyLg, color = MiraColors.primaryContainer)
            Spacer(Modifier.height(MiraSpacing.stackSm))
        }

        PosePreviewCarousel(steps = routine.poseSequence, posesById = posesById)

        Column(modifier = Modifier.padding(MiraSpacing.containerPadding)) {
            StartWorkoutButton(
                enabled = routine.isCoachingSupported,
                onClick = onStartWorkoutClick,
            )
            if (!routine.isCoachingSupported) {
                Text(
                    text = "Live AI coaching isn't ready for every pose in this routine yet — you can still preview it.",
                    style = MiraType.labelSm,
                    color = MiraColors.onSurfaceVariant,
                    modifier = Modifier.padding(top = MiraSpacing.stackSm),
                )
            }
        }
    }
}

@Composable
private fun PosePreviewCarousel(steps: List<RoutineStep>, posesById: Map<String, Pose>) {
    LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = MiraSpacing.containerPadding),
        horizontalArrangement = Arrangement.spacedBy(MiraSpacing.gutter),
    ) {
        items(steps, key = { "${it.poseId}-${it.order}" }) { step ->
            val pose = posesById[step.poseId]
            Column(
                modifier = Modifier
                    .width(220.dp)
                    .clip(RoundedCornerShape(MiraRadius.card))
                    .background(MiraColors.surfaceContainerLowest)
                    .padding(MiraSpacing.stackMd),
            ) {
                Text(
                    text = pose?.displayName ?: step.poseId,
                    style = MiraType.headlineMd,
                    color = MiraColors.primary,
                )
                pose?.sanskritName?.let {
                    Text(text = it, style = MiraType.labelSm, color = MiraColors.onSurfaceVariant)
                }
                Spacer(Modifier.height(MiraSpacing.stackSm))
                Text(text = sideLabel(step.side), style = MiraType.labelMd, color = MiraColors.tertiaryContainer)
                Text(text = targetLabel(step), style = MiraType.bodyMd, color = MiraColors.onSurfaceVariant)
            }
        }
    }
}

private fun sideLabel(side: PoseSide): String = when (side) {
    PoseSide.NONE -> "Single stance"
    PoseSide.LEFT -> "Left side"
    PoseSide.RIGHT -> "Right side"
    PoseSide.BOTH -> "Both sides"
}

private fun targetLabel(step: RoutineStep): String = when {
    step.targetHoldSec != null && step.targetReps != null -> "${step.targetHoldSec}s hold · ${step.targetReps} rounds"
    step.targetHoldSec != null -> "${step.targetHoldSec}s hold"
    step.targetReps != null -> "${step.targetReps} reps"
    else -> ""
}

@Composable
private fun StartWorkoutButton(enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = MiraColors.softGold,
            contentColor = MiraColors.primary,
            disabledContainerColor = MiraColors.surfaceVariant,
            disabledContentColor = MiraColors.onSurfaceVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = "Start Workout", style = MiraType.labelMd)
    }
}

@Preview(showBackground = true)
@Composable
private fun RoutineDetailScreenPreview() {
    val routine = Routine(
        id = "foundations_full_body",
        title = "Foundations — Full Body Wake-Up",
        categoryIds = listOf("full_body"),
        level = com.mira.miraai.content.RoutineLevel.BEGINNER,
        estimatedDurationSec = 420,
        coverImageRes = 0,
        poseSequence = listOf(RoutineStep("warrior_ii", PoseSide.BOTH, 20, null, 1)),
        isCoachingSupported = false,
    )
    RoutineDetailScreen(routine = routine, posesById = emptyMap(), onBackClick = {}, onStartWorkoutClick = {})
}
