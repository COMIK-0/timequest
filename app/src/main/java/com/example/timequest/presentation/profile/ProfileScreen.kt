package com.example.timequest.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.timequest.data.local.TaskEntity
import com.example.timequest.domain.Achievement
import com.example.timequest.domain.GamificationManager
import com.example.timequest.presentation.components.AppCard
import com.example.timequest.presentation.components.MetricCard
import com.example.timequest.presentation.components.SoftProgress
import com.example.timequest.presentation.tasks.TaskViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun ProfileScreen(taskViewModel: TaskViewModel) {
    val tasks by taskViewModel.allTasks.collectAsState()
    var showStatsDetails by remember { mutableStateOf(false) }
    var showAchievementDetails by remember { mutableStateOf(false) }

    val completedTasks = tasks.filter { it.isCompleted }
    val activeTasks = tasks.filter { !it.isCompleted }
    val totalXp = GamificationManager.totalXp(tasks)
    val levelInfo = GamificationManager.levelInfo(totalXp)
    val streak = GamificationManager.currentStreak(tasks)
    val achievements = GamificationManager.achievements(tasks)
    val unlockedAchievements = achievements.filter { it.isUnlocked }
    val completionRate = if (tasks.isEmpty()) 0 else completedTasks.size * 100 / tasks.size
    val completedToday = tasks.count { it.completedAt?.toLocalDate() == LocalDate.now() }
    val bestDay = GamificationManager.bestCompletionDay(tasks)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Профиль",
            style = MaterialTheme.typography.headlineSmall
        )

        AppCard(highlighted = true) {
            Text(
                text = "Уровень ${levelInfo.level}",
            style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "$totalXp XP накоплено",
                style = MaterialTheme.typography.titleMedium
            )
            SoftProgress(
                progress = levelInfo.progress,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "До следующего уровня: ${(levelInfo.nextLevelXp - totalXp).coerceAtLeast(0)} XP",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AppCard {
            SectionHeader(
                title = "Статистика",
                actionText = if (showStatsDetails) "Скрыть" else "Подробнее",
                onActionClick = { showStatsDetails = !showStatsDetails }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard(title = "Выполнено", value = completedTasks.size.toString(), modifier = Modifier.weight(1f))
                MetricCard(title = "Серия", value = "$streak дн.", modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard(title = "Сегодня", value = completedToday.toString(), modifier = Modifier.weight(1f))
                MetricCard(title = "Успех", value = "$completionRate%", modifier = Modifier.weight(1f))
            }
            if (showStatsDetails) {
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                DetailLine("Активные задачи", activeTasks.size.toString())
                DetailLine("Все задачи", tasks.size.toString())
                DetailLine("Запланировано", tasks.count { it.scheduledStartTime != null }.toString())
                DetailLine(
                    "Лучший день",
                    bestDay?.let { "${it.first.dayOfMonth}.${it.first.monthValue}: ${it.second}" } ?: "Пока нет"
                )
            }
        }

        AppCard {
            SectionHeader(
                title = "Награды",
                actionText = if (showAchievementDetails) "Скрыть" else "Подробнее",
                onActionClick = { showAchievementDetails = !showAchievementDetails }
            )
            Text(
                text = "${unlockedAchievements.size} из ${achievements.size} открыто",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val preview = if (showAchievementDetails) achievements else achievements.take(3)
            preview.forEach { achievement ->
                AchievementRow(achievement = achievement)
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionText: String,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        TextButton(onClick = onActionClick) {
            Text(text = actionText)
        }
    }
}

@Composable
private fun AchievementRow(achievement: Achievement) {
    Surface(
        color = if (achievement.isUnlocked) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.38f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f)
        },
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = achievement.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = achievement.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SoftProgress(
                progress = if (achievement.targetValue == 0) {
                    0f
                } else {
                    achievement.currentValue.toFloat() / achievement.targetValue
                },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = if (achievement.isUnlocked) {
                    "Получено"
                } else {
                    "${achievement.currentValue}/${achievement.targetValue}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (achievement.isUnlocked) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun Long.toLocalDate(): LocalDate {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}
