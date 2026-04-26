package com.example.timequest.domain

import com.example.timequest.data.local.TaskEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object TaskPrioritizer {
    fun sortSmart(tasks: List<TaskEntity>): List<TaskEntity> {
        return tasks.sortedWith(
            // 1. Сначала показываем задачи на сегодня, потому что они самые важные для плана дня.
            compareBy<TaskEntity> { task -> if (task.dueDate?.isToday() == true) 0 else 1 }
                // 2. Затем идут задачи с ближайшей датой. Задачи без даты уходят ниже.
                .thenBy { task -> task.dueDate ?: Long.MAX_VALUE }
                // 3. При равной дате выше ставим более высокий приоритет.
                .thenByDescending { task -> priorityWeight(task.priority) }
                // 4. Затем учитываем сложность: сложные задачи лучше увидеть раньше.
                .thenByDescending { task -> difficultyWeight(task.difficulty) }
                // 5. Короткие задачи легче быстро закрыть, поэтому они идут выше.
                .thenBy { task -> if (task.estimatedMinutes > 0) task.estimatedMinutes else Int.MAX_VALUE }
                // 6. Если всё одинаково, более старые задачи не теряются внизу списка.
                .thenBy { task -> task.createdAt }
        )
    }

    fun sortByDeadline(tasks: List<TaskEntity>): List<TaskEntity> {
        return tasks.sortedWith(
            compareBy<TaskEntity> { task -> task.dueDate ?: Long.MAX_VALUE }
                .thenBy { task -> task.createdAt }
        )
    }

    fun sortByPriority(tasks: List<TaskEntity>): List<TaskEntity> {
        return tasks.sortedWith(
            compareByDescending<TaskEntity> { task -> priorityWeight(task.priority) }
                .thenBy { task -> task.dueDate ?: Long.MAX_VALUE }
                .thenBy { task -> task.createdAt }
        )
    }

    fun sortByEstimatedTime(tasks: List<TaskEntity>): List<TaskEntity> {
        return tasks.sortedWith(
            compareBy<TaskEntity> { task -> if (task.estimatedMinutes > 0) task.estimatedMinutes else Int.MAX_VALUE }
                .thenBy { task -> task.dueDate ?: Long.MAX_VALUE }
                .thenBy { task -> task.createdAt }
        )
    }

    fun sort(tasks: List<TaskEntity>, option: TaskSortOption): List<TaskEntity> {
        return when (option) {
            TaskSortOption.SMART -> sortSmart(tasks)
            TaskSortOption.DEADLINE -> sortByDeadline(tasks)
            TaskSortOption.PRIORITY -> sortByPriority(tasks)
            TaskSortOption.TIME -> sortByEstimatedTime(tasks)
        }
    }

    private fun priorityWeight(priority: String): Int {
        return when (priority) {
            "high" -> 3
            "medium" -> 2
            else -> 1
        }
    }

    private fun difficultyWeight(difficulty: String): Int {
        return when (difficulty) {
            "hard" -> 3
            "medium" -> 2
            else -> 1
        }
    }

    private fun Long.isToday(): Boolean {
        return Instant.ofEpochMilli(this)
            .atZone(ZoneId.systemDefault())
            .toLocalDate() == LocalDate.now()
    }
}
