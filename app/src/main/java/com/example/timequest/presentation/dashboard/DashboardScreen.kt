package com.example.timequest.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.timequest.R
import com.example.timequest.data.local.TaskEntity
import com.example.timequest.domain.GamificationManager
import com.example.timequest.domain.TaskPrioritizer
import com.example.timequest.presentation.components.SoftProgress
import com.example.timequest.presentation.tasks.TaskViewModel
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    taskViewModel: TaskViewModel,
    onOpenTasks: () -> Unit,
    onAddTask: () -> Unit,
    onOpenDayPlanner: (Long) -> Unit
) {
    val tasks by taskViewModel.allTasks.collectAsState()
    val todayTasks = tasks.filter { task -> task.dueDate?.isToday() == true }
    val activeTodayTasks = todayTasks.filter { task -> !task.isCompleted }
    val sortedTodayTasks = TaskPrioritizer.sortSmart(activeTodayTasks)
    val scheduledTodayTasks = todayTasks
        .filter { task -> task.scheduledStartTime != null && task.scheduledEndTime != null }
        .sortedBy { task -> task.scheduledStartTime }
    val activeScheduledTodayTasks = scheduledTodayTasks.filter { task -> !task.isCompleted }
    val remainingPlannedMinutes = activeScheduledTodayTasks.sumOf { it.estimatedMinutes.coerceAtLeast(0) }
    val completedToday = tasks.count { task -> task.completedAt?.isToday() == true }
    val completedTasksToday = tasks.filter { task -> task.completedAt?.isToday() == true }
    val todayXp = completedTasksToday
        .filter { task -> task.xpAwarded }
        .sumOf { task -> GamificationManager.calculateTaskXp(task) }
    val completedTodayFromTodayTasks = todayTasks.count { task -> task.isCompleted }
    val todayProgress = if (todayTasks.isEmpty()) 0f else completedTodayFromTodayTasks.toFloat() / todayTasks.size
    val totalXp = GamificationManager.totalXp(tasks)
    val levelInfo = GamificationManager.levelInfo(totalXp)
    val firstTask = activeScheduledTodayTasks.firstOrNull() ?: sortedTodayTasks.firstOrNull()
    val isTodayFullyCompleted = todayTasks.isNotEmpty() && activeTodayTasks.isEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(R.string.today_title),
            style = MaterialTheme.typography.headlineSmall
        )

        if (tasks.isEmpty()) {
            InfoCard {
                Text(
                    text = stringResource(R.string.dashboard_empty_state),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onAddTask,
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text(text = stringResource(R.string.add_task))
                }
            }
            return@Column
        }

        InfoCard {
            Text(text = "Сегодня", style = MaterialTheme.typography.titleLarge)
            CompactStatLine("Задач на дату", todayTasks.size.toString())
            CompactStatLine("Выполнено сегодня", completedToday.toString())
            Text(
                text = stringResource(R.string.daily_progress_value, completedTodayFromTodayTasks, todayTasks.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SoftProgress(progress = todayProgress, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = { onOpenDayPlanner(todayStartMillis()) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Распределить задачи")
            }
        }

        InfoCard(highlighted = true) {
            Text(text = "Прогресс", style = MaterialTheme.typography.titleLarge)
            CompactStatLine("$totalXp XP", "Уровень ${levelInfo.level}")
            SoftProgress(progress = levelInfo.progress, modifier = Modifier.fillMaxWidth())
            Text(
                text = "До следующего уровня: ${(levelInfo.nextLevelXp - totalXp).coerceAtLeast(0)} XP",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        firstTask?.let { task ->
            InfoCard {
                Text(
                    text = "Начать сейчас",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                TaskPreview(task = task, important = true)
            }
        }

        InfoCard {
            Text(text = "Итог дня", style = MaterialTheme.typography.titleLarge)
            CompactStatLine("Выполнено", completedToday.toString())
            CompactStatLine("XP сегодня", todayXp.toString())
            Text(
                text = "Осталось на сегодня: ${activeTodayTasks.size} задач.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        InfoCard {
            Text(
                text = if (scheduledTodayTasks.isNotEmpty()) "План на день" else "Рекомендуемый план",
                style = MaterialTheme.typography.titleLarge
            )
            when {
                isTodayFullyCompleted -> Text(
                    text = "Все задачи на сегодня выполнены.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                activeScheduledTodayTasks.isNotEmpty() -> {
                    Text(
                        text = "Осталось: ${activeScheduledTodayTasks.size} задач • ${remainingPlannedMinutes} мин",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    activeScheduledTodayTasks.forEach { task -> ScheduledTaskPreview(task = task) }
                }
                sortedTodayTasks.isEmpty() -> Text(
                    text = "На сегодня активных задач нет.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> sortedTodayTasks.forEach { task -> TaskPreview(task = task) }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenTasks,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                ) {
                    Text(text = "Задачи", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Button(
                    onClick = { onOpenDayPlanner(todayStartMillis()) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                ) {
                    Text(text = "План", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun CompactStatLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ScheduledTaskPreview(task: TaskEntity) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatTime(task.scheduledStartTime),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(0.22f)
        )
        TaskPreview(task = task, modifier = Modifier.weight(0.78f))
    }
}

@Composable
private fun TaskPreview(
    task: TaskEntity,
    modifier: Modifier = Modifier,
    important: Boolean = false
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = task.title,
            style = if (important) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
            fontWeight = if (important) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = buildTaskInfo(task),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun InfoCard(
    highlighted: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.36f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

private fun buildTaskInfo(task: TaskEntity): String {
    val dateText = task.dueDate?.let { formatDate(it) } ?: "Дата не выбрана"
    val timeText = if (task.scheduledStartTime != null && task.scheduledEndTime != null) {
        " • ${formatTime(task.scheduledStartTime)}-${formatTime(task.scheduledEndTime)}"
    } else {
        ""
    }
    return "$dateText$timeText • ${task.estimatedMinutes} мин"
}

private fun Long.isToday(): Boolean {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate() == LocalDate.now()
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(timestamp))
}

private fun formatTime(timestamp: Long?): String {
    if (timestamp == null) return "--:--"
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
}

private fun todayStartMillis(): Long {
    return LocalDate.now()
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}
