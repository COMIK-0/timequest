package com.example.timequest.presentation.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.timequest.R
import com.example.timequest.data.local.TaskEntity
import com.example.timequest.domain.GamificationManager
import com.example.timequest.domain.TaskSortOption
import com.example.timequest.notifications.TaskReminderScheduler
import com.example.timequest.presentation.tasks.components.TaskCard
import kotlinx.coroutines.launch

@Composable
fun TasksScreen(
    taskViewModel: TaskViewModel,
    onAddTask: () -> Unit,
    onEditTask: (Long) -> Unit,
    onStartFocus: () -> Unit
) {
    val uiState by taskViewModel.uiState.collectAsState()
    val focusSession by taskViewModel.focusSession.collectAsState()
    val filters = listOf(TaskFilter.ACTIVE, TaskFilter.COMPLETED)
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var pendingFocusTask by remember { mutableStateOf<TaskEntity?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val sortMenuWidth = 190.dp

    fun startFocusFor(task: TaskEntity) {
        val session = taskViewModel.startFocusQuest(task)
        TaskReminderScheduler.showFocusNotification(
            context = context,
            taskTitle = session.taskTitle,
            endAtMillis = session.endAtMillis
        )
        TaskReminderScheduler.scheduleFocusFinished(
            context = context,
            taskId = session.taskId,
            endAtMillis = session.endAtMillis
        )
        onStartFocus()
    }

    fun openOrStartFocusFor(task: TaskEntity) {
        val currentFocus = focusSession
        if (currentFocus?.taskId == task.id) {
            onStartFocus()
        } else {
            startFocusFor(task)
        }
    }

    pendingFocusTask?.let { task ->
        AlertDialog(
            onDismissRequest = { pendingFocusTask = null },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            shape = MaterialTheme.shapes.large,
            title = { Text(text = "Уже идёт фокус") },
            text = {
                Text(
                    text = "Сейчас запущен фокус на другой задаче. Можно продолжить его или сбросить текущий таймер и начать новый."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        taskViewModel.closeFocusQuest()
                        TaskReminderScheduler.cancelFocusNotification(context)
                        TaskReminderScheduler.cancelFocusFinished(context)
                        pendingFocusTask = null
                        startFocusFor(task)
                    }
                ) {
                    Text(text = "Сбросить и начать")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingFocusTask = null
                        onStartFocus()
                    }
                ) {
                    Text(text = "Оставить текущий")
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            SmallFloatingActionButton(
                onClick = onAddTask,
                shape = MaterialTheme.shapes.large,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.add_task),
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Text(
                text = stringResource(R.string.tasks_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            TabRow(
                selectedTabIndex = filters.indexOf(uiState.selectedFilter),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                filters.forEach { filter ->
                    Tab(
                        selected = uiState.selectedFilter == filter,
                        onClick = { taskViewModel.changeFilter(filter) },
                        text = { Text(text = filterLabel(filter), maxLines = 1) }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Сортировка",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(modifier = Modifier.width(sortMenuWidth)) {
                    OutlinedButton(
                        onClick = { sortMenuExpanded = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 40.dp),
                        shape = MaterialTheme.shapes.medium,
                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Sort,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = sortLabel(uiState.selectedSort),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false },
                        modifier = Modifier.width(sortMenuWidth),
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp,
                        shadowElevation = 6.dp,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        TaskSortOption.entries.forEach { sortOption ->
                            DropdownMenuItem(
                                text = { Text(text = sortLabel(sortOption)) },
                                onClick = {
                                    taskViewModel.changeSort(sortOption)
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.tasks.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emptyStateText(uiState.selectedFilter),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, top = 6.dp, end = 16.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = uiState.tasks,
                            key = { task -> task.id }
                        ) { task ->
                            TaskCard(
                                task = task,
                                onToggleComplete = {
                                    if (!task.isCompleted) {
                                        val earnedXp = GamificationManager.calculateTaskXp(task)
                                        TaskReminderScheduler.cancelTaskReminder(context, task.id)
                                        if (!task.xpAwarded) {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("+$earnedXp XP за задачу")
                                            }
                                        }
                                    } else {
                                        TaskReminderScheduler.scheduleTaskDeadline(
                                            context = context,
                                            taskId = task.id,
                                            dueDate = task.dueDate,
                                            scheduledStartTime = task.scheduledStartTime
                                        )
                                    }
                                    taskViewModel.toggleComplete(task)
                                },
                                onEditClick = { onEditTask(task.id) },
                                focusActionLabel = if (focusSession?.taskId == task.id) {
                                    "Вернуться в фокус"
                                } else {
                                    "Начать фокус"
                                },
                                onFocusClick = { selectedTask ->
                                    val currentFocus = focusSession
                                    if (currentFocus != null && currentFocus.taskId != selectedTask.id) {
                                        pendingFocusTask = selectedTask
                                    } else {
                                        openOrStartFocusFor(selectedTask)
                                    }
                                },
                                onDeleteClick = {
                                    TaskReminderScheduler.cancelTaskReminder(context, task.id)
                                    taskViewModel.deleteTask(task)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun sortLabel(sortOption: TaskSortOption): String {
    return when (sortOption) {
        TaskSortOption.SMART -> "Умный порядок"
        TaskSortOption.DEADLINE -> "Дата"
        TaskSortOption.PRIORITY -> "Приоритет"
        TaskSortOption.TIME -> "Время"
    }
}

@Composable
private fun filterLabel(filter: TaskFilter): String {
    return when (filter) {
        TaskFilter.ACTIVE -> stringResource(R.string.filter_active)
        TaskFilter.COMPLETED -> stringResource(R.string.filter_completed)
        TaskFilter.ALL -> stringResource(R.string.filter_all)
    }
}

private fun emptyStateText(selectedFilter: TaskFilter): String {
    if (selectedFilter == TaskFilter.COMPLETED) {
        return "Выполненных задач здесь пока нет."
    }
    return "Активных задач пока нет."
}
