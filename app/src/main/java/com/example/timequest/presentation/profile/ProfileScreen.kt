package com.example.timequest.presentation.profile

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.timequest.data.local.TaskEntity
import com.example.timequest.domain.Achievement
import com.example.timequest.domain.DailyGoal
import com.example.timequest.domain.GamificationManager
import com.example.timequest.domain.ThemeShopCatalog
import com.example.timequest.presentation.components.AppCard
import com.example.timequest.presentation.components.MetricCard
import com.example.timequest.presentation.components.SoftProgress
import com.example.timequest.presentation.theme_shop.ThemeShopScreen
import com.example.timequest.presentation.tasks.TaskViewModel
import com.example.timequest.notifications.TaskReminderScheduler
import com.example.timequest.ui.theme.AppThemeMode
import com.example.timequest.ui.theme.AppThemeStyle
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun ProfileScreen(
    taskViewModel: TaskViewModel,
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit,
    themeStyle: AppThemeStyle,
    unlockedThemeStyles: Set<AppThemeStyle>,
    spentXp: Int,
    onThemeStyleChange: (AppThemeStyle) -> Unit,
    onThemeStylePurchase: (AppThemeStyle, Int) -> Unit,
    morningNotificationEnabled: Boolean,
    eveningNotificationEnabled: Boolean,
    taskNotificationsEnabled: Boolean,
    onMorningNotificationChange: (Boolean) -> Unit,
    onEveningNotificationChange: (Boolean) -> Unit,
    onTaskNotificationsChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val tasks by taskViewModel.allTasks.collectAsState()
    var showStatsDetails by remember { mutableStateOf(false) }
    var showAchievementDetails by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showShop by remember { mutableStateOf(false) }

    val completedTasks = tasks.filter { it.isCompleted }
    val activeTasks = tasks.filterNot { it.isCompleted }
    val totalXp = GamificationManager.totalXp(tasks)
    val availableXp = (totalXp - spentXp).coerceAtLeast(0)
    val levelInfo = GamificationManager.levelInfo(totalXp)
    val streak = GamificationManager.currentStreak(tasks)
    val achievements = GamificationManager.achievements(tasks)
    val unlockedAchievements = achievements.filter { it.isUnlocked }
    val nextAchievement = achievements
        .filterNot { it.isUnlocked }
        .minByOrNull { achievement -> achievement.targetValue - achievement.currentValue }
    val completionRate = if (tasks.isEmpty()) 0 else completedTasks.size * 100 / tasks.size
    val completedToday = DailyGoal.completedToday(tasks)
    val bestDay = GamificationManager.bestCompletionDay(tasks)
    val shopItems = ThemeShopCatalog.items
    val unlockedShopCount = shopItems.count { item -> item.style in unlockedThemeStyles }

    if (showShop) {
        ThemeShopScreen(
            totalXp = totalXp,
            availableXp = availableXp,
            spentXp = spentXp,
            items = shopItems,
            unlockedThemeStyles = unlockedThemeStyles,
            selectedStyle = themeStyle,
            onBack = { showShop = false },
            onSelect = onThemeStyleChange,
            onPurchase = onThemeStylePurchase
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Профиль",
                style = MaterialTheme.typography.headlineSmall
            )
            IconButton(onClick = { showSettings = true }) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Настройки"
                )
            }
        }

        LevelHeroCard(
            level = levelInfo.level,
            totalXp = totalXp,
            availableXp = availableXp,
            spentXp = spentXp,
            progress = levelInfo.progress,
            nextLevelXp = levelInfo.nextLevelXp
        )

        DailyMotivationCard(
            completedToday = completedToday,
            streak = streak,
            goalProgress = DailyGoal.progress(tasks)
        )

        GoalCard(nextAchievement = nextAchievement)

        ThemeShopPreviewCard(
            availableXp = availableXp,
            unlockedCount = unlockedShopCount,
            totalCount = shopItems.size,
            onOpenShop = { showShop = true }
        )

        StatisticsCard(
            completedTasks = completedTasks.size,
            activeTasks = activeTasks.size,
            totalTasks = tasks.size,
            plannedTasks = tasks.count { it.scheduledStartTime != null },
            streak = streak,
            completedToday = completedToday,
            completionRate = completionRate,
            bestDay = bestDay?.let { "${it.first.dayOfMonth}.${it.first.monthValue}: ${it.second}" },
            showDetails = showStatsDetails,
            onToggleDetails = { showStatsDetails = !showStatsDetails }
        )

        AchievementsCard(
            achievements = achievements,
            unlockedCount = unlockedAchievements.size,
            showDetails = showAchievementDetails,
            onToggleDetails = { showAchievementDetails = !showAchievementDetails }
        )
    }

    if (showSettings) {
        ProfileSettingsDialog(
            themeMode = themeMode,
            onThemeModeChange = onThemeModeChange,
            morningNotificationEnabled = morningNotificationEnabled,
            eveningNotificationEnabled = eveningNotificationEnabled,
            taskNotificationsEnabled = taskNotificationsEnabled,
            onMorningNotificationChange = onMorningNotificationChange,
            onEveningNotificationChange = onEveningNotificationChange,
            onTaskNotificationsChange = { enabled ->
                onTaskNotificationsChange(enabled)
                activeTasks.forEach { task ->
                    TaskReminderScheduler.cancelTaskReminder(context, task.id)
                    if (enabled) {
                        TaskReminderScheduler.scheduleTaskDeadline(
                            context = context,
                            taskId = task.id,
                            dueDate = task.dueDate,
                            scheduledStartTime = task.scheduledStartTime
                        )
                    }
                }
            },
            onDismiss = { showSettings = false }
        )
    }
}

