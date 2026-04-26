package com.example.timequest.domain

import com.example.timequest.data.local.TaskEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DayScheduleResult(
    val scheduledTasks: List<TaskEntity>,
    val unscheduledTasks: List<TaskEntity>
)

class DayTaskScheduler(
    private val taskPrioritizer: TaskPrioritizer = TaskPrioritizer
) {
    fun schedule(
        tasks: List<TaskEntity>,
        selectedDate: LocalDate,
        freeTimeSlots: List<FreeTimeSlot>,
        energyLevel: EnergyLevel = EnergyLevel.MEDIUM
    ): DayScheduleResult {
        val dayTasks = tasks
            .filter { task -> task.dueDate?.toLocalDate() == selectedDate }
            .filter { task -> !task.isCompleted }

        return scheduleInOrder(
            tasks = sortForEnergy(taskPrioritizer.sortSmart(dayTasks), energyLevel),
            freeTimeSlots = freeTimeSlots
        )
    }

    fun scheduleInOrder(
        tasks: List<TaskEntity>,
        freeTimeSlots: List<FreeTimeSlot>
    ): DayScheduleResult {
        val slots = freeTimeSlots.sortedBy { it.startTime }
        val scheduledTasks = mutableListOf<TaskEntity>()
        val unscheduledTasks = mutableListOf<TaskEntity>()
        val slotCursor = slots.associateWith { it.startTime }.toMutableMap()

        tasks.forEach { task ->
            val durationMillis = task.estimatedMinutes.toLong() * MILLIS_IN_MINUTE
            if (durationMillis <= 0L) {
                unscheduledTasks.add(task.copy(scheduledStartTime = null, scheduledEndTime = null))
                return@forEach
            }

            val slot = slots.firstOrNull { freeSlot ->
                val startTime = slotCursor[freeSlot] ?: freeSlot.startTime
                startTime + durationMillis <= freeSlot.endTime
            }

            if (slot == null) {
                unscheduledTasks.add(task.copy(scheduledStartTime = null, scheduledEndTime = null))
            } else {
                val startTime = slotCursor[slot] ?: slot.startTime
                val endTime = startTime + durationMillis
                slotCursor[slot] = endTime
                scheduledTasks.add(
                    task.copy(
                        scheduledStartTime = startTime,
                        scheduledEndTime = endTime
                    )
                )
            }
        }

        return DayScheduleResult(
            scheduledTasks = scheduledTasks,
            unscheduledTasks = unscheduledTasks
        )
    }

    private fun Long.toLocalDate(): LocalDate {
        return Instant.ofEpochMilli(this)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    private fun sortForEnergy(tasks: List<TaskEntity>, energyLevel: EnergyLevel): List<TaskEntity> {
        return when (energyLevel) {
            EnergyLevel.HIGH -> tasks.sortedWith(
                compareByDescending<TaskEntity> { difficultyWeight(it.difficulty) }
                    .thenByDescending { priorityWeight(it.priority) }
                    .thenBy { it.estimatedMinutes }
            )
            EnergyLevel.LOW -> tasks.sortedWith(
                compareBy<TaskEntity> { difficultyWeight(it.difficulty) }
                    .thenBy { it.estimatedMinutes }
                    .thenByDescending { priorityWeight(it.priority) }
            )
            EnergyLevel.MEDIUM -> tasks
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

    private companion object {
        const val MILLIS_IN_MINUTE = 60_000L
    }
}
