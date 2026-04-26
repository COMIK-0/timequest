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
    val activeTasks = tasks.filter { !it.isCompleted }
    val todayTasks = tasks.filter { task -> task.dueDate?.isToday() == true }
    val activeTodayTasks = todayTasks.filter { task -> !task.isCompleted }
    val sortedTodayTasks = TaskPrioritizer.sortSmart(activeTodayTasks)
    val allScheduledTodayTasks = todayTasks
        .filter { task -> task.scheduledStartTime != null && task.scheduledEndTime != null }
        .sortedBy { task -> task.scheduledStartTime }
    val activeScheduledTodayTasks = allScheduledTodayTasks.filter { task -> !task.isCompleted }
    val remainingPlannedMinutes = activeScheduledTodayTasks.sumOf { task ->
        task.estimatedMinutes.coerceAtLeast(0)
    }
    val completedToday = tasks.count { task -> task.completedAt?.isToday() == true }
    val completedTasksToday = tasks.filter { task -> task.completedAt?.isToday() == true }
    val todayXp = completedTasksToday
        .filter { task -> task.xpAwarded }
        .sumOf { task -> GamificationManager.calculateTaskXp(task) }
    val bestCategoryToday = completedTasksToday
        .groupingBy { task -> task.category.ifBlank { "Без категории" } }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key
    val completedTodayFromTodayTasks = todayTasks.count { task -> task.isCompleted }
    val todayProgress = if (todayTasks.isEmpty()) {
        0f
    } else {
        completedTodayFromTodayTasks.toFloat() / todayTasks.size
    }
    val firstTask = activeScheduledTodayTasks.firstOrNull() ?: sortedTodayTasks.firstOrNull()
    val totalXp = GamificationManager.totalXp(tasks)
    val levelInfo = GamificationManager.levelInfo(totalXp)
    val isTodayFullyCompleted = todayTasks.isNotEmpty() && activeTodayTasks.isEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.today_title),
            style = MaterialTheme.typography.headlineMedium
        )

        if (tasks.isEmpty()) {
            InfoCard {
                Text(
                    text = stringResource(R.string.dashboard_empty_state),
                    style = MaterialTheme.typography.bodyLarge
                )
                Button(
                    onClick = onAddTask,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(text = stringResource(R.string.add_task))
                }
            }
            return@Column
        }

        InfoCard {
            Text(
                text = "Сегодня",
                style = MaterialTheme.typography.titleLarge
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SmallMetricCard(
                    title = "Задач на дату",
                    value = todayTasks.size.toString(),
                    modifier = Modifier.weight(1f)
                )
                SmallMetricCard(
                    title = stringResource(R.string.completed_today_count),
                    value = completedToday.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = stringResource(
                    R.string.daily_progress_value,
                    completedTodayFromTodayTasks,
                    todayTasks.size
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SoftProgress(progress = todayProgress, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = { onOpenDayPlanner(todayStartMillis()) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Распределить задачи на день")
            }
        }

        if (firstTask != null) {
            InfoCard(
                highlighted = true
            ) {
                Text(
                    text = "Начните с этого",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                TaskPreview(task = firstTask, important = true)
            }
        }

        InfoCard {
            Text(
                text = "Уровень и прогресс",
                style = MaterialTheme.typography.titleLarge
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "Уровень ${levelInfo.level}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$totalXp XP",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            SoftProgress(progress = levelInfo.progress, modifier = Modifier.fillMaxWidth())
            Text(
                text = "До следующего уровня: ${(levelInfo.nextLevelXp - totalXp).coerceAtLeast(0)} XP",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        InfoCard {
            Text(
                text = "Итог дня",
                style = MaterialTheme.typography.titleLarge
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SmallMetricCard(
                    title = "Выполнено",
                    value = completedToday.toString(),
                    modifier = Modifier.weight(1f)
                )
                SmallMetricCard(
                    title = "XP сегодня",
                    value = todayXp.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = if (bestCategoryToday != null) {
                    "Лучшее направление сегодня: $bestCategoryToday."
                } else {
                    "Завершите задачу, чтобы появился отчёт дня."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Осталось на сегодня: ${activeTodayTasks.size} задач.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        InfoCard {
            Text(
                text = if (allScheduledTodayTasks.isNotEmpty()) {
                    "План на день"
                } else {
                    "Рекомендуемый план на сегодня"
                },
                style = MaterialTheme.typography.titleLarge
            )

            if (isTodayFullyCompleted) {
                Text(
                    text = "Все задачи на сегодня выполнены. Отличная работа! Приходите с новыми задачами.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (activeScheduledTodayTasks.isNotEmpty()) {
                Text(
                    text = "Осталось: ${activeScheduledTodayTasks.size} задач • ${remainingPlannedMinutes} мин",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                activeScheduledTodayTasks.forEach { task ->
                    ScheduledTaskPreview(task = task)
                }
            } else if (sortedTodayTasks.isEmpty()) {
                Text(
                    text = "На сегодня активных задач нет.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onAddTask,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Добавить задачу")
                }
            } else {
                sortedTodayTasks.forEach { task ->
                    TaskPreview(task = task)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenTasks,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text(
                        text = "Задачи",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (allScheduledTodayTasks.isNotEmpty()) {
                    Button(
                        onClick = { onOpenDayPlanner(todayStartMillis()) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text(
                            text = "План",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Button(
            onClick = onAddTask,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.add_task))
        }
    }
}

@Composable
private fun ScheduledTaskPreview(task: TaskEntity) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = formatTime(task.scheduledStartTime),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = formatTime(task.scheduledEndTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${task.category} • ${priorityLabel(task.priority)} • ${difficultyLabel(task.difficulty)} • ${task.estimatedMinutes} мин • ${statusLabel(task)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SmallMetricCard(
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
                color = MaterialTheme.colorScheme.primary
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
private fun TaskPreview(
    task: TaskEntity,
    important: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = task.title,
            style = if (important) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
            fontWeight = if (important) FontWeight.SemiBold else FontWeight.Normal
        )
        Text(
            text = buildTaskInfo(task),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
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

private fun buildTaskInfo(task: TaskEntity): String {
    val dateText = task.dueDate?.let { "Дата: ${formatDate(it)}" } ?: "Дата не выбрана"
    val timeText = if (task.scheduledStartTime != null && task.scheduledEndTime != null) {
        " • ${formatTime(task.scheduledStartTime)}–${formatTime(task.scheduledEndTime)}"
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
    val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

private fun formatTime(timestamp: Long?): String {
    if (timestamp == null) return "--:--"
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

private fun todayStartMillis(): Long {
    return LocalDate.now()
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}

private fun priorityLabel(priority: String): String {
    return when (priority) {
        "high" -> "высокий"
        "medium" -> "средний"
        else -> "низкий"
    }
}

private fun difficultyLabel(difficulty: String): String {
    return when (difficulty) {
        "hard" -> "сложная"
        "medium" -> "средняя"
        else -> "лёгкая"
    }
}

private fun statusLabel(task: TaskEntity): String {
    return if (task.isCompleted) "выполнена" else "активна"
}
