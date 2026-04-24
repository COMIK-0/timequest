package com.example.timequest.presentation.tasks

import com.example.timequest.data.local.TaskEntity
import com.example.timequest.domain.TaskSortOption

data class TaskListUiState(
    val tasks: List<TaskEntity> = emptyList(),
    val selectedFilter: TaskFilter = TaskFilter.ALL,
    val selectedSort: TaskSortOption = TaskSortOption.SMART,
    val isLoading: Boolean = true
)
