package com.example.timequest.presentation.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.timequest.R
import com.example.timequest.domain.TaskSortOption
import com.example.timequest.presentation.tasks.components.TaskCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TasksScreen(
    taskViewModel: TaskViewModel,
    onAddTask: () -> Unit,
    onEditTask: (Long) -> Unit
) {
    val uiState by taskViewModel.uiState.collectAsState()
    val filters = listOf(TaskFilter.ALL, TaskFilter.ACTIVE, TaskFilter.COMPLETED)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTask) {
                Text(text = stringResource(R.string.add_short))
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
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            ScrollableTabRow(
                selectedTabIndex = filters.indexOf(uiState.selectedFilter)
            ) {
                filters.forEach { filter ->
                    Tab(
                        selected = uiState.selectedFilter == filter,
                        onClick = { taskViewModel.changeFilter(filter) },
                        text = { Text(text = filterLabel(filter)) }
                    )
                }
            }

            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskSortOption.entries.forEach { sortOption ->
                    FilterChip(
                        selected = uiState.selectedSort == sortOption,
                        onClick = { taskViewModel.changeSort(sortOption) },
                        label = { Text(text = sortLabel(sortOption)) }
                    )
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
                            text = stringResource(R.string.no_tasks_yet),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = uiState.tasks,
                            key = { task -> task.id }
                        ) { task ->
                            TaskCard(
                                task = task,
                                onToggleComplete = { taskViewModel.toggleComplete(task) },
                                onEditClick = { onEditTask(task.id) },
                                onDeleteClick = { taskViewModel.deleteTask(task) }
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
        TaskSortOption.SMART -> stringResource(R.string.sort_smart)
        TaskSortOption.DEADLINE -> stringResource(R.string.sort_deadline)
        TaskSortOption.PRIORITY -> stringResource(R.string.sort_priority)
        TaskSortOption.TIME -> stringResource(R.string.sort_time)
    }
}

@Composable
private fun filterLabel(filter: TaskFilter): String {
    return when (filter) {
        TaskFilter.ALL -> stringResource(R.string.filter_all)
        TaskFilter.ACTIVE -> stringResource(R.string.filter_active)
        TaskFilter.COMPLETED -> stringResource(R.string.filter_completed)
    }
}
