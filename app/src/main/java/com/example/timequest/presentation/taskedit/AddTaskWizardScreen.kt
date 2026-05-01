package com.example.timequest.presentation.taskedit

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.timequest.R
import com.example.timequest.data.local.TaskEntity
import com.example.timequest.domain.GamificationManager
import com.example.timequest.navigation.NavigationGuardHandler
import com.example.timequest.notifications.TaskReminderScheduler
import com.example.timequest.presentation.components.AppCard
import com.example.timequest.presentation.tasks.TaskViewModel
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTaskWizardScreen(
    taskId: Long?,
    initialDueDateMillis: Long?,
    taskViewModel: TaskViewModel,
    onNavigateBack: () -> Unit,
    onNavigationGuardChanged: (NavigationGuardHandler?) -> Unit,
    onTaskSaved: () -> Unit
) {
    val isEditMode = taskId != null
    val defaultCategory = stringResource(R.string.category_study)
    val context = LocalContext.current

    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(defaultCategory) }
    var priority by rememberSaveable { mutableStateOf("medium") }
    var difficulty by rememberSaveable { mutableStateOf("medium") }
    var estimatedMinutes by rememberSaveable { mutableStateOf(30) }
    var dueDate by rememberSaveable { mutableStateOf<Long?>(initialDueDateMillis ?: todayStartMillis()) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var titleError by rememberSaveable { mutableStateOf(false) }
    var isSaving by rememberSaveable { mutableStateOf(false) }
    var hasUnsavedChanges by rememberSaveable { mutableStateOf(false) }
    var existingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var pendingExitAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun markChanged() {
        hasUnsavedChanges = true
    }

    fun requestExit(action: () -> Unit) {
        if (hasUnsavedChanges && !isSaving) {
            pendingExitAction = action
        } else {
            action()
        }
    }

    LaunchedEffect(taskId, defaultCategory) {
        if (taskId != null) {
            val task = taskViewModel.getTaskById(taskId)
            existingTask = task
            if (task != null) {
                title = task.title
                description = task.description
                category = task.category.ifBlank { defaultCategory }
                priority = task.priority
                difficulty = task.difficulty
                estimatedMinutes = task.estimatedMinutes
                dueDate = task.dueDate
            }
        } else if (category.isBlank()) {
            category = defaultCategory
        }
    }

    BackHandler {
        requestExit(onNavigateBack)
    }

    DisposableEffect(hasUnsavedChanges, isSaving) {
        onNavigationGuardChanged { action ->
            requestExit(action)
        }
        onDispose {
            onNavigationGuardChanged(null)
        }
    }

    pendingExitAction?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingExitAction = null },
            title = { Text(text = "Выйти без сохранения?") },
            text = { Text(text = "Изменения в задаче не будут сохранены.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingExitAction = null
                        action()
                    }
                ) {
                    Text(text = "Выйти")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingExitAction = null }) {
                    Text(text = "Остаться")
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (isEditMode) R.string.edit_task_title else R.string.add_task_title
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { requestExit(onNavigateBack) }) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.cancel)
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
                .imePadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Заполните название, остальные параметры можно оставить по умолчанию.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                AppCard {
                    SectionTitle("Основное")
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            titleError = false
                            markChanged()
                        },
                        label = { Text(text = stringResource(R.string.task_title_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = titleError,
                        supportingText = {
                            if (titleError) {
                                Text(text = stringResource(R.string.required_field))
                            }
                        },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = {
                            description = it
                            markChanged()
                        },
                        label = { Text(text = stringResource(R.string.task_description_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }

                AppCard {
                    SectionTitle("Параметры")
                    DropdownField(
                        label = stringResource(R.string.task_category_label),
                        selectedValue = category,
                        options = categoryOptions(),
                        onValueSelected = {
                            category = it
                            markChanged()
                        }
                    )
                    Text(text = stringResource(R.string.priority_label), style = MaterialTheme.typography.titleSmall)
                    OptionChips(
                        selectedValue = priority,
                        options = priorityOptions(),
                        colorForValue = ::priorityColor,
                        onSelected = {
                            priority = it
                            markChanged()
                        }
                    )
                    Text(text = stringResource(R.string.difficulty_label), style = MaterialTheme.typography.titleSmall)
                    OptionChips(
                        selectedValue = difficulty,
                        options = difficultyOptions(),
                        colorForValue = ::difficultyColor,
                        onSelected = {
                            difficulty = it
                            markChanged()
                        }
                    )
                }

                AppCard {
                    SectionTitle("Планирование")
                    DateField(
                        dueDate = dueDate,
                        onOpenDatePicker = { showDatePicker = true },
                        onClearDate = {
                            dueDate = null
                            markChanged()
                        }
                    )
                    Text(
                        text = stringResource(R.string.estimated_time_label),
                        style = MaterialTheme.typography.titleSmall
                    )
                    TimePickerInline(
                        selectedMinutes = estimatedMinutes,
                        onMinutesSelected = {
                            estimatedMinutes = it
                            markChanged()
                        }
                    )
                }

                AppCard(highlighted = true) {
                    val expectedXp = expectedXp(
                        title = title,
                        category = category.ifBlank { defaultCategory },
                        priority = priority,
                        difficulty = difficulty,
                        estimatedMinutes = estimatedMinutes,
                        dueDate = dueDate
                    )
                    SectionTitle("Итог")
                    SummaryLine("Награда", "$expectedXp XP")
                    SummaryLine("Дата", dueDate?.let(::formatDate) ?: stringResource(R.string.no_due_date))
                    SummaryLine("Время", stringResource(R.string.minutes_value, estimatedMinutes))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { requestExit(onNavigateBack) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text(text = "Отмена")
                }
                Button(
                    onClick = {
                        if (title.isBlank()) {
                            titleError = true
                            return@Button
                        }
                        if (isSaving) return@Button

                        isSaving = true
                        val task = TaskEntity(
                            id = existingTask?.id ?: 0,
                            title = title.trim(),
                            description = description.trim(),
                            category = category.trim().ifBlank { defaultCategory },
                            priority = priority,
                            difficulty = difficulty,
                            dueDate = dueDate,
                            estimatedMinutes = estimatedMinutes,
                            scheduledStartTime = existingTask?.scheduledStartTime,
                            scheduledEndTime = existingTask?.scheduledEndTime,
                            isCompleted = existingTask?.isCompleted ?: false,
                            xpAwarded = existingTask?.xpAwarded ?: false,
                            createdAt = existingTask?.createdAt ?: System.currentTimeMillis(),
                            completedAt = existingTask?.completedAt
                        )

                        if (isEditMode) {
                            taskViewModel.updateTask(task) {
                                TaskReminderScheduler.cancelTaskReminder(context, task.id)
                                TaskReminderScheduler.scheduleTaskDeadline(
                                    context = context,
                                    taskId = task.id,
                                    dueDate = task.dueDate,
                                    scheduledStartTime = task.scheduledStartTime
                                )
                                hasUnsavedChanges = false
                                onTaskSaved()
                            }
                        } else {
                            taskViewModel.addTask(task) { savedTaskId ->
                                TaskReminderScheduler.scheduleTaskDeadline(
                                    context = context,
                                    taskId = savedTaskId,
                                    dueDate = task.dueDate,
                                    scheduledStartTime = task.scheduledStartTime
                                )
                                hasUnsavedChanges = false
                                onTaskSaved()
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    enabled = !isSaving
                ) {
                    Text(text = stringResource(R.string.save))
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueDate ?: todayStartMillis(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return localDateFromMillis(utcTimeMillis) >= LocalDate.now()
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        dueDate = datePickerState.selectedDateMillis
                        showDatePicker = false
                        markChanged()
                    }
                ) {
                    Text(text = stringResource(R.string.date_picker_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        text = stringResource(R.string.date_picker_title),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                headline = {
                    val selectedDateText = datePickerState.selectedDateMillis?.let(::formatDate)
                        ?: stringResource(R.string.no_due_date)
                    Text(
                        text = selectedDateText,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionChips(
    selectedValue: String,
    options: List<FormOption>,
    colorForValue: (String) -> Color,
    onSelected: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            val color = colorForValue(option.value)
            FilterChip(
                selected = selectedValue == option.value,
                onClick = { onSelected(option.value) },
                label = { Text(text = option.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = color.copy(alpha = 0.22f),
                    selectedLabelColor = color
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedValue == option.value,
                    borderColor = color.copy(alpha = 0.65f),
                    selectedBorderColor = color
                )
            )
        }
    }
}

@Composable
private fun DateField(
    dueDate: Long?,
    onOpenDatePicker: () -> Unit,
    onClearDate: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = dueDate?.let(::formatDate) ?: "",
            onValueChange = {},
            label = { Text(text = stringResource(R.string.due_date_label)) },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            placeholder = { Text(text = stringResource(R.string.due_date_hint)) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = stringResource(R.string.select_due_date)
                )
            },
            singleLine = true
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { onOpenDatePicker() }
                .clearAndSetSemantics {
                    contentDescription = "Дата: ${dueDate?.let(::formatDate) ?: "не выбрана"}"
                }
        )
    }
    if (dueDate != null) {
        TextButton(onClick = onClearDate) {
            Text(text = stringResource(R.string.clear_due_date))
        }
    }
}

@Composable
private fun DropdownField(
    label: String,
    selectedValue: String,
    options: List<String>,
    onValueSelected: (String) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var fieldSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val menuModifier = if (fieldSize.width > 0) {
        Modifier.width(with(density) { fieldSize.width.toDp() })
    } else {
        Modifier.fillMaxWidth()
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(text = label) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = null
                )
            },
            modifier = Modifier
                .onGloballyPositioned { coordinates ->
                    fieldSize = coordinates.size
                }
                .fillMaxWidth()
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true }
                .clearAndSetSemantics {
                    contentDescription = "$label: $selectedValue"
                }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = menuModifier,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            shape = MaterialTheme.shapes.medium
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = option) },
                    onClick = {
                        onValueSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimePickerInline(
    selectedMinutes: Int,
    onMinutesSelected: (Int) -> Unit
) {
    val minutes = selectedMinutes.coerceIn(5, 360)

    LaunchedEffect(selectedMinutes) {
        if (selectedMinutes != minutes) {
            onMinutesSelected(minutes)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = "Длительность",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        TimeScaleControl(
            minutes = minutes,
            onMinutesSelected = onMinutesSelected
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "5 мин",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "6 ч",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { onMinutesSelected((minutes - 5).coerceAtLeast(5)) },
                modifier = Modifier.weight(1f),
                enabled = minutes > 5
            ) {
                Text(text = "-5 мин")
            }
            Button(
                onClick = { onMinutesSelected((minutes + 5).coerceAtMost(360)) },
                modifier = Modifier.weight(1f),
                enabled = minutes < 360
            ) {
                Text(text = "+5 мин")
            }
        }
    }
}

@Composable
private fun TimeScaleControl(
    minutes: Int,
    onMinutesSelected: (Int) -> Unit
) {
    val minMinutes = 5
    val maxMinutes = 360
    val progress = (minutes - minMinutes).toFloat() / (maxMinutes - minMinutes)
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        label = "time slider progress"
    )
    var trackWidth by remember { mutableStateOf(0) }

    fun selectMinutes(positionX: Float) {
        if (trackWidth <= 0) return

        val rawProgress = (positionX / trackWidth).coerceIn(0f, 1f)
        val rawMinutes = minMinutes + rawProgress * (maxMinutes - minMinutes)
        val roundedMinutes = (rawMinutes / 5).roundToInt() * 5
        onMinutesSelected(roundedMinutes.coerceIn(minMinutes, maxMinutes))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .onSizeChanged { size -> trackWidth = size.width }
            .pointerInput(trackWidth) {
                detectTapGestures { offset -> selectMinutes(offset.x) }
            }
            .pointerInput(trackWidth) {
                detectDragGestures { change, _ -> selectMinutes(change.position.x) }
            },
        contentAlignment = Alignment.BottomStart
    ) {
        Text(
            text = stringResource(R.string.minutes_value, minutes),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = MaterialTheme.shapes.small
                )
                .padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = MaterialTheme.shapes.extraSmall
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .height(12.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.extraSmall
                )
        )
    }
}

@Composable
private fun categoryOptions(): List<String> {
    return listOf(
        stringResource(R.string.category_study),
        stringResource(R.string.category_work),
        stringResource(R.string.category_home),
        stringResource(R.string.category_health),
        stringResource(R.string.category_finance),
        stringResource(R.string.category_personal),
        stringResource(R.string.category_shopping),
        stringResource(R.string.category_other)
    )
}

@Composable
private fun priorityOptions(): List<FormOption> {
    return listOf(
        FormOption("low", stringResource(R.string.priority_low)),
        FormOption("medium", stringResource(R.string.priority_medium)),
        FormOption("high", stringResource(R.string.priority_high))
    )
}

@Composable
private fun difficultyOptions(): List<FormOption> {
    return listOf(
        FormOption("easy", stringResource(R.string.difficulty_easy)),
        FormOption("medium", stringResource(R.string.difficulty_medium)),
        FormOption("hard", stringResource(R.string.difficulty_hard))
    )
}

private data class FormOption(
    val value: String,
    val label: String
)

private fun expectedXp(
    title: String,
    category: String,
    priority: String,
    difficulty: String,
    estimatedMinutes: Int,
    dueDate: Long?
): Int {
    val previewTask = TaskEntity(
        title = title.trim().ifBlank { "Задача" },
        description = "",
        category = category,
        priority = priority,
        difficulty = difficulty,
        estimatedMinutes = estimatedMinutes,
        dueDate = dueDate,
        createdAt = System.currentTimeMillis()
    )
    return GamificationManager.calculateTaskXp(previewTask)
}

private fun priorityColor(priority: String): Color {
    return when (priority) {
        "high" -> Color(0xFFE35D5D)
        "medium" -> Color(0xFFE2B93B)
        else -> Color(0xFF4CAF7A)
    }
}

private fun difficultyColor(difficulty: String): Color {
    return when (difficulty) {
        "hard" -> Color(0xFFE35D5D)
        "medium" -> Color(0xFFE2B93B)
        else -> Color(0xFF4CAF7A)
    }
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(timestamp))
}

private fun todayStartMillis(): Long {
    return LocalDate.now()
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}

private fun localDateFromMillis(timestamp: Long): LocalDate {
    return Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}
