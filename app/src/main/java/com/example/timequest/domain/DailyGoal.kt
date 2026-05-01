package com.example.timequest.domain

import com.example.timequest.data.local.TaskEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object DailyGoal {
    const val TARGET_TASKS = 3

    fun completedToday(tasks: List<TaskEntity>): Int {
        return tasks.count { task -> task.completedAt?.isToday() == true }
    }

    fun progress(tasks: List<TaskEntity>): Float {
        return completedToday(tasks).toFloat() / TARGET_TASKS
    }

    fun displayedCompleted(completedToday: Int): Int {
        return completedToday.coerceAtMost(TARGET_TASKS)
    }

    fun isCompleted(tasks: List<TaskEntity>): Boolean {
        return completedToday(tasks) >= TARGET_TASKS
    }

    private fun Long.isToday(): Boolean {
        return Instant.ofEpochMilli(this)
            .atZone(ZoneId.systemDefault())
            .toLocalDate() == LocalDate.now()
    }
}
