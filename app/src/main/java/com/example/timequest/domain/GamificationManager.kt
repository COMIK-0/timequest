package com.example.timequest.domain

import com.example.timequest.data.local.TaskEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class LevelInfo(
    val level: Int,
    val totalXp: Int,
    val currentLevelStartXp: Int,
    val nextLevelXp: Int,
    val progress: Float
)

data class Achievement(
    val title: String,
    val description: String,
    val isUnlocked: Boolean,
    val currentValue: Int,
    val targetValue: Int
)

object GamificationManager {
    fun calculateTaskXp(task: TaskEntity): Int {
        val baseXp = when (task.difficulty) {
            "hard" -> 40
            "medium" -> 20
            else -> 10
        }
        val priorityBonus = if (task.priority == "high") 10 else 0
        val timingBonus = if (isCompletedOnTime(task)) 5 else 0
        val planBonus = if (task.scheduledStartTime != null && task.scheduledEndTime != null) 5 else 0
        return baseXp + priorityBonus + timingBonus + planBonus
    }

    fun totalXp(tasks: List<TaskEntity>): Int {
        return tasks
            .filter { task -> task.isCompleted && task.xpAwarded }
            .sumOf(::calculateTaskXp)
    }

    fun levelInfo(totalXp: Int): LevelInfo {
        var level = 1
        var currentStart = 0
        var nextThreshold = 100
        var increment = 150

        while (totalXp >= nextThreshold) {
            level++
            currentStart = nextThreshold
            nextThreshold += increment
            increment += 50
        }

        val progress = if (nextThreshold == currentStart) {
            0f
        } else {
            (totalXp - currentStart).toFloat() / (nextThreshold - currentStart)
        }

        return LevelInfo(
            level = level,
            totalXp = totalXp,
            currentLevelStartXp = currentStart,
            nextLevelXp = nextThreshold,
            progress = progress.coerceIn(0f, 1f)
        )
    }

    fun currentStreak(tasks: List<TaskEntity>): Int {
        val activeDays = tasks
            .mapNotNull { task -> task.completedAt?.toLocalDate() }
            .toSet()

        if (activeDays.isEmpty()) return 0

        var date = LocalDate.now()
        if (date !in activeDays) {
            date = date.minusDays(1)
        }

        var streak = 0
        while (date in activeDays) {
            streak++
            date = date.minusDays(1)
        }
        return streak
    }

    fun bestCompletionDay(tasks: List<TaskEntity>): Pair<LocalDate, Int>? {
        return tasks
            .mapNotNull { task -> task.completedAt?.toLocalDate() }
            .groupingBy { date -> date }
            .eachCount()
            .maxByOrNull { it.value }
            ?.toPair()
    }

    fun achievements(tasks: List<TaskEntity>): List<Achievement> {
        val completedCount = tasks.count { it.isCompleted }
        val totalXp = totalXp(tasks)
        val streak = currentStreak(tasks)
        val level = levelInfo(totalXp).level
        val hasSavedPlan = tasks.any { task ->
            task.scheduledStartTime != null && task.scheduledEndTime != null
        }
        val plannedCount = tasks.count { task ->
            task.scheduledStartTime != null && task.scheduledEndTime != null
        }
        val hardCompleted = tasks.count { it.isCompleted && it.difficulty == "hard" }
        val highPriorityCompleted = tasks.count { it.isCompleted && it.priority == "high" }
        val onTimeCompleted = tasks.count { it.isCompleted && isCompletedOnTime(it) }
        val activeCategories = tasks
            .filter { it.isCompleted }
            .map { it.category }
            .filter { it.isNotBlank() }
            .toSet()

        return listOf(
            achievement("Первый шаг", "Выполнить первую задачу", completedCount, 1),
            achievement("Разгон", "Закрыть 3 задачи и почувствовать темп", completedCount, 3),
            achievement("Десять побед", "Выполнить 10 задач", completedCount, 10),
            achievement("Марафон задач", "Выполнить 25 задач", completedCount, 25),
            achievement("Сотня XP", "Набрать 100 XP", totalXp, 100),
            achievement("Новый уровень", "Достичь 3 уровня", level, 3),
            achievement("Планировщик", "Сохранить хотя бы один план на день", if (hasSavedPlan) 1 else 0, 1),
            achievement("Архитектор дня", "Запланировать 5 задач по времени", plannedCount, 5),
            achievement("Сложный режим", "Выполнить 5 сложных задач", hardCompleted, 5),
            achievement("Главное вперед", "Закрыть 5 задач с высоким приоритетом", highPriorityCompleted, 5),
            achievement("В срок", "Выполнить 5 задач до дедлайна", onTimeCompleted, 5),
            achievement("Баланс сфер", "Выполнить задачи в 4 разных категориях", activeCategories.size, 4),
            achievement("Серия 3 дня", "Выполнять задачи 3 дня подряд", streak, 3),
            achievement("Неделя дисциплины", "Выполнять задачи 7 дней подряд", streak, 7)
        )
    }

    private fun achievement(
        title: String,
        description: String,
        currentValue: Int,
        targetValue: Int
    ): Achievement {
        return Achievement(
            title = title,
            description = description,
            isUnlocked = currentValue >= targetValue,
            currentValue = currentValue.coerceIn(0, targetValue),
            targetValue = targetValue
        )
    }

    private fun isCompletedOnTime(task: TaskEntity): Boolean {
        val completedAt = task.completedAt ?: return false
        val scheduledEndTime = task.scheduledEndTime
        if (scheduledEndTime != null) {
            return completedAt <= scheduledEndTime
        }

        val dueDate = task.dueDate ?: return false
        val endOfDay = dueDate.toLocalDate()
            .plusDays(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli() - 1
        return completedAt <= endOfDay
    }

    private fun Long.toLocalDate(): LocalDate {
        return Instant.ofEpochMilli(this)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }
}
