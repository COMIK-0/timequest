package com.example.timequest.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.timequest.data.local.TaskEntity
import com.example.timequest.presentation.tasks.TaskViewModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CalendarScreen(
    taskViewModel: TaskViewModel,
    onAddTaskForDate: (Long) -> Unit,
    onOpenDayPlanner: (Long) -> Unit
) {
    val tasks by taskViewModel.allTasks.collectAsState()
    var visibleMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    val plannedDates = tasks.mapNotNull { task -> task.plannedDate() }.toSet()
    val completedDates = tasks.mapNotNull { task -> task.completedAt?.toLocalDate() }.toSet()
    val selectedTasks = tasks
        .filter { task -> task.plannedDate() == selectedDate || task.completedAt?.toLocalDate() == selectedDate }
        .sortedWith(compareBy<TaskEntity> { it.isCompleted }.thenBy { it.scheduledStartTime ?: it.dueDate ?: Long.MAX_VALUE })
    val recentCompleted = tasks
        .filter { it.isCompleted && it.completedAt != null }
        .sortedByDescending { it.completedAt }
        .take(4)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MonthHeader(
            month = visibleMonth,
            onPrevious = { visibleMonth = visibleMonth.minusMonths(1) },
            onNext = { visibleMonth = visibleMonth.plusMonths(1) }
        )

        CalendarMonth(
            month = visibleMonth,
            selectedDate = selectedDate,
            plannedDates = plannedDates,
            completedDates = completedDates,
            onDateSelected = { selectedDate = it }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(color = MaterialTheme.colorScheme.primary, text = "есть план")
            LegendItem(color = MaterialTheme.colorScheme.secondary, text = "есть выполнение")
        }

        SectionCard(
            title = selectedDate.format(DateTimeFormatter.ofPattern("d MMMM", Locale("ru"))),
            subtitle = "План и результат за выбранный день"
        ) {
            if (selectedTasks.isEmpty()) {
                EmptyLine("На этот день задач пока нет.")
            } else {
                selectedTasks.forEach { task ->
                    CalendarTaskRow(task = task)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onAddTaskForDate(selectedDate.startMillis()) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Добавить задачу", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Button(
                    onClick = { onOpenDayPlanner(selectedDate.startMillis()) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "План дня", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        SectionCard(
            title = "Последние выполненные",
            subtitle = "Короткая история прогресса"
        ) {
            if (recentCompleted.isEmpty()) {
                EmptyLine("Выполненные задачи появятся здесь.")
            } else {
                recentCompleted.forEach { task ->
                    CalendarTaskRow(task = task)
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(
    month: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(imageVector = Icons.Outlined.ChevronLeft, contentDescription = "Предыдущий месяц")
        }
        Text(
            text = month.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale("ru"))).uppercase(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        IconButton(onClick = onNext) {
            Icon(imageVector = Icons.Outlined.ChevronRight, contentDescription = "Следующий месяц")
        }
    }
}

@Composable
private fun CalendarMonth(
    month: YearMonth,
    selectedDate: LocalDate,
    plannedDates: Set<LocalDate>,
    completedDates: Set<LocalDate>,
    onDateSelected: (LocalDate) -> Unit
) {
    val firstCell = month.atDay(1).previousOrSame(DayOfWeek.MONDAY)
    val dates = (0 until 42).map { firstCell.plusDays(it.toLong()) }
    val weekDays = listOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС")

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                weekDays.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            dates.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        CalendarDay(
                            date = date,
                            isCurrentMonth = date.month == month.month,
                            isSelected = date == selectedDate,
                            hasPlan = date in plannedDates,
                            hasCompleted = date in completedDates,
                            onClick = { onDateSelected(date) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDay(
    date: LocalDate,
    isCurrentMonth: Boolean,
    isSelected: Boolean,
    hasPlan: Boolean,
    hasCompleted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isCurrentMonth -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    }
    val background = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
                fontWeight = if (isSelected || hasPlan) FontWeight.SemiBold else FontWeight.Normal
            )
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (hasPlan) Dot(MaterialTheme.colorScheme.primary)
                if (hasCompleted) Dot(MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun Dot(color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .size(4.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun LegendItem(
    color: androidx.compose.ui.graphics.Color,
    text: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Dot(color)
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            content()
        }
    }
}

@Composable
private fun CalendarTaskRow(task: TaskEntity) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (task.isCompleted) Icons.Outlined.CheckCircle else Icons.Outlined.Event,
            contentDescription = null,
            tint = if (task.isCompleted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (task.isCompleted) "Выполнено" else "${task.estimatedMinutes} мин",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private fun TaskEntity.plannedDate(): LocalDate? {
    val millis = scheduledStartTime ?: dueDate ?: return null
    return millis.toLocalDate()
}

private fun Long.toLocalDate(): LocalDate {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}

private fun LocalDate.previousOrSame(dayOfWeek: DayOfWeek): LocalDate {
    var date = this
    while (date.dayOfWeek != dayOfWeek) {
        date = date.minusDays(1)
    }
    return date
}

private fun LocalDate.startMillis(): Long {
    return atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}
