package com.example.timequest.domain

import com.example.timequest.data.local.TaskEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object TaskPrioritizer {
    fun sortSmart(tasks: List<TaskEntity>): List<TaskEntity> {
        return tasks.sortedWith(
            compareBy<TaskEntity> { task -> if (task.dueDate?.isToday() == true) 0 else 1 }
                .thenBy { task -> task.dueDate ?: Long.MAX_VALUE }
                .thenByDescending { task -> priorityWeight(task.priority) }
                .thenByDescending { task -> difficultyWeight(task.difficulty) }
                .thenBy { task -> if (task.estimatedMinutes > 0) task.estimatedMinutes else Int.MAX_VALUE }
                .thenByDescending { task -> task.createdAt }
        )
    }

    fun sortByDeadline(tasks: List<TaskEntity>): List<TaskEntity> {
        return tasks.sortedWith(
            compareBy<TaskEntity> { task -> task.dueDate ?: Long.MAX_VALUE }
                .thenByDescending { task -> task.createdAt }
        )
    }

    fun sortByPriority(tasks: List<TaskEntity>): List<TaskEntity> {
        return tasks.sortedWith(
            compareByDescending<TaskEntity> { task -> priorityWeight(task.priority) }
                .thenBy { task -> task.dueDate ?: Long.MAX_VALUE }
                .thenByDescending { task -> task.createdAt }
        )
    }

    fun sortByEstimatedTime(tasks: List<TaskEntity>): List<TaskEntity> {
        return tasks.sortedWith(
            compareBy<TaskEntity> { task -> if (task.estimatedMinutes > 0) task.estimatedMinutes else Int.MAX_VALUE }
                .thenBy { task -> task.dueDate ?: Long.MAX_VALUE }
                .thenByDescending { task -> task.createdAt }
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
