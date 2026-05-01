package com.example.timequest.presentation.focus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.timequest.notifications.TaskReminderScheduler
import com.example.timequest.presentation.tasks.FocusSessionUiState
import com.example.timequest.presentation.tasks.TaskViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusQuestScreen(
    taskViewModel: TaskViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val focusSession by taskViewModel.focusSession.collectAsState()
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(focusSession?.taskId, focusSession?.isRunning) {
        while (focusSession?.isRunning == true) {
            nowMillis = System.currentTimeMillis()
            delay(1_000L)
        }
    }

    val session = focusSession
    val remainingSeconds = taskViewModel.focusRemainingSeconds(nowMillis)
    val progress = if (session == null || session.totalSeconds <= 0) {
        0f
    } else {
        (1f - remainingSeconds.toFloat() / session.totalSeconds).coerceIn(0f, 1f)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(text = "Фокус") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (session == null) {
                EmptyFocusCard(onNavigateBack = onNavigateBack)
                return@Column
            }

            FocusTimerCard(
                session = session,
                remainingSeconds = remainingSeconds,
                progress = progress
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val updated = if (session.isRunning) {
                            taskViewModel.pauseFocusQuest()
                        } else {
                            taskViewModel.resumeFocusQuest()
                        }
                        updateFocusNotification(context, updated)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = if (session.isRunning) "Пауза" else "Продолжить")
                }
                OutlinedButton(
                    onClick = {
                        val updated = taskViewModel.resetFocusQuest()
                        updateFocusNotification(context, updated)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Сбросить таймер")
                }
            }

            Button(
                onClick = {
                    TaskReminderScheduler.cancelFocusNotification(context)
                    TaskReminderScheduler.cancelFocusFinished(context)
                    taskViewModel.completeFocusQuest {
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Завершить задачу")
            }

            OutlinedButton(
                onClick = {
                    taskViewModel.closeFocusQuest()
                    TaskReminderScheduler.cancelFocusNotification(context)
                    TaskReminderScheduler.cancelFocusFinished(context)
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Остановить фокус")
            }
        }
    }
}

@Composable
private fun EmptyFocusCard(onNavigateBack: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Фокус не запущен",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Выберите активную задачу и нажмите «Начать фокус».",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onNavigateBack) {
                Text(text = "Вернуться")
            }
        }
    }
}

@Composable
private fun FocusTimerCard(
    session: FocusSessionUiState,
    remainingSeconds: Int,
    progress: Float
) {
    val isFinished = remainingSeconds <= 0
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = session.taskTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (isFinished) {
                    "Время фокуса закончилось. Отметьте результат задачи."
                } else {
                    "Сконцентрируйтесь на одной задаче. Можно выйти с экрана и вернуться по таймеру."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatTimer(remainingSeconds),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun updateFocusNotification(
    context: android.content.Context,
    session: FocusSessionUiState?
) {
    if (session == null) return
    if (session.isRunning) {
        TaskReminderScheduler.showFocusNotification(
            context = context,
            taskTitle = session.taskTitle,
            endAtMillis = session.endAtMillis
        )
        TaskReminderScheduler.scheduleFocusFinished(
            context = context,
            taskId = session.taskId,
            endAtMillis = session.endAtMillis
        )
    } else {
        TaskReminderScheduler.cancelFocusFinished(context)
        TaskReminderScheduler.showFocusNotification(
            context = context,
            taskTitle = session.taskTitle,
            endAtMillis = null,
            remainingSeconds = session.remainingSeconds
        )
    }
}

private fun formatTimer(seconds: Int): String {
    val minutes = seconds.coerceAtLeast(0) / 60
    val secondsPart = seconds.coerceAtLeast(0) % 60
    return "%02d:%02d".format(minutes, secondsPart)
}
