package com.example.timequest.domain

import com.example.timequest.data.local.TaskEntity

data class DayBalance(
    val title: String,
    val description: String,
    val isWarning: Boolean
)

object DayBalanceAnalyzer {
    fun analyze(
        tasks: List<TaskEntity>,
        freeTimeSlots: List<FreeTimeSlot>
    ): DayBalance {
        val availableMinutes = freeTimeSlots.sumOf { slot ->
            ((slot.endTime - slot.startTime) / 60_000L).coerceAtLeast(0L)
        }.toInt()
        val taskMinutes = tasks.sumOf { task -> task.estimatedMinutes.coerceAtLeast(0) }
        val hardTasks = tasks.count { task -> task.difficulty == "hard" }

        return when {
            tasks.isEmpty() -> DayBalance(
                title = "План пока пустой",
                description = "Добавьте задачи и свободное время, чтобы оценить нагрузку дня",
                isWarning = false
            )
            availableMinutes == 0 -> DayBalance(
                title = "Нет свободного времени",
                description = "Добавьте хотя бы один промежуток для планирования",
                isWarning = true
            )
            taskMinutes > availableMinutes -> DayBalance(
                title = "День перегружен",
                description = "Задач на $taskMinutes мин, а доступно $availableMinutes мин. Часть задач лучше перенести",
                isWarning = true
            )
            hardTasks >= 3 -> DayBalance(
                title = "Высокая нагрузка",
                description = "Много сложных задач. Лучше ставить их на время высокой энергии",
                isWarning = true
            )
            availableMinutes - taskMinutes >= 60 -> DayBalance(
                title = "Есть свободное окно",
                description = "После задач останется примерно ${availableMinutes - taskMinutes} мин свободного времени",
                isWarning = false
            )
            else -> DayBalance(
                title = "Баланс хороший",
                description = "Задачи помещаются в день без явной перегрузки",
                isWarning = false
            )
        }
    }
}
