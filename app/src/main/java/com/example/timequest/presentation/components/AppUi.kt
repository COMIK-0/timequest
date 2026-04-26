package com.example.timequest.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.timequest.ui.theme.DangerRed
import com.example.timequest.ui.theme.SuccessGreen
import com.example.timequest.ui.theme.WarningAmber

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    }
}

@Composable
fun AppChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.14f),
        contentColor = color,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1
        )
    }
}

@Composable
fun XpBadge(xp: Int, modifier: Modifier = Modifier) {
    AppChip(
        text = "$xp XP",
        color = SuccessGreen,
        modifier = modifier
    )
}

@Composable
fun PriorityChip(priority: String, modifier: Modifier = Modifier) {
    AppChip(
        text = priorityLabel(priority),
        color = priorityColor(priority),
        modifier = modifier
    )
}

@Composable
fun DifficultyChip(difficulty: String, modifier: Modifier = Modifier) {
    AppChip(
        text = difficultyLabel(difficulty),
        color = difficultyColor(difficulty),
        modifier = modifier
    )
}

@Composable
fun SoftProgress(progress: Float, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { progress },
        modifier = modifier,
        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    )
}

fun priorityColor(priority: String): Color {
    return when (priority) {
        "high" -> DangerRed
        "medium" -> WarningAmber
        else -> SuccessGreen
    }
}

fun difficultyColor(difficulty: String): Color {
    return when (difficulty) {
        "hard" -> Color(0xFFBFA7FF)
        "medium" -> Color(0xFF8FB7FF)
        else -> SuccessGreen
    }
}

fun priorityLabel(priority: String): String {
    return when (priority) {
        "high" -> "Высокий"
        "medium" -> "Средний"
        else -> "Низкий"
    }
}

fun difficultyLabel(difficulty: String): String {
    return when (difficulty) {
        "hard" -> "Сложная"
        "medium" -> "Средняя"
        else -> "Лёгкая"
    }
}
