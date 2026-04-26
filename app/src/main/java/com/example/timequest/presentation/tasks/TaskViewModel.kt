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

data class FocusSessionUiState(
    val taskId: Long,
    val taskTitle: String,
    val totalSeconds: Int,
    val remainingSeconds: Int,
    val isRunning: Boolean,
    val endAtMillis: Long?
)

@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModel(
    private val repository: TaskRepository
) : ViewModel() {

    private val selectedFilter = MutableStateFlow(TaskFilter.ACTIVE)
    private val selectedSort = MutableStateFlow(TaskSortOption.SMART)
    private val _focusSession = MutableStateFlow<FocusSessionUiState?>(null)

    val focusSession: StateFlow<FocusSessionUiState?> = _focusSession

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
            initialValue = TaskListUiState(selectedFilter = TaskFilter.ACTIVE)
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

    fun updateTasks(tasks: List<TaskEntity>, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateTasks(tasks)
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
                xpAwarded = task.xpAwarded || !task.isCompleted,
                completedAt = if (!task.isCompleted) System.currentTimeMillis() else null
            )
            repository.updateTask(updatedTask)
        }
    }

    fun startFocusQuest(task: TaskEntity): FocusSessionUiState {
        val current = _focusSession.value
        if (current?.taskId == task.id) {
            val remaining = current.remainingSeconds(System.currentTimeMillis())
            if (remaining > 0) {
                val updated = current.copy(
                    taskTitle = task.title,
                    remainingSeconds = remaining
                )
                _focusSession.value = updated
                return updated
            }
        }

        val totalSeconds = task.estimatedMinutes.coerceAtLeast(1) * 60
        val session = FocusSessionUiState(
            taskId = task.id,
            taskTitle = task.title,
            totalSeconds = totalSeconds,
            remainingSeconds = totalSeconds,
            isRunning = true,
            endAtMillis = System.currentTimeMillis() + totalSeconds * 1_000L
        )
        _focusSession.value = session
        return session
    }

    fun pauseFocusQuest(nowMillis: Long = System.currentTimeMillis()): FocusSessionUiState? {
        val current = _focusSession.value ?: return null
        val remaining = current.remainingSeconds(nowMillis)
        val updated = current.copy(
            remainingSeconds = remaining,
            isRunning = false,
            endAtMillis = null
        )
        _focusSession.value = updated
        return updated
    }

    fun resumeFocusQuest(nowMillis: Long = System.currentTimeMillis()): FocusSessionUiState? {
        val current = _focusSession.value ?: return null
        val updated = current.copy(
            isRunning = true,
            endAtMillis = nowMillis + current.remainingSeconds.coerceAtLeast(0) * 1_000L
        )
        _focusSession.value = updated
        return updated
    }

    fun resetFocusQuest(): FocusSessionUiState? {
        val current = _focusSession.value ?: return null
        val updated = current.copy(
            remainingSeconds = current.totalSeconds,
            isRunning = false,
            endAtMillis = null
        )
        _focusSession.value = updated
        return updated
    }

    fun closeFocusQuest() {
        _focusSession.value = null
    }

    fun completeFocusQuest(onSaved: () -> Unit = {}) {
        val current = _focusSession.value ?: return
        viewModelScope.launch {
            val task = repository.getTaskById(current.taskId) ?: return@launch
            if (!task.isCompleted) {
                repository.updateTask(
                    task.copy(
                        isCompleted = true,
                        xpAwarded = true,
                        completedAt = System.currentTimeMillis()
                    )
                )
            }
            _focusSession.value = null
            onSaved()
        }
    }

    fun focusRemainingSeconds(nowMillis: Long = System.currentTimeMillis()): Int {
        return _focusSession.value?.remainingSeconds(nowMillis) ?: 0
    }

    suspend fun getTaskById(taskId: Long): TaskEntity? {
        return repository.getTaskById(taskId)
    }

    private fun FocusSessionUiState.remainingSeconds(nowMillis: Long): Int {
        return if (isRunning && endAtMillis != null) {
            ((endAtMillis - nowMillis) / 1_000L).toInt().coerceAtLeast(0)
        } else {
            remainingSeconds.coerceAtLeast(0)
        }
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
