package com.mira.miraai.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mira.miraai.content.Category
import com.mira.miraai.content.Routine
import com.mira.miraai.ui.home.formatLevelAndDuration
import com.mira.miraai.ui.theme.MiraColors
import com.mira.miraai.ui.theme.MiraRadius
import com.mira.miraai.ui.theme.MiraSpacing
import com.mira.miraai.ui.theme.MiraType

/**
 * Category Browse — US-2 (feature-spec.md), matching docs/ux/workout_picker_choose_your_practice.
 *
 * Given/When/Then coverage:
 * - N routines in [category] render N cards showing title, formatted duration, level, cover.
 * - A routine with `isCoachingSupported == false` shows a "Preview only" badge in place of the
 *   duration chip (Category Browse still lets you tap through to preview it — the CTA it must
 *   disable per spec is "Start Workout", which lives on Routine Detail, US-3).
 */
@Composable
fun CategoryBrowseScreen(
    category: Category,
    routines: List<Routine>,
    onBackClick: () -> Unit,
    onRoutineClick: (Routine) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MiraColors.primaryContainer),
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
                tint = MiraColors.primaryFixed,
                modifier = Modifier.clickable(onClick = onBackClick),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MiraSpacing.containerPadding),
        ) {
            Text(text = "Daily Practice", style = MiraType.labelMd, color = MiraColors.primaryFixedDim)
            Text(
                text = category.title,
                style = MiraType.headlineLgMobile,
                color = MiraColors.primaryFixed,
                modifier = Modifier.padding(top = MiraSpacing.unit),
            )
            Text(
                text = category.subtitle,
                style = MiraType.bodyMd,
                color = MiraColors.primaryFixedDim,
                modifier = Modifier.padding(top = MiraSpacing.stackSm, bottom = MiraSpacing.stackLg),
            )

            if (routines.isEmpty()) {
                Text(
                    text = "New routines for this category are coming soon.",
                    style = MiraType.bodyMd,
                    color = MiraColors.primaryFixedDim,
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(MiraSpacing.gutter)) {
                    items(routines, key = { it.id }) { routine ->
                        RoutineCard(routine = routine, onClick = { onRoutineClick(routine) })
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutineCard(routine: Routine, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MiraRadius.card))
            .background(MiraColors.surface)
            .clickable(onClick = onClick)
            .padding(MiraSpacing.stackMd),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (routine.isCoachingSupported) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MiraColors.primaryFixed.copy(alpha = 0.4f))
                        .padding(horizontal = MiraSpacing.stackSm, vertical = MiraSpacing.unit / 2),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, tint = MiraColors.primaryContainer, modifier = Modifier.padding(end = 4.dp))
                    Text(text = formatLevelAndDuration(routine), style = MiraType.labelSm, color = MiraColors.primaryContainer)
                }
            } else {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MiraColors.surfaceVariant)
                        .padding(horizontal = MiraSpacing.stackSm, vertical = MiraSpacing.unit / 2),
                ) {
                    Text(text = "Preview only", style = MiraType.labelSm, color = MiraColors.onSurfaceVariant)
                }
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MiraColors.primaryFixedDim)
        }
        Text(
            text = routine.title,
            style = MiraType.headlineMd,
            color = MiraColors.primary,
            modifier = Modifier.padding(top = MiraSpacing.stackMd),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(MiraRadius.sm))
                .background(MiraColors.surfaceContainerLow)
                .padding(MiraSpacing.stackSm)
                .padding(top = MiraSpacing.unit),
        ) {
            Text(
                text = if (routine.isCoachingSupported) {
                    "${routine.poseSequence.size} poses · live-corrected"
                } else {
                    "${routine.poseSequence.size} poses · coaching not yet available"
                },
                style = MiraType.bodyMd,
                color = MiraColors.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CategoryBrowseScreenPreview() {
    CategoryBrowseScreen(
        category = Category("full_body", "Full Body", "Standing strength & balance", 0, emptyList()),
        routines = emptyList(),
        onBackClick = {},
        onRoutineClick = {},
    )
}
