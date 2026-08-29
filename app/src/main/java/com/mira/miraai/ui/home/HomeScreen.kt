package com.mira.miraai.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mira.miraai.content.Category
import com.mira.miraai.content.Routine
import com.mira.miraai.content.RoutineLevel
import com.mira.miraai.ui.theme.MiraColors
import com.mira.miraai.ui.theme.MiraRadius
import com.mira.miraai.ui.theme.MiraSpacing
import com.mira.miraai.ui.theme.MiraType

/**
 * Home / Discover — US-1 (feature-spec.md), matching docs/ux/home_the_invitation.
 *
 * - Freestyle hero card always shown first (US-1: "Given the user has never opened the app...
 *   shows the Freestyle hero card first, then the category rail").
 * - Continue card shown only when [lastRoutine] is non-null (a completed prior session).
 */
@Composable
fun HomeScreen(
    greeting: String,
    categories: List<Category>,
    recommendedRoutines: List<Routine>,
    lastRoutine: Routine?,
    onFreestyleClick: () -> Unit,
    onContinueClick: (Routine) -> Unit,
    onCategoryClick: (Category) -> Unit,
    onRoutineClick: (Routine) -> Unit,
    onSettingsClick: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MiraColors.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = MiraSpacing.stackLg),
    ) {
        HomeTopBar(onSettingsClick = onSettingsClick)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MiraSpacing.containerPadding)
                .padding(top = MiraSpacing.stackMd),
            verticalArrangement = Arrangement.spacedBy(MiraSpacing.stackLg),
        ) {
            Text(text = greeting, style = MiraType.headlineLgMobile, color = MiraColors.primaryContainer)

            FreestyleHeroCard(onClick = onFreestyleClick)

            if (lastRoutine != null) {
                ContinueCard(routine = lastRoutine, onClick = { onContinueClick(lastRoutine) })
            }

            CategoryRail(categories = categories, onCategoryClick = onCategoryClick)

            RecommendedRoutines(routines = recommendedRoutines, onRoutineClick = onRoutineClick)
        }
    }
}

@Composable
private fun HomeTopBar(onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MiraColors.surface)
            .padding(horizontal = MiraSpacing.containerPadding, vertical = MiraSpacing.stackSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MiraColors.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Person, contentDescription = "User profile", tint = MiraColors.primary)
        }
        Text(
            text = "Mira.ai",
            style = MiraType.headlineMd,
            color = MiraColors.primaryContainer,
            modifier = Modifier.weight(1f).padding(horizontal = MiraSpacing.gutter),
        )
        Icon(
            Icons.Filled.Settings,
            contentDescription = "Settings",
            tint = MiraColors.primary,
            modifier = Modifier.clickable(onClick = onSettingsClick),
        )
    }
}

@Composable
private fun FreestyleHeroCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MiraColors.surfaceContainerLowest)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(MiraSpacing.stackSm)) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MiraColors.softGold),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Mic, contentDescription = null, tint = MiraColors.surfaceContainerLowest)
            }
            Text(text = "Freestyle Session", style = MiraType.headlineMd, color = MiraColors.primaryContainer)
            Text(text = "Tap to begin a conversation.", style = MiraType.bodyLg, color = MiraColors.onSurfaceVariant)
        }
    }
}

@Composable
private fun ContinueCard(routine: Routine, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MiraRadius.md))
            .background(MiraColors.surfaceContainerLowest)
            .clickable(onClick = onClick)
            .padding(MiraSpacing.stackMd),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(text = "Continue", style = MiraType.labelMd, color = MiraColors.tertiaryContainer)
            Text(text = routine.title, style = MiraType.headlineMd, color = MiraColors.primaryContainer)
        }
        Text(text = "Resume", style = MiraType.labelMd, color = MiraColors.primary)
    }
}

@Composable
private fun CategoryRail(categories: List<Category>, onCategoryClick: (Category) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MiraSpacing.stackSm)) {
        Text(text = "Explore", style = MiraType.bodyLg, color = MiraColors.primaryContainer)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(MiraSpacing.gutter)) {
            items(categories, key = { it.id }) { category ->
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(MiraRadius.card))
                        .background(MiraColors.surfaceContainerLowest)
                        .clickable { onCategoryClick(category) }
                        .padding(MiraSpacing.stackMd)
                        .width(160.dp),
                ) {
                    Image(
                        painter = painterResource(category.iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                    )
                    Spacer(Modifier.height(MiraSpacing.unit))
                    Text(text = category.title, style = MiraType.labelMd, color = MiraColors.primaryContainer)
                    Text(text = category.subtitle, style = MiraType.labelSm, color = MiraColors.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun RecommendedRoutines(routines: List<Routine>, onRoutineClick: (Routine) -> Unit) {
    if (routines.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(MiraSpacing.stackSm)) {
        Text(text = "Recommended for you", style = MiraType.bodyLg, color = MiraColors.primaryContainer)
        routines.forEach { routine ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(MiraRadius.card))
                    .background(MiraColors.surfaceContainerLowest)
                    .clickable { onRoutineClick(routine) }
                    .padding(MiraSpacing.stackMd),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(text = routine.title, style = MiraType.labelMd, color = MiraColors.primaryContainer)
                    Text(text = formatLevelAndDuration(routine), style = MiraType.labelSm, color = MiraColors.onSurfaceVariant)
                }
                Icon(Icons.Filled.Schedule, contentDescription = null, tint = MiraColors.onSurfaceVariant)
            }
        }
    }
}

internal fun formatLevelAndDuration(routine: Routine): String {
    val minutes = routine.estimatedDurationSec / 60
    val level = routine.level.name.lowercase().replaceFirstChar { it.uppercase() }
    return "$level · $minutes min"
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    HomeScreen(
        greeting = "Good evening —\nready to unwind?",
        categories = emptyList(),
        recommendedRoutines = emptyList(),
        lastRoutine = null,
        onFreestyleClick = {},
        onContinueClick = {},
        onCategoryClick = {},
        onRoutineClick = {},
    )
}
