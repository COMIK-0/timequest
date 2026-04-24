package com.example.timequest.presentation.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.timequest.R
import com.example.timequest.presentation.tasks.TaskViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun StatisticsScreen(taskViewModel: TaskViewModel) {
    val tasks by taskViewModel.allTasks.collectAsState()
    val totalTasks = tasks.size
    val completedTasks = tasks.count { it.isCompleted }
    val activeTasks = totalTasks - completedTasks
    val completionProgress = if (totalTasks == 0) 0f else completedTasks.toFloat() / totalTasks
    val completionPercent = (completionProgress * 100).toInt()
    val otherCategory = stringResource(R.string.category_other)
    val lowPriority = stringResource(R.string.priority_low)
    val mediumPriority = stringResource(R.string.priority_medium)
    val highPriority = stringResource(R.string.priority_high)
    val categoryCounts = tasks
        .groupingBy { it.category.ifBlank { otherCategory } }
        .eachCount()
        .toList()
        .sortedByDescending { it.second }
    val priorityCounts = tasks
        .groupingBy { priorityLabel(it.priority, lowPriority, mediumPriority, highPriority) }
        .eachCount()
        .toList()
    val completedLastSevenDays = tasks.count { task ->
        task.completedAt?.toLocalDate()?.let { completedDate ->
            val today = LocalDate.now()
            completedDate >= today.minusDays(6) && completedDate <= today
        } == true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.statistics_title),
            style = MaterialTheme.typography.headlineMedium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = stringResource(R.string.total_tasks),
                value = totalTasks.toString(),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = stringResource(R.string.active_tasks_count),
                value = activeTasks.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = stringResource(R.string.completed_tasks),
                value = completedTasks.toString(),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = stringResource(R.string.completed_last_7_days),
                value = completedLastSevenDays.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        SectionCard {
            Text(
                text = stringResource(R.string.completion_percentage),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "$completionPercent%",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            LinearProgressIndicator(
                progress = { completionProgress },
                modifier = Modifier.fillMaxWidth()
            )
        }

        SectionCard {
            Text(
                text = stringResource(R.string.tasks_by_category),
                style = MaterialTheme.typography.titleMedium
            )
            if (categoryCounts.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_statistics_yet),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                categoryCounts.forEach { (category, count) ->
                    StatLine(label = category, value = count.toString())
                }
            }
        }

        SectionCard {
            Text(
                text = stringResource(R.string.tasks_by_priority),
                style = MaterialTheme.typography.titleMedium
            )
            if (priorityCounts.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_statistics_yet),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                priorityCounts.forEach { (priority, count) ->
                    StatLine(label = priority, value = count.toString())
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private fun priorityLabel(
    priority: String,
    lowLabel: String,
    mediumLabel: String,
    highLabel: String
): String {
    return when (priority) {
        "high" -> highLabel
        "medium" -> mediumLabel
        else -> lowLabel
    }
}

private fun Long.toLocalDate(): LocalDate {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}
