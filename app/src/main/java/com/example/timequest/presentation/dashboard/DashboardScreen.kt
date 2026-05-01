package com.example.timequest.presentation.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.timequest.R
import com.example.timequest.data.local.TaskEntity
import com.example.timequest.domain.DailyGoal
import com.example.timequest.domain.GamificationManager
import com.example.timequest.domain.TaskPrioritizer
import com.example.timequest.presentation.components.AppCard
import com.example.timequest.presentation.components.EmptyStateCard
import com.example.timequest.presentation.components.SoftProgress
import com.example.timequest.presentation.tasks.TaskViewModel
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
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
    val dailyGoalCompleted = DailyGoal.completedToday(tasks)
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.dashboard_title),
            style = MaterialTheme.typography.headlineSmall
        )

        DashboardHero(
            totalTasks = todayTasks.size,
            completedTasks = completedTodayFromTodayTasks,
            progress = todayProgress,
            onAddTask = onAddTask,
            onOpenDayPlanner = { onOpenDayPlanner(todayStartMillis()) }
        )

        DailyGoalCard(completed = dailyGoalCompleted)

        AnimatedVisibility(visible = tasks.isEmpty()) {
            EmptyStateCard(
                title = "Задач пока нет",
                subtitle = "Добавьте первую задачу и начните зарабатывать XP",
                action = {
                    Button(
                        onClick = onAddTask,
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(text = stringResource(R.string.add_task))
                    }
                }
            )
        }

        if (tasks.isEmpty()) {
            return@Column
        }

        AppCard(highlighted = true) {
            Text(text = "Прогресс", style = MaterialTheme.typography.titleLarge)
            CompactStatLine("$totalXp XP", "Уровень ${levelInfo.level}")
            SoftProgress(progress = levelInfo.progress, modifier = Modifier.fillMaxWidth(), height = 8.dp)
            AnimatedContent(
                targetState = (levelInfo.nextLevelXp - totalXp).coerceAtLeast(0),
                label = "xp to next level"
            ) { remainingXp ->
                Text(
                    text = "До следующего уровня: $remainingXp XP",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        firstTask?.let { task ->
            AppCard {
                Text(
                    text = "Начать сейчас",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                TaskPreview(task = task, important = true)
            }
        }

        AppCard {
            Text(text = "Итог дня", style = MaterialTheme.typography.titleLarge)
            CompactStatLine("Выполнено", completedToday.toString())
            CompactStatLine("XP сегодня", todayXp.toString())
            Text(
                text = "Осталось на сегодня: ${activeTodayTasks.size} задач.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AppCard {
            Text(
                text = if (scheduledTodayTasks.isNotEmpty()) "План на сегодня" else "Что делать дальше",
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
                        text = "Осталось: ${activeScheduledTodayTasks.size} задач • $remainingPlannedMinutes мин",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    activeScheduledTodayTasks.forEach { task -> ScheduledTaskPreview(task = task) }
                }
                sortedTodayTasks.isEmpty() -> {
                    Text(
                        text = "Составьте план на день",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Выберите задачи на сегодня и распределите их по времени.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> sortedTodayTasks.forEach { task -> TaskPreview(task = task) }
            }

            TextButton(onClick = onOpenTasks) {
                Text(text = "Открыть список задач")
            }
        }
    }
}

@Composable
private fun DashboardHero(
    totalTasks: Int,
    completedTasks: Int,
    progress: Float,
    onAddTask: () -> Unit,
    onOpenDayPlanner: () -> Unit
) {
    val greeting = if (LocalTime.now().hour in 18..23) "Добрый вечер" else "Добрый день"
    val heroBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.42f),
            MaterialTheme.colorScheme.surface
        )
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(heroBrush)
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    AnimatedContent(targetState = totalTasks, label = "today task count") { count ->
                        Text(
                            text = "Сегодня задач: $count",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "Выполнено",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AnimatedContent(
                        targetState = "$completedTasks из $totalTasks",
                        label = "dashboard completed count"
                    ) { value ->
                        Text(
                            text = value,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                SoftProgress(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth(),
                    height = 12.dp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onAddTask,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                    ) {
                        Text(text = "Добавить", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Button(
                        onClick = onOpenDayPlanner,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                    ) {
                        Text(text = "План дня", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyGoalCard(completed: Int) {
    val progress = completed.toFloat() / DailyGoal.TARGET_TASKS
    val displayedCompleted = DailyGoal.displayedCompleted(completed)
    AppCard(highlighted = true) {
        Text(text = "Цель дня", style = MaterialTheme.typography.titleLarge)
        CompactStatLine("Выполнено", "$displayedCompleted из ${DailyGoal.TARGET_TASKS}")
        SoftProgress(progress = progress, modifier = Modifier.fillMaxWidth(), height = 8.dp)
        AnimatedVisibility(visible = completed >= DailyGoal.TARGET_TASKS) {
            Text(
                text = "Цель выполнена",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
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
        AnimatedContent(targetState = value, label = "stat value") { animatedValue ->
            Text(
                text = animatedValue,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
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

private fun buildTaskInfo(task: TaskEntity): String {
    val dateText = task.dueDate?.let { formatDate(it) } ?: "Дата не выбрана"
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
