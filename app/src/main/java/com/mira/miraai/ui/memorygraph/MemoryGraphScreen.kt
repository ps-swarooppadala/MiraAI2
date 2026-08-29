package com.mira.miraai.ui.memorygraph

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.mira.miraai.memory.Fact
import com.mira.miraai.ui.theme.MiraColors
import com.mira.miraai.ui.theme.MiraRadius
import com.mira.miraai.ui.theme.MiraSpacing
import com.mira.miraai.ui.theme.MiraType
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * US-9 Memory Graph screen (feature-spec.md).
 *
 * **Doc gap, flagged not silently guessed — same pattern as Phase 5's Routine Detail and Phase
 * 8's Freestyle screen:** the task instructions point at `docs/ux/memory-graph.md`; no such file
 * exists in `docs/ux/`. Built directly against US-9's own Given/When/Then + `DESIGN.md`'s general
 * card/color-token system instead. Whoever runs a docs pass next should add a real export.
 */
private const val MIN_FACTS_FOR_GRAPH = 3

@Composable
fun MemoryGraphScreen(
    facts: List<Fact>,
    onBackClick: () -> Unit,
    onExportClick: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MiraColors.background)
            .padding(MiraSpacing.containerPadding),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Go back",
                tint = MiraColors.primary,
                modifier = Modifier.clickable(onClick = onBackClick),
            )
            Text(text = "What Mira knows", style = MiraType.headlineMd, color = MiraColors.primary)
            Spacer(Modifier.size(24.dp))
        }

        Spacer(Modifier.height(MiraSpacing.stackSm))

        ReassuranceLine()

        Spacer(Modifier.height(MiraSpacing.stackLg))

        if (facts.size < MIN_FACTS_FOR_GRAPH) {
            EarlyState()
        } else {
            FactGraph(facts)
        }

        if (onExportClick != null && facts.isNotEmpty()) {
            Spacer(Modifier.height(MiraSpacing.stackMd))
            ExportMemoryButton(onClick = onExportClick)
        }
    }
}

/**
 * build-architecture.md Section 5's JSON-export fallback trigger — dumps [Fact] rows to a file
 * a laptop script can render offline. The primary embedded-server + live D3 page is a later
 * polish layer, not built this phase.
 */
@Composable
private fun ExportMemoryButton(onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MiraRadius.card))
            .background(MiraColors.surfaceContainerLowest)
            .clickable(onClick = onClick)
            .padding(MiraSpacing.stackMd),
    ) {
        Text(text = "Export memory", style = MiraType.labelMd, color = MiraColors.primary)
    }
}

@Composable
private fun ReassuranceLine() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MiraSpacing.unit)) {
        Icon(Icons.Filled.Lock, contentDescription = null, tint = MiraColors.onSurfaceVariant, modifier = Modifier.size(16.dp))
        Text(text = "Everything here stays on your phone", style = MiraType.labelMd, color = MiraColors.onSurfaceVariant)
    }
}

@Composable
private fun EarlyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = MiraSpacing.stackLg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MiraColors.primaryContainer),
        )
        Spacer(Modifier.height(MiraSpacing.stackMd))
        Text(
            text = "I'm still getting to know you",
            style = MiraType.headlineMd,
            color = MiraColors.primary,
        )
        Spacer(Modifier.height(MiraSpacing.unit))
        Text(
            text = "Practice a few more sessions and I'll start sharing what I've noticed.",
            style = MiraType.bodyMd,
            color = MiraColors.onSurfaceVariant,
        )
    }
}

