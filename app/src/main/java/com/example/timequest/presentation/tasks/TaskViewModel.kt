package com.example.timequest.presentation.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.timequest.data.local.TaskEntity
import com.example.timequest.data.repository.TaskRepository
import com.example.timequest.domain.TaskPrioritizer
import com.example.timequest.domain.TaskSortOption
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModel(
    private val repository: TaskRepository
) : ViewModel() {

    private val selectedFilter = MutableStateFlow(TaskFilter.ALL)
    private val selectedSort = MutableStateFlow(TaskSortOption.SMART)

    val allTasks: StateFlow<List<TaskEntity>> = repository.getAllTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val uiState: StateFlow<TaskListUiState> = selectedFilter
        .flatMapLatest { filter ->
            val tasksFlow = when (filter) {
                TaskFilter.ALL -> repository.getAllTasks()
                TaskFilter.ACTIVE -> repository.getActiveTasks()
                TaskFilter.COMPLETED -> repository.getCompletedTasks()
            }
            combine(tasksFlow, selectedFilter, selectedSort) { tasks, currentFilter, currentSort ->
                TaskListUiState(
                    tasks = TaskPrioritizer.sort(tasks, currentSort),
                    selectedFilter = currentFilter,
                    selectedSort = currentSort,
                    isLoading = false
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TaskListUiState()
        )

    fun changeFilter(filter: TaskFilter) {
        selectedFilter.value = filter
    }

    fun changeSort(sortOption: TaskSortOption) {
        selectedSort.value = sortOption
    }

    fun addTask(task: TaskEntity, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val taskId = repository.insertTask(task)
            onSaved(taskId)
        }
    }

    fun updateTask(task: TaskEntity, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateTask(task)
            onSaved()
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun toggleComplete(task: TaskEntity) {
        viewModelScope.launch {
            val updatedTask = task.copy(
                isCompleted = !task.isCompleted,
                completedAt = if (!task.isCompleted) System.currentTimeMillis() else null
            )
            repository.updateTask(updatedTask)
        }
    }

    suspend fun getTaskById(taskId: Long): TaskEntity? {
        return repository.getTaskById(taskId)
    }

    class Factory(
        private val repository: TaskRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
                return TaskViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
