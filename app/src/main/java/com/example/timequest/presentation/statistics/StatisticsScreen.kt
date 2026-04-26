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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.timequest.R
import com.example.timequest.data.local.TaskEntity
import com.example.timequest.domain.GamificationManager
import com.example.timequest.presentation.components.SoftProgress
import com.example.timequest.presentation.tasks.TaskViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun StatisticsScreen(taskViewModel: TaskViewModel) {
    val tasks by taskViewModel.allTasks.collectAsState()
    val totalTasks = tasks.size
    val completedTasks = tasks.count { it.isCompleted }
    val activeTasks = tasks.filter { !it.isCompleted }
    val completedToday = tasks.count { task -> task.completedAt?.toLocalDate() == LocalDate.now() }
    val totalXp = GamificationManager.totalXp(tasks)
    val bestDay = GamificationManager.bestCompletionDay(tasks)
    val completionProgress = if (totalTasks == 0) 0f else completedTasks.toFloat() / totalTasks
    val completionPercent = (completionProgress * 100).toInt()
    val lastSevenDays = (6 downTo 0).map { daysAgo -> LocalDate.now().minusDays(daysAgo.toLong()) }
    val activityByDay = lastSevenDays.map { date ->
        date to tasks.count { task -> task.completedAt?.toLocalDate() == date }
    }
    val completedLastSevenDays = activityByDay.sumOf { it.second }
    val maxDayCount = activityByDay.maxOfOrNull { it.second } ?: 0
    val otherCategory = stringResource(R.string.category_other)
    val lowPriority = stringResource(R.string.priority_low)
    val mediumPriority = stringResource(R.string.priority_medium)
    val highPriority = stringResource(R.string.priority_high)
    val categoryCounts = tasks
        .groupingBy { task -> task.category.ifBlank { otherCategory } }
        .eachCount()
        .toList()
        .sortedByDescending { it.second }
    val priorityCounts = listOf(
        highPriority to tasks.count { it.priority == "high" },
        mediumPriority to tasks.count { it.priority == "medium" },
        lowPriority to tasks.count { it.priority != "high" && it.priority != "medium" }
    )
    val averageActiveTime = activeTasks
        .map { it.estimatedMinutes }
        .filter { it > 0 }
        .average()
        .takeIf { !it.isNaN() }
        ?.toInt() ?: 0
    val todayPlannedTime = tasks
        .filter { task -> task.dueDate?.toLocalDate() == LocalDate.now() }
        .sumOf { it.estimatedMinutes.coerceAtLeast(0) }

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

        if (tasks.isEmpty()) {
            SectionCard {
                Text(
                    text = "Статистика появится после добавления и выполнения задач.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Column
        }

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
                value = activeTasks.size.toString(),
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
                title = stringResource(R.string.completion_percentage),
                value = "$completionPercent%",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = stringResource(R.string.completed_today_count),
                value = completedToday.toString(),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = stringResource(R.string.completed_last_7_days),
                value = completedLastSevenDays.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Всего XP",
                value = totalXp.toString(),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Лучший день",
                value = bestDay?.let { "${it.first.format(dayFormatter())}: ${it.second}" } ?: "0",
                modifier = Modifier.weight(1f)
            )
        }

        SectionCard {
            Text(
                text = stringResource(R.string.completion_percentage),
                style = MaterialTheme.typography.titleMedium
            )
            SoftProgress(progress = completionProgress, modifier = Modifier.fillMaxWidth())
            Text(
                text = "$completedTasks из $totalTasks задач выполнено",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SectionCard {
            Text(
                text = "Активность за 7 дней",
                style = MaterialTheme.typography.titleMedium
            )
            activityByDay.forEach { (date, count) ->
                val progress = if (maxDayCount == 0) 0f else count.toFloat() / maxDayCount
                ProgressLine(
                    label = date.format(dayFormatter()),
                    value = count.toString(),
                    progress = progress
                )
            }
        }

        SectionCard {
            Text(
                text = "Категории",
                style = MaterialTheme.typography.titleMedium
            )
            categoryCounts.forEach { (category, count) ->
                StatLine(label = category, value = "$count")
            }
        }

        SectionCard {
            Text(
                text = "Приоритеты",
                style = MaterialTheme.typography.titleMedium
            )
            priorityCounts.forEach { (priority, count) ->
                StatLine(label = priority, value = "$count")
            }
        }

        SectionCard {
            Text(
                text = "Среднее время",
                style = MaterialTheme.typography.titleMedium
            )
            StatLine(
                label = "Среднее время активных задач",
                value = "$averageActiveTime мин"
            )
            StatLine(
                label = "Запланировано на сегодня",
                value = "$todayPlannedTime мин"
            )
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
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ProgressLine(
    label: String,
    value: String,
    progress: Float
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        StatLine(label = label, value = value)
        SoftProgress(progress = progress, modifier = Modifier.fillMaxWidth())
    }
}

private fun Long.toLocalDate(): LocalDate {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}

private fun dayFormatter(): DateTimeFormatter {
    return DateTimeFormatter.ofPattern("dd.MM", Locale.getDefault())
}