@Composable
private fun FactGraph(facts: List<Fact>) {
    var selectedFact by remember { mutableStateOf<Fact?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val nodePositions = remember(facts, canvasSize) { computeNodePositions(facts, canvasSize) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .onGloballyPositioned { canvasSize = it.size },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (canvasSize == IntSize.Zero) return@Canvas
            val center = Offset(size.width / 2f, size.height / 2f)
            nodePositions.forEach { (fact, offset) ->
                drawLine(
                    color = MiraColors.softGold.copy(alpha = 0.25f + fact.confidence * 0.6f),
                    start = center,
                    end = offset,
                    strokeWidth = 2f + fact.confidence * 6f,
                    cap = StrokeCap.Round,
                )
            }
        }

        // Central "you" node.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(64.dp)
                .clip(CircleShape)
                .background(MiraColors.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "You", style = MiraType.labelMd, color = MiraColors.background)
        }

        val density = LocalDensity.current
        val nodeSizeDp = 40.dp
        val halfNodePx = with(density) { (nodeSizeDp / 2).toPx() }

        nodePositions.forEach { (fact, offset) ->
            Box(
                modifier = Modifier
                    .offset { IntOffset((offset.x - halfNodePx).toInt(), (offset.y - halfNodePx).toInt()) }
                    .size(nodeSizeDp)
                    .clip(CircleShape)
                    .background(MiraColors.softGold.copy(alpha = 0.5f + fact.confidence * 0.5f))
                    .clickable { selectedFact = fact },
            )
        }
    }

    selectedFact?.let { fact ->
        Spacer(Modifier.height(MiraSpacing.stackMd))
        FactCard(fact)
    }
}

@Composable
private fun FactCard(fact: Fact) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MiraRadius.card))
            .background(MiraColors.surfaceContainerLowest)
            .padding(MiraSpacing.stackMd),
    ) {
        Text(text = naturalLanguage(fact), style = MiraType.bodyLg, color = MiraColors.primary)
        Spacer(Modifier.height(MiraSpacing.unit))
        Text(
            text = "Last updated ${relativeTime(fact.lastUpdated)} · from session ${fact.sourceSessionId}",
            style = MiraType.labelMd,
            color = MiraColors.onSurfaceVariant,
        )
    }
}

private fun computeNodePositions(facts: List<Fact>, canvasSize: IntSize): List<Pair<Fact, Offset>> {
    if (canvasSize == IntSize.Zero) return emptyList()
    val center = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
    val radius = min(canvasSize.width, canvasSize.height) / 2.4f
    val angleStep = (2 * Math.PI / facts.size)
    return facts.mapIndexed { index, fact ->
        val angle = angleStep * index - Math.PI / 2
        val offset = Offset(
            x = center.x + (radius * cos(angle)).toFloat(),
            y = center.y + (radius * sin(angle)).toFloat(),
        )
        fact to offset
    }
}

private fun naturalLanguage(fact: Fact): String {
    val poseName = fact.subject.removePrefix("user.").replace('_', ' ')
    return when (fact.predicate) {
        "struggles_with" -> "You tend to struggle with ${fact.objectValue.replace('_', ' ')} in ${poseName.ifBlank { "your practice" }}."
        "avg_hold_time" -> "Your average hold time is ${fact.objectValue}."
        "prefers" -> "You seem to prefer ${fact.objectValue.replace('_', ' ')}."
        else -> "${fact.subject}: ${fact.predicate.replace('_', ' ')} ${fact.objectValue.replace('_', ' ')}."
    }
}

private fun relativeTime(lastUpdatedMs: Long): String {
    val deltaMs = System.currentTimeMillis() - lastUpdatedMs
    val days = deltaMs / (1000 * 60 * 60 * 24)
    return when {
        days <= 0 -> "today"
        days == 1L -> "yesterday"
        else -> "$days days ago"
    }
}

@Preview(showBackground = true)
@Composable
private fun MemoryGraphScreenEarlyPreview() {
    MemoryGraphScreen(facts = emptyList(), onBackClick = {})
}

@Preview(showBackground = true)
@Composable
private fun MemoryGraphScreenPreview() {
    MemoryGraphScreen(
        facts = listOf(
            Fact("f1", "user.warrior_ii", "struggles_with", "front_knee_past_ankle", 0.8f, System.currentTimeMillis(), "s3"),
            Fact("f2", "user", "avg_hold_time", "18s", 0.6f, System.currentTimeMillis(), "s3"),
            Fact("f3", "user", "prefers", "shorter_holds", 0.4f, System.currentTimeMillis(), "s2"),
        ),
        onBackClick = {},
    )
}
