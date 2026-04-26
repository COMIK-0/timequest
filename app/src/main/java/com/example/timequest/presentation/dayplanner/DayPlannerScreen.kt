package com.example.timequest.presentation.dayplanner

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.timequest.data.local.TaskEntity
import com.example.timequest.domain.DayBalanceAnalyzer
import com.example.timequest.domain.DayTaskScheduler
import com.example.timequest.domain.EnergyLevel
import com.example.timequest.domain.FreeTimeSlot
import com.example.timequest.notifications.TaskReminderScheduler
import com.example.timequest.presentation.components.AppCard
import com.example.timequest.presentation.components.MetricCard
import com.example.timequest.presentation.components.difficultyLabel
import com.example.timequest.presentation.components.priorityColor
import com.example.timequest.presentation.components.priorityLabel
import com.example.timequest.presentation.tasks.TaskViewModel
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayPlannerScreen(
    taskViewModel: TaskViewModel,
    initialDateMillis: Long?,
    onNavigateBack: () -> Unit,
    onPlanSaved: () -> Unit
) {
    val context = LocalContext.current
    val tasks by taskViewModel.allTasks.collectAsState()
    val scheduler = remember { DayTaskScheduler() }
    val freeTimeSlots = remember { mutableStateListOf<FreeTimeSlot>() }

    var selectedDateMillis by rememberSaveable { mutableStateOf(initialDateMillis ?: todayStartMillis()) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var timePickerTarget by remember { mutableStateOf<TimePickerTarget?>(null) }
    var scheduledTasks by remember { mutableStateOf<List<TaskEntity>>(emptyList()) }
    var unscheduledTasks by remember { mutableStateOf<List<TaskEntity>>(emptyList()) }
    var message by remember { mutableStateOf<String?>(null) }
    var warning by remember { mutableStateOf<String?>(null) }
    var energyLevel by rememberSaveable { mutableStateOf(EnergyLevel.MEDIUM) }

    val selectedDate = selectedDateMillis.toLocalDate()
    val activeTasksForDate = tasks.filter { task ->
        !task.isCompleted && task.dueDate?.toLocalDate() == selectedDate
    }
    val existingPlannedTasksForDate = activeTasksForDate.filter { task ->
        task.scheduledStartTime != null && task.scheduledEndTime != null
    }
    val availableMinutes = freeTimeSlots.sumOf { slot ->
        ((slot.endTime - slot.startTime) / 60_000L).coerceAtLeast(0L)
    }
    val taskMinutes = activeTasksForDate.sumOf { task -> task.estimatedMinutes.coerceAtLeast(0) }
    val dayBalance = DayBalanceAnalyzer.analyze(activeTasksForDate, freeTimeSlots)

    LaunchedEffect(selectedDateMillis, tasks) {
        val savedPlan = activeTasksForDate
            .filter { task -> task.scheduledStartTime != null && task.scheduledEndTime != null }
            .sortedBy { task -> task.scheduledStartTime }

        scheduledTasks = savedPlan
        unscheduledTasks = emptyList()
        warning = null
        freeTimeSlots.clear()

        if (savedPlan.isNotEmpty()) {
            freeTimeSlots.add(inferFreeTimeSlot(savedPlan))
            message = "Загружен сохранённый план. Можно изменить промежутки и распределить задачи заново."
        } else {
            message = null
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Планировщик дня",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Event,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Дата планирования",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatDate(selectedDateMillis),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(text = "Выбрать")
                    }
                }
            }

            AppCard {
                SectionTitle(title = "Свободное время")
                if (freeTimeSlots.isEmpty()) {
                    Text(
                        text = "Добавьте один или несколько промежутков для планирования.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                freeTimeSlots.forEachIndexed { index, slot ->
                    FreeSlotRow(
                        slot = slot,
                        onPickStart = { timePickerTarget = TimePickerTarget(index, true) },
                        onPickEnd = { timePickerTarget = TimePickerTarget(index, false) },
                        onDelete = {
                            freeTimeSlots.removeAt(index)
                            scheduledTasks = emptyList()
                            unscheduledTasks = emptyList()
                        }
                    )
                }
                OutlinedButton(
                    onClick = {
                        freeTimeSlots.add(defaultSlot(selectedDate))
                        scheduledTasks = emptyList()
                        unscheduledTasks = emptyList()
                    },
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(text = "Добавить промежуток")
                }
            }

            AppCard {
                SectionTitle(title = "Распределение")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Задач на дату",
                        value = activeTasksForDate.size.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Свободно",
                        value = "$availableMinutes мин",
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = "Задач на $taskMinutes мин",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (availableMinutes in 1 until taskMinutes.toLong()) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Text(
                    text = "Энергия сегодня",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EnergyLevel.entries.forEach { level ->
                        FilterChip(
                            selected = energyLevel == level,
                            onClick = { energyLevel = level },
                            label = {
                                Text(
                                    text = energyLevelLabel(level),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                StatusSurface(
                    text = "${cleanBalanceText(dayBalance.title)}. ${cleanBalanceText(dayBalance.description)}",
                    isWarning = dayBalance.isWarning
                )
                Button(
                    onClick = {
                        val validationMessage = validatePlanInput(activeTasksForDate, freeTimeSlots)
                        if (validationMessage != null) {
                            message = validationMessage
                            warning = null
                            scheduledTasks = emptyList()
                            unscheduledTasks = emptyList()
                        } else {
                            val result = scheduler.schedule(
                                tasks = tasks,
                                selectedDate = selectedDate,
                                freeTimeSlots = freeTimeSlots,
                                energyLevel = energyLevel
                            )
                            scheduledTasks = result.scheduledTasks
                            unscheduledTasks = result.unscheduledTasks
                            warning = buildWarning(result.unscheduledTasks)
                            message = if (result.scheduledTasks.isEmpty()) {
                                "Задачи не поместились в выбранное свободное время."
                            } else {
                                "План построен. Проверьте порядок и сохраните."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(text = "Распределить задачи")
                }
                if (isDebugBuild(context)) {
                    OutlinedButton(
                        onClick = {
                            TaskReminderScheduler.scheduleDebugNotification(context)
                            message = "Тестовое уведомление запланировано через 30 секунд."
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(text = "Тест уведомления через 30 секунд")
                    }
                }
                if (existingPlannedTasksForDate.isNotEmpty() || scheduledTasks.isNotEmpty()) {
                    OutlinedButton(
                        onClick = {
                            val tasksForDate = tasks.filter { task ->
                                task.dueDate?.toLocalDate() == selectedDate
                            }
                            val clearedTasks = tasksForDate.map { task ->
                                task.copy(scheduledStartTime = null, scheduledEndTime = null)
                            }
                            taskViewModel.updateTasks(clearedTasks) {
                                tasksForDate.forEach { task ->
                                    TaskReminderScheduler.cancelTaskReminder(context, task.id)
                                    if (!task.isCompleted) {
                                        TaskReminderScheduler.scheduleTaskDeadline(
                                            context = context,
                                            taskId = task.id,
                                            dueDate = task.dueDate,
                                            scheduledStartTime = null
                                        )
                                    }
                                }
                                scheduledTasks = emptyList()
                                unscheduledTasks = emptyList()
                                freeTimeSlots.clear()
                                warning = null
                                message = "План на день очищен."
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(text = "Очистить план")
                    }
                }
            }

            message?.let {
                StatusSurface(text = it, isWarning = warning == null && scheduledTasks.isEmpty())
            }
            warning?.let {
                StatusSurface(text = it, isWarning = true)
            }

            if (scheduledTasks.isNotEmpty()) {
                AppCard {
                    SectionTitle(title = "Расписание")
                    scheduledTasks.forEachIndexed { index, task ->
                        TimelineTaskRow(
                            task = task,
                            canMoveUp = index > 0,
                            canMoveDown = index < scheduledTasks.lastIndex,
                            onMoveUp = {
                                val reordered = scheduledTasks.toMutableList().apply {
                                    add(index - 1, removeAt(index))
                                }
                                val result = scheduler.scheduleInOrder(reordered, freeTimeSlots)
                                scheduledTasks = result.scheduledTasks
                                unscheduledTasks = mergeUnscheduled(result.unscheduledTasks, unscheduledTasks)
                                warning = if (result.unscheduledTasks.isNotEmpty()) {
                                    "После перестановки часть задач не помещается в свободное время."
                                } else {
                                    null
                                }
                            },
                            onMoveDown = {
                                val reordered = scheduledTasks.toMutableList().apply {
                                    add(index + 1, removeAt(index))
                                }
                                val result = scheduler.scheduleInOrder(reordered, freeTimeSlots)
                                scheduledTasks = result.scheduledTasks
                                unscheduledTasks = mergeUnscheduled(result.unscheduledTasks, unscheduledTasks)
                                warning = if (result.unscheduledTasks.isNotEmpty()) {
                                    "После перестановки часть задач не помещается в свободное время."
                                } else {
                                    null
                                }
                            }
                        )
                    }
                    Button(
                        onClick = {
                            val scheduledIds = scheduledTasks.map { it.id }.toSet()
                            val tasksForDate = tasks.filter { task ->
                                task.dueDate?.toLocalDate() == selectedDate
                            }
                            val clearedTasks = tasksForDate
                                .filter { it.id !in scheduledIds }
                                .map { it.copy(scheduledStartTime = null, scheduledEndTime = null) }
                            val tasksToSave = scheduledTasks + clearedTasks

                            taskViewModel.updateTasks(tasksToSave) {
                                tasksForDate.forEach { task ->
                                    TaskReminderScheduler.cancelTaskReminder(context, task.id)
                                }
                                tasksToSave.filter { task -> !task.isCompleted }.forEach { task ->
                                    TaskReminderScheduler.scheduleTaskDeadline(
                                        context = context,
                                        taskId = task.id,
                                        dueDate = task.dueDate,
                                        scheduledStartTime = task.scheduledStartTime
                                    )
                                }
                                onPlanSaved()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(text = "Сохранить план")
                    }
                }
            }

            if (unscheduledTasks.isNotEmpty()) {
                AppCard {
                    SectionTitle(title = "Не вошли в день")
                    Text(
                        text = "Этим задачам не хватило свободного времени.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    unscheduledTasks.distinctBy { it.id }.forEach { task ->
                        Text(
                            text = "${task.title} • ${task.estimatedMinutes} мин",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedDateMillis = datePickerState.selectedDateMillis ?: selectedDateMillis
                        showDatePicker = false
                    }
                ) {
                    Text(text = "Готово")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(text = "Отмена")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    timePickerTarget?.let { target ->
        val slot = freeTimeSlots.getOrNull(target.index)
        if (slot != null) {
            val initialTime = if (target.isStart) slot.startTime else slot.endTime
            val localTime = initialTime.toLocalTime()
            val timePickerState = rememberTimePickerState(
                initialHour = localTime.hour,
                initialMinute = localTime.minute,
                is24Hour = true
            )

            AlertDialog(
                onDismissRequest = { timePickerTarget = null },
                title = { Text(text = if (target.isStart) "Время начала" else "Время окончания") },
                text = { TimePicker(state = timePickerState) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val newTime = selectedDate.atTime(timePickerState.hour, timePickerState.minute)
                                .atZone(ZoneId.systemDefault())
                                .toInstant()
                                .toEpochMilli()
                            freeTimeSlots[target.index] = if (target.isStart) {
                                slot.copy(startTime = newTime)
                            } else {
                                slot.copy(endTime = newTime)
                            }
                            scheduledTasks = emptyList()
                            unscheduledTasks = emptyList()
                            timePickerTarget = null
                        }
                    ) {
                        Text(text = "Готово")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { timePickerTarget = null }) {
                        Text(text = "Отмена")
                    }
                }
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun FreeSlotRow(
    slot: FreeTimeSlot,
    onPickStart: () -> Unit,
    onPickEnd: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onPickStart,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(text = formatTime(slot.startTime))
            }
            Text(
                text = "-",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = onPickEnd,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(text = formatTime(slot.endTime))
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Удалить промежуток"
                )
            }
        }
    }
}

@Composable
private fun TimelineTaskRow(
    task: TaskEntity,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.width(64.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = formatTime(task.scheduledStartTime),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = formatTime(task.scheduledEndTime),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(76.dp)
                .background(priorityColor(task.priority), MaterialTheme.shapes.small)
        )
        Surface(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${task.category.ifBlank { "Без категории" }} • ${priorityLabel(task.priority).lowercase()} • ${difficultyLabel(task.difficulty).lowercase()} • ${task.estimatedMinutes} мин",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = onMoveUp,
                        enabled = canMoveUp,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(text = "Вверх")
                    }
                    TextButton(
                        onClick = onMoveDown,
                        enabled = canMoveDown,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(text = "Вниз")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusSurface(text: String, isWarning: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isWarning) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
        },
        contentColor = if (isWarning) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        },
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private data class TimePickerTarget(
    val index: Int,
    val isStart: Boolean
)

private fun validatePlanInput(
    tasks: List<TaskEntity>,
    slots: List<FreeTimeSlot>
): String? {
    if (tasks.isEmpty()) return "На выбранную дату нет активных задач."
    if (slots.isEmpty()) return "Добавьте хотя бы один промежуток свободного времени."

    val zeroTask = tasks.firstOrNull { it.estimatedMinutes <= 0 }
    if (zeroTask != null) return "У задачи «${zeroTask.title}» время выполнения 0 минут."

    val sortedSlots = slots.sortedBy { it.startTime }
    sortedSlots.forEach { slot ->
        if (slot.startTime >= slot.endTime) return "Начало промежутка должно быть раньше окончания."
        if (slot.endTime - slot.startTime < 5 * 60_000L) return "Свободный промежуток должен быть не меньше 5 минут."
    }

    sortedSlots.zipWithNext().forEach { (current, next) ->
        if (current.endTime > next.startTime) return "Свободные промежутки не должны пересекаться."
    }

    return null
}

private fun buildWarning(tasks: List<TaskEntity>): String? {
    if (tasks.isEmpty()) return null
    return "Часть задач не поместилась в доступное время. Проверьте список ниже."
}

private fun mergeUnscheduled(
    fresh: List<TaskEntity>,
    previous: List<TaskEntity>
): List<TaskEntity> {
    return (fresh + previous).distinctBy { it.id }
}

private fun defaultSlot(date: LocalDate): FreeTimeSlot {
    val start = date.atTime(16, 0)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
    val end = date.atTime(20, 0)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
    return FreeTimeSlot(start, end)
}

private fun inferFreeTimeSlot(tasks: List<TaskEntity>): FreeTimeSlot {
    val start = tasks.mapNotNull { it.scheduledStartTime }.minOrNull() ?: todayStartMillis()
    val end = tasks.mapNotNull { it.scheduledEndTime }.maxOrNull() ?: (start + 60 * 60_000L)
    return FreeTimeSlot(start, end)
}

private fun isDebugBuild(context: android.content.Context): Boolean {
    return context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
}

private fun cleanBalanceText(text: String): String {
    return text
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(timestamp))
}

private fun formatTime(timestamp: Long?): String {
    if (timestamp == null) return "--:--"
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
}

private fun energyLevelLabel(level: EnergyLevel): String {
    return when (level) {
        EnergyLevel.LOW -> "Низкая"
        EnergyLevel.MEDIUM -> "Средняя"
        EnergyLevel.HIGH -> "Высокая"
    }
}

private fun todayStartMillis(): Long {
    return LocalDate.now()
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}

private fun Long.toLocalDate(): LocalDate {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}

private fun Long.toLocalTime(): LocalTime {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalTime()
}
