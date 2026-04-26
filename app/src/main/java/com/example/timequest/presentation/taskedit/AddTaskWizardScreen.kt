package com.example.timequest.presentation.taskedit

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
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
import com.example.timequest.notifications.TaskReminderScheduler
import com.example.timequest.presentation.tasks.TaskViewModel
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTaskWizardScreen(
    taskId: Long?,
    taskViewModel: TaskViewModel,
    onNavigateBack: () -> Unit,
    onTaskSaved: () -> Unit
) {
    val isEditMode = taskId != null
    val defaultCategory = stringResource(R.string.category_study)
    val priorityItems = priorityOptions()
    val difficultyItems = difficultyOptions()
    val context = LocalContext.current

    var currentStep by rememberSaveable { mutableStateOf(0) }
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var priority by rememberSaveable { mutableStateOf("medium") }
    var difficulty by rememberSaveable { mutableStateOf("medium") }
    var estimatedMinutes by rememberSaveable { mutableStateOf(30) }
    var dueDate by rememberSaveable { mutableStateOf<Long?>(todayStartMillis()) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var titleError by rememberSaveable { mutableStateOf(false) }
    var isSaving by rememberSaveable { mutableStateOf(false) }
    var existingTask by remember { mutableStateOf<TaskEntity?>(null) }

    val stepCount = 6
    val canGoNext = currentStep != 0 || title.isNotBlank()

    LaunchedEffect(taskId) {
        if (taskId != null) {
            val task = taskViewModel.getTaskById(taskId)
            existingTask = task
            if (task != null) {
                title = task.title
                description = task.description
                category = task.category
                priority = task.priority
                difficulty = task.difficulty
                estimatedMinutes = task.estimatedMinutes
                dueDate = task.dueDate
            }
        }
    }

    LaunchedEffect(defaultCategory) {
        if (category.isBlank()) {
            category = defaultCategory
        }
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
                    IconButton(onClick = onNavigateBack) {
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            WizardProgress(
                currentStep = currentStep,
                stepCount = stepCount
            )

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "task_wizard_step",
                modifier = Modifier.weight(1f)
            ) { step ->
                StepCard {
                    when (step) {
                        0 -> MainInfoStep(
                            title = title,
                            description = description,
                            titleError = titleError,
                            onTitleChange = {
                                title = it
                                titleError = false
                            },
                            onDescriptionChange = { description = it }
                        )

                        1 -> CategoryStep(
                            category = category,
                            onCategoryChange = { category = it }
                        )

                        2 -> PriorityDifficultyStep(
                            priority = priority,
                            difficulty = difficulty,
                            priorityItems = priorityItems,
                            difficultyItems = difficultyItems,
                            onPriorityChange = { priority = it },
                            onDifficultyChange = { difficulty = it }
                        )

                        3 -> EstimatedTimeStep(
                            estimatedMinutes = estimatedMinutes,
                            onEstimatedMinutesChange = { estimatedMinutes = it }
                        )

                        4 -> DeadlineStep(
                            dueDate = dueDate,
                            onOpenDatePicker = { showDatePicker = true },
                            onClearDate = { dueDate = null }
                        )

                        else -> ReviewStep(
                            title = title,
                            category = category.ifBlank { defaultCategory },
                            priority = priority,
                            difficulty = difficulty,
                            estimatedMinutes = estimatedMinutes,
                            dueDate = dueDate
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (currentStep == 0) {
                            onNavigateBack()
                        } else {
                            currentStep--
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Назад")
                }

                Button(
                    onClick = {
                        if (currentStep == 0 && title.isBlank()) {
                            titleError = true
                            return@Button
                        }

                        if (currentStep < stepCount - 1) {
                            currentStep++
                        } else if (!isSaving) {
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
                                    onTaskSaved()
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = (canGoNext || currentStep == stepCount - 1) && !isSaving
                ) {
                    Text(text = if (currentStep == stepCount - 1) "Сохранить" else "Далее")
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
private fun WizardProgress(
    currentStep: Int,
    stepCount: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Шаг ${currentStep + 1} из $stepCount",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stepTitle(currentStep),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LinearProgressIndicator(
            progress = { (currentStep + 1).toFloat() / stepCount },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StepCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
    }
}

@Composable
private fun MainInfoStep(
    title: String,
    description: String,
    titleError: Boolean,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit
) {
    StepHeader(
        title = "Что нужно сделать?",
        subtitle = "Сначала запишите задачу простыми словами."
    )
    OutlinedTextField(
        value = title,
        onValueChange = onTitleChange,
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
        onValueChange = onDescriptionChange,
        label = { Text(text = stringResource(R.string.task_description_label)) },
        modifier = Modifier.fillMaxWidth(),
        minLines = 4
    )
}

@Composable
private fun CategoryStep(
    category: String,
    onCategoryChange: (String) -> Unit
) {
    StepHeader(
        title = "Выберите категорию",
        subtitle = "Категория помогает видеть, куда уходит время."
    )
    DropdownField(
        label = stringResource(R.string.task_category_label),
        selectedValue = category,
        options = categoryOptions(),
        onValueSelected = onCategoryChange
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PriorityDifficultyStep(
    priority: String,
    difficulty: String,
    priorityItems: List<WizardFormOption>,
    difficultyItems: List<WizardFormOption>,
    onPriorityChange: (String) -> Unit,
    onDifficultyChange: (String) -> Unit
) {
    StepHeader(
        title = "Оцените задачу",
        subtitle = "Приоритет и сложность влияют на порядок и XP."
    )
    Text(
        text = stringResource(R.string.priority_label),
        style = MaterialTheme.typography.titleSmall
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        priorityItems.forEach { item ->
            FilterChip(
                selected = priority == item.value,
                onClick = { onPriorityChange(item.value) },
                label = { Text(text = item.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = priorityColor(item.value).copy(alpha = 0.22f),
                    selectedLabelColor = priorityColor(item.value)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = priority == item.value,
                    borderColor = priorityColor(item.value).copy(alpha = 0.65f),
                    selectedBorderColor = priorityColor(item.value)
                )
            )
        }
    }
    Text(
        text = stringResource(R.string.difficulty_label),
        style = MaterialTheme.typography.titleSmall
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        difficultyItems.forEach { item ->
            FilterChip(
                selected = difficulty == item.value,
                onClick = { onDifficultyChange(item.value) },
                label = { Text(text = item.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = difficultyColor(item.value).copy(alpha = 0.22f),
                    selectedLabelColor = difficultyColor(item.value)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = difficulty == item.value,
                    borderColor = difficultyColor(item.value).copy(alpha = 0.65f),
                    selectedBorderColor = difficultyColor(item.value)
                )
            )
        }
    }
}

@Composable
private fun EstimatedTimeStep(
    estimatedMinutes: Int,
    onEstimatedMinutesChange: (Int) -> Unit
) {
    StepHeader(
        title = "Сколько займет задача?",
        subtitle = "Выберите примерную длительность с шагом 5 минут."
    )
    TimeWheelPicker(
        selectedMinutes = estimatedMinutes,
        onMinutesSelected = onEstimatedMinutesChange
    )
}

@Composable
private fun DeadlineStep(
    dueDate: Long?,
    onOpenDatePicker: () -> Unit,
    onClearDate: () -> Unit
) {
    StepHeader(
        title = "К какому дню выполнить?",
        subtitle = "Прошедшие даты выбрать нельзя."
    )
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
private fun ReviewStep(
    title: String,
    category: String,
    priority: String,
    difficulty: String,
    estimatedMinutes: Int,
    dueDate: Long?
) {
    val previewTask = TaskEntity(
        title = title.trim(),
        description = "",
        category = category,
        priority = priority,
        difficulty = difficulty,
        estimatedMinutes = estimatedMinutes,
        dueDate = dueDate,
        createdAt = System.currentTimeMillis()
    )
    val expectedXp = GamificationManager.calculateTaskXp(previewTask)

    StepHeader(
        title = "Проверьте задачу",
        subtitle = "Если всё верно, сохраните задачу."
    )
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "$expectedXp XP",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Примерная награда за выполнение",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    SummaryRow(label = stringResource(R.string.task_title_label), value = title)
    SummaryRow(label = stringResource(R.string.task_category_label), value = category)
    SummaryRow(label = stringResource(R.string.priority_label), value = priorityOptionLabel(priority))
    SummaryRow(label = stringResource(R.string.difficulty_label), value = difficultyOptionLabel(difficulty))
    SummaryRow(
        label = stringResource(R.string.estimated_time_label),
        value = stringResource(R.string.minutes_value, estimatedMinutes)
    )
    SummaryRow(
        label = stringResource(R.string.due_date_label),
        value = dueDate?.let(::formatDate) ?: stringResource(R.string.no_due_date)
    )
}

@Composable
private fun StepHeader(
    title: String,
    subtitle: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value.ifBlank { "Не указано" },
            style = MaterialTheme.typography.titleMedium
        )
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
                .fillMaxWidth(),
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

@Composable
private fun TimeWheelPicker(
    selectedMinutes: Int,
    onMinutesSelected: (Int) -> Unit
) {
    val minutes = selectedMinutes.coerceIn(0, 480)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.minutes_value, minutes),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Будет сохранено это значение",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { onMinutesSelected((minutes - 5).coerceAtLeast(0)) },
                modifier = Modifier.weight(1f),
                enabled = minutes > 0
            ) {
                Text(text = "-5 мин")
            }
            Button(
                onClick = { onMinutesSelected((minutes + 5).coerceAtMost(480)) },
                modifier = Modifier.weight(1f),
                enabled = minutes < 480
            ) {
                Text(text = "+5 мин")
            }
        }

        Slider(
            value = minutes.toFloat(),
            onValueChange = { value ->
                val roundedValue = (value / 5).toInt() * 5
                onMinutesSelected(roundedValue.coerceIn(0, 480))
            },
            valueRange = 0f..480f,
            steps = 95,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun stepTitle(step: Int): String {
    return when (step) {
        0 -> stringResource(R.string.section_main_info)
        1 -> stringResource(R.string.task_category_label)
        2 -> stringResource(R.string.section_task_params)
        3 -> "Время"
        4 -> stringResource(R.string.due_date_label)
        else -> "Итог"
    }
}

@Composable
private fun priorityOptionLabel(value: String): String {
    return when (value) {
        "high" -> stringResource(R.string.priority_high)
        "medium" -> stringResource(R.string.priority_medium)
        else -> stringResource(R.string.priority_low)
    }
}

@Composable
private fun difficultyOptionLabel(value: String): String {
    return when (value) {
        "hard" -> stringResource(R.string.difficulty_hard)
        "medium" -> stringResource(R.string.difficulty_medium)
        else -> stringResource(R.string.difficulty_easy)
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
private fun priorityOptions(): List<WizardFormOption> {
    return listOf(
        WizardFormOption("low", stringResource(R.string.priority_low)),
        WizardFormOption("medium", stringResource(R.string.priority_medium)),
        WizardFormOption("high", stringResource(R.string.priority_high))
    )
}

@Composable
private fun difficultyOptions(): List<WizardFormOption> {
    return listOf(
        WizardFormOption("easy", stringResource(R.string.difficulty_easy)),
        WizardFormOption("medium", stringResource(R.string.difficulty_medium)),
        WizardFormOption("hard", stringResource(R.string.difficulty_hard))
    )
}

private data class WizardFormOption(
    val value: String,
    val label: String
)

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
    val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    return formatter.format(Date(timestamp))
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
