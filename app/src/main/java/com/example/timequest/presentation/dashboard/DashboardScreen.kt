package com.example.timequest.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import com.example.timequest.data.local.TaskEntity
import com.example.timequest.domain.TaskPrioritizer
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
    onAddTask: () -> Unit
) {
    val tasks by taskViewModel.allTasks.collectAsState()
    val activeTasks = tasks.filter { !it.isCompleted }
    val completedToday = tasks.count { it.isCompleted && it.completedAt?.isToday() == true }
    val todayTasks = tasks.filter { task ->
        task.dueDate?.isToday() == true || task.completedAt?.isToday() == true
    }
    val completedTodayFromTodayTasks = todayTasks.count { it.isCompleted }
    val progress = if (todayTasks.isEmpty()) 0f else completedTodayFromTodayTasks.toFloat() / todayTasks.size
    val nearestDeadline = activeTasks
        .filter { it.dueDate != null }
        .minByOrNull { it.dueDate ?: Long.MAX_VALUE }
    val recommendedToday = TaskPrioritizer.sortSmart(activeTasks.filter { task ->
        task.dueDate?.isToday() == true
    })
    val nearestTasks = TaskPrioritizer.sortSmart(activeTasks).take(3)

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
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SmallMetricCard(
                    title = stringResource(R.string.active_tasks_count),
                    value = activeTasks.size.toString(),
                    modifier = Modifier.weight(1f)
                )
                SmallMetricCard(
                    title = stringResource(R.string.completed_today_count),
                    value = completedToday.toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            InfoCard {
                Text(
                    text = stringResource(R.string.daily_progress),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(
                        R.string.daily_progress_value,
                        completedTodayFromTodayTasks,
                        todayTasks.size
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            InfoCard {
                Text(
                    text = stringResource(R.string.nearest_deadline),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = nearestDeadline?.let {
                        stringResource(R.string.task_with_date, it.title, formatDate(it.dueDate ?: 0L))
                    } ?: stringResource(R.string.no_nearest_deadline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            InfoCard {
                Text(
                    text = stringResource(R.string.level_xp_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.level_xp_placeholder),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            InfoCard {
                Text(
                    text = stringResource(R.string.recommended_plan_today),
                    style = MaterialTheme.typography.titleMedium
                )
                if (recommendedToday.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_recommended_tasks),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    recommendedToday.take(3).forEachIndexed { index, task ->
                        Text(
                            text = if (index == 0) {
                                stringResource(R.string.start_with_task, task.title)
                            } else {
                                task.title
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (index == 0) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }

            InfoCard {
                Text(
                    text = stringResource(R.string.nearest_active_tasks),
                    style = MaterialTheme.typography.titleMedium
                )
                nearestTasks.forEach { task ->
                    Text(
                        text = task.dueDate?.let {
                            stringResource(R.string.task_with_date, task.title, formatDate(it))
                        } ?: task.title,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Button(onClick = onOpenTasks) {
                    Text(text = stringResource(R.string.open_tasks))
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
}

@Composable
private fun SmallMetricCard(
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
private fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
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

private fun Long.isToday(): Boolean {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate() == LocalDate.now()
}

private fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