@Composable
private fun LevelHeroCard(
    level: Int,
    totalXp: Int,
    availableXp: Int,
    spentXp: Int,
    progress: Float,
    nextLevelXp: Int
) {
    AppCard(highlighted = true) {
        Text(
            text = "Центр мотивации",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Уровень",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AnimatedContent(targetState = level, label = "level") { value ->
                    Text(
                        text = value.toString(),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Column {
                Text(
                    text = "Всего XP",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AnimatedContent(targetState = totalXp, label = "total xp") { value ->
                    Text(
                        text = "$value XP",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        SoftProgress(progress = progress, modifier = Modifier.fillMaxWidth())
        Text(
            text = "До следующего уровня: ${(nextLevelXp - totalXp).coerceAtLeast(0)} XP",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(title = "Доступно", value = "$availableXp XP", modifier = Modifier.weight(1f))
            MetricCard(title = "Потрачено", value = "$spentXp XP", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun DailyMotivationCard(
    completedToday: Int,
    streak: Int,
    goalProgress: Float
) {
    AppCard {
        Text(
            text = "Сегодня",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(title = "Выполнено", value = completedToday.toString(), modifier = Modifier.weight(1f))
            MetricCard(title = "Серия", value = "$streak дн.", modifier = Modifier.weight(1f))
        }
        DetailLine("Цель дня", "${DailyGoal.displayedCompleted(completedToday)} из ${DailyGoal.TARGET_TASKS}")
        SoftProgress(progress = goalProgress, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun NotificationSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun GoalCard(nextAchievement: Achievement?) {
    AppCard {
        Text(
            text = "Ближайшая цель",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        if (nextAchievement != null) {
            Text(
                text = nextAchievement.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = nextAchievement.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SoftProgress(
                progress = nextAchievement.currentValue.toFloat() / nextAchievement.targetValue,
                modifier = Modifier.fillMaxWidth()
            )
            DetailLine(
                "Осталось до награды",
                "${(nextAchievement.targetValue - nextAchievement.currentValue).coerceAtLeast(0)}"
            )
        } else {
            Text(
                text = "Все достижения открыты. Можно увеличивать уровень и серию.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "Больше XP дают сложные задачи, высокий приоритет, выполнение в срок и сохранённый план дня.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ThemeShopPreviewCard(
    availableXp: Int,
    unlockedCount: Int,
    totalCount: Int,
    onOpenShop: () -> Unit
) {
    AppCard {
        SectionHeader(
            title = "Магазин XP",
            actionText = "Открыть",
            onActionClick = onOpenShop
        )
        Text(
            text = "Оформления покупаются за XP, полученный за выполнение задач.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        DetailLine("Доступно", "$availableXp XP")
        DetailLine("Открыто оформлений", "$unlockedCount из $totalCount")
        SoftProgress(
            progress = unlockedCount.toFloat() / totalCount,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StatisticsCard(
    completedTasks: Int,
    activeTasks: Int,
    totalTasks: Int,
    plannedTasks: Int,
    streak: Int,
    completedToday: Int,
    completionRate: Int,
    bestDay: String?,
    showDetails: Boolean,
    onToggleDetails: () -> Unit
) {
    AppCard {
        SectionHeader(
            title = "Статистика",
            actionText = if (showDetails) "Скрыть детали" else "Показать детали",
            onActionClick = onToggleDetails
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(title = "Выполнено", value = completedTasks.toString(), modifier = Modifier.weight(1f))
            MetricCard(title = "Серия", value = "$streak дн.", modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(title = "Сегодня", value = completedToday.toString(), modifier = Modifier.weight(1f))
            MetricCard(title = "Успех", value = "$completionRate%", modifier = Modifier.weight(1f))
        }
        AnimatedVisibility(visible = showDetails) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                DetailLine("Активные задачи", activeTasks.toString())
                DetailLine("Все задачи", totalTasks.toString())
                DetailLine("Запланировано", plannedTasks.toString())
                DetailLine("Лучший день", bestDay ?: "Пока нет")
            }
        }
    }
}

@Composable
private fun AchievementsCard(
    achievements: List<Achievement>,
    unlockedCount: Int,
    showDetails: Boolean,
    onToggleDetails: () -> Unit
) {
    AppCard {
        SectionHeader(
            title = "Достижения",
            actionText = if (showDetails) "Скрыть все" else "Показать все",
            onActionClick = onToggleDetails
        )
        Text(
            text = "$unlockedCount из ${achievements.size} открыто",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val preview = if (showDetails) achievements else achievements.take(3)
        preview.forEach { achievement ->
            AchievementRow(achievement = achievement)
        }
    }
}

@Composable
private fun ProfileSettingsDialog(
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit,
    morningNotificationEnabled: Boolean,
    eveningNotificationEnabled: Boolean,
    taskNotificationsEnabled: Boolean,
    onMorningNotificationChange: (Boolean) -> Unit,
    onEveningNotificationChange: (Boolean) -> Unit,
    onTaskNotificationsChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Настройки") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Режим темы",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                ThemeOptionRow(
                    title = "Как в системе",
                    selected = themeMode == AppThemeMode.SYSTEM,
                    onClick = { onThemeModeChange(AppThemeMode.SYSTEM) }
                )
                ThemeOptionRow(
                    title = "Светлая",
                    selected = themeMode == AppThemeMode.LIGHT,
                    onClick = { onThemeModeChange(AppThemeMode.LIGHT) }
                )
                ThemeOptionRow(
                    title = "Тёмная",
                    selected = themeMode == AppThemeMode.DARK,
                    onClick = { onThemeModeChange(AppThemeMode.DARK) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Text(
                    text = "Уведомления",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                NotificationSwitchRow(
                    title = "Утренний план",
                    description = "В 09:00",
                    checked = morningNotificationEnabled,
                    onCheckedChange = onMorningNotificationChange
                )
                NotificationSwitchRow(
                    title = "Вечерний итог",
                    description = "В 20:00",
                    checked = eveningNotificationEnabled,
                    onCheckedChange = onEveningNotificationChange
                )
                NotificationSwitchRow(
                    title = "Напоминания по задачам",
                    description = "За 5 минут до старта или утром в день срока",
                    checked = taskNotificationsEnabled,
                    onCheckedChange = onTaskNotificationsChange
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Готово")
            }
        }
    )
}

@Composable
private fun ThemeOptionRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        RadioButton(
            selected = selected,
            onClick = onClick
        )
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
        modifier = Modifier.fillMaxWidth(),
        color = if (achievement.isUnlocked) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.38f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f)
        },
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
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
            AnimatedVisibility(visible = achievement.isUnlocked) {
                Text(
                    text = "Бейдж открыт",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
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
        AnimatedContent(targetState = value, label = "detail value") { animatedValue ->
            Text(
                text = animatedValue,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun Long.toLocalDate(): LocalDate {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}
