package com.example.timequest.presentation.tasks.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.timequest.R
import com.example.timequest.data.local.TaskEntity
import com.example.timequest.domain.GamificationManager
import com.example.timequest.presentation.components.AppChip
import com.example.timequest.presentation.components.PriorityChip
import com.example.timequest.presentation.components.XpBadge
import com.example.timequest.presentation.components.priorityColor
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskCard(
    task: TaskEntity,
    onToggleComplete: () -> Unit,
    onEditClick: () -> Unit,
    focusActionLabel: String = "Начать фокус",
    onFocusClick: (TaskEntity) -> Unit,
    onDeleteClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val taskXp = GamificationManager.calculateTaskXp(task)
    val completeActionDescription = if (task.isCompleted) {
        "Вернуть задачу ${task.title} в активные"
    } else {
        "Отметить задачу ${task.title} выполненной"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 112.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(priorityColor(task.priority))
            )
            Column(
                modifier = Modifier.padding(start = 10.dp, top = 10.dp, end = 8.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = task.isCompleted,
                            onCheckedChange = { onToggleComplete() },
                            modifier = Modifier
                                .size(44.dp)
                                .semantics { contentDescription = completeActionDescription }
                        )
                        Column(
                            modifier = Modifier.padding(start = 2.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.titleMedium,
                                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (task.description.isNotBlank()) {
                                Text(
                                    text = task.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = "Действия с задачей",
                                modifier = Modifier.size(21.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp,
                            shadowElevation = 6.dp,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            DropdownMenuItem(
                                text = { Text(text = stringResource(R.string.edit_task)) },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Outlined.Edit, contentDescription = null)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onEditClick()
                                }
                            )
                            if (!task.isCompleted) {
                                DropdownMenuItem(
                                    text = { Text(text = focusActionLabel) },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Outlined.PlayArrow, contentDescription = null)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onFocusClick(task)
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(text = stringResource(R.string.delete_task)) },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Outlined.Delete, contentDescription = null)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteClick()
                                }
                            )
                        }
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    XpBadge(xp = taskXp)
                    PriorityChip(priority = task.priority)
                    if (task.scheduledStartTime != null && task.scheduledEndTime != null) {
                        AppChip(
                            text = "${formatTime(task.scheduledStartTime)}-${formatTime(task.scheduledEndTime)}",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (task.category.isNotBlank()) {
                        AppChip(text = task.category, color = MaterialTheme.colorScheme.secondary)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        task.dueDate?.let { dueDate ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarToday,
                                    contentDescription = null,
                                    tint = if (dueDate.isNearDeadline()) priorityColor("high") else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = formatDate(dueDate),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = if (dueDate.isNearDeadline()) {
                                        priorityColor("high")
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                        Text(
                            text = taskStatusText(task),
                            style = MaterialTheme.typography.bodySmall,
                            color = taskStatusColor(task)
                        )
                    }
                }

                if (!task.isCompleted && task.dueDate?.isOverdue() == true) {
                    Text(
                        text = "Просрочено. Начните с 15 минут или перенесите в план.",
                        style = MaterialTheme.typography.bodySmall,
                        color = priorityColor("high")
                    )
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

private fun formatTime(timestamp: Long): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

private fun Long.isNearDeadline(): Boolean {
    val date = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
    val today = LocalDate.now()
    return date <= today.plusDays(1)
}

private fun Long.isOverdue(): Boolean {
    val date = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
    return date < LocalDate.now()
}

@Composable
private fun taskStatusText(task: TaskEntity): String {
    return when {
        task.isCompleted -> "Выполнена"
        task.dueDate?.isOverdue() == true -> "Просрочена"
        else -> "Активная"
    }
}

@Composable
private fun taskStatusColor(task: TaskEntity): androidx.compose.ui.graphics.Color {
    return when {
        task.isCompleted -> MaterialTheme.colorScheme.secondary
        task.dueDate?.isOverdue() == true -> priorityColor("high")
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}
