package com.example.timequest.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.timequest.data.local.TaskEntity
import com.example.timequest.domain.Achievement
import com.example.timequest.domain.GamificationManager
import com.example.timequest.presentation.components.AppCard
import com.example.timequest.presentation.components.MetricCard
import com.example.timequest.presentation.components.SoftProgress
import com.example.timequest.presentation.tasks.TaskViewModel
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
    onThemeStylePurchase: (AppThemeStyle, Int) -> Unit
) {
    val tasks by taskViewModel.allTasks.collectAsState()
    var showStatsDetails by remember { mutableStateOf(false) }
    var showAchievementDetails by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showShop by remember { mutableStateOf(false) }
    var pendingPurchase by remember { mutableStateOf<ThemeShopItem?>(null) }

    val completedTasks = tasks.filter { it.isCompleted }
    val activeTasks = tasks.filter { !it.isCompleted }
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
    val completedToday = tasks.count { it.completedAt?.toLocalDate() == LocalDate.now() }
    val bestDay = GamificationManager.bestCompletionDay(tasks)
    val shopItems = themeShopItems()
    val unlockedShopCount = shopItems.count { item -> item.style in unlockedThemeStyles }

    pendingPurchase?.let { item ->
        ConfirmThemePurchaseDialog(
            item = item,
            availableXp = availableXp,
            onConfirm = {
                onThemeStylePurchase(item.style, item.price)
                pendingPurchase = null
            },
            onDismiss = { pendingPurchase = null }
        )
    }

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
            onPurchaseRequest = { item -> pendingPurchase = item }
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
            DetailLine("Доступно для магазина", "$availableXp XP")
            DetailLine("Потрачено на оформление", "$spentXp XP")
        }

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

        AppCard {
            Text(
                text = "Магазин XP",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Открыто $unlockedShopCount из ${shopItems.size} тем. Доступно: $availableXp XP.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SoftProgress(
                progress = unlockedShopCount.toFloat() / shopItems.size,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { showShop = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Открыть магазин тем")
            }
        }

        AppCard {
            SectionHeader(
                title = "Статистика",
                actionText = if (showStatsDetails) "Скрыть детали" else "Показать детали",
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
                actionText = if (showAchievementDetails) "Скрыть все" else "Показать все",
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

    if (showSettings) {
        ProfileSettingsDialog(
            themeMode = themeMode,
            onThemeModeChange = onThemeModeChange,
            themeStyle = themeStyle,
            unlockedThemeStyles = unlockedThemeStyles,
            onThemeStyleChange = onThemeStyleChange,
            onDismiss = { showSettings = false }
        )
    }
}

@Composable
private fun ThemeShopScreen(
    totalXp: Int,
    availableXp: Int,
    spentXp: Int,
    items: List<ThemeShopItem>,
    unlockedThemeStyles: Set<AppThemeStyle>,
    selectedStyle: AppThemeStyle,
    onBack: () -> Unit,
    onSelect: (AppThemeStyle) -> Unit,
    onPurchaseRequest: (ThemeShopItem) -> Unit
) {
    val unlockedCount = items.count { item -> item.style in unlockedThemeStyles }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Магазин тем",
                style = MaterialTheme.typography.headlineSmall
            )
            TextButton(onClick = onBack) {
                Text(text = "Назад")
            }
        }

        AppCard(highlighted = true) {
            Text(
                text = "$availableXp XP доступно",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            DetailLine("Всего заработано", "$totalXp XP")
            DetailLine("Потрачено на темы", "$spentXp XP")
            DetailLine("Коллекция", "$unlockedCount из ${items.size}")
            SoftProgress(
                progress = unlockedCount.toFloat() / items.size,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Text(
            text = "Темы меняют палитру приложения. Купленная тема остаётся доступной навсегда.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { item ->
                    ThemeShopCard(
                        item = item,
                        isUnlocked = item.style in unlockedThemeStyles,
                        isSelected = item.style == selectedStyle,
                        availableXp = availableXp,
                        onSelect = { onSelect(item.style) },
                        onPurchaseRequest = { onPurchaseRequest(item) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ThemeShopCard(
    item: ThemeShopItem,
    isUnlocked: Boolean,
    isSelected: Boolean,
    availableXp: Int,
    onSelect: () -> Unit,
    onPurchaseRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemePreview(item = item)
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (item.price == 0) "Бесплатно" else "${item.price} XP",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            when {
                isUnlocked -> {
                    OutlinedButton(
                        onClick = onSelect,
                        enabled = !isSelected,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = if (isSelected) "Выбрано" else "Выбрать")
                    }
                }
                availableXp >= item.price -> {
                    Button(
                        onClick = onPurchaseRequest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Купить")
                    }
                }
                else -> {
                    OutlinedButton(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Не хватает XP")
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemePreview(item: ThemeShopItem) {
    val preview = item.preview
    Surface(
        color = preview.background,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = preview.card,
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(18.dp),
                        color = preview.primary,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {}
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(8.dp),
                            color = preview.primary.copy(alpha = 0.8f),
                            shape = MaterialTheme.shapes.extraSmall
                        ) {}
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(0.45f)
                                .height(7.dp),
                            color = preview.secondary.copy(alpha = 0.75f),
                            shape = MaterialTheme.shapes.extraSmall
                        ) {}
                    }
                }
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
                color = preview.primary,
                shape = MaterialTheme.shapes.extraSmall
            ) {}
        }
    }
}

@Composable
private fun ConfirmThemePurchaseDialog(
    item: ThemeShopItem,
    availableXp: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Купить тему?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ThemePreview(item = item)
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Стоимость: ${item.price} XP. После покупки останется ${(availableXp - item.price).coerceAtLeast(0)} XP.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = availableXp >= item.price
            ) {
                Text(text = "Купить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Отмена")
            }
        }
    )
}

private data class ThemeShopItem(
    val style: AppThemeStyle,
    val title: String,
    val description: String,
    val price: Int,
    val preview: ThemePreviewColors
)

private data class ThemePreviewColors(
    val background: Color,
    val card: Color,
    val primary: Color,
    val secondary: Color
)

private fun themeShopItems(): List<ThemeShopItem> {
    return listOf(
        ThemeShopItem(
            style = AppThemeStyle.CLASSIC,
            title = "Классика",
            description = "Базовое оформление TimeQuest.",
            price = 0,
            preview = ThemePreviewColors(
                background = Color(0xFFF6F8FB),
                card = Color.White,
                primary = Color(0xFF276EF1),
                secondary = Color(0xFF3DBA84)
            )
        ),
        ThemeShopItem(
            style = AppThemeStyle.FOREST,
            title = "Лесной фокус",
            description = "Зелёные акценты для спокойного планирования.",
            price = 80,
            preview = ThemePreviewColors(
                background = Color(0xFFEAF5EF),
                card = Color(0xFFFFFFFF),
                primary = Color(0xFF207A4C),
                secondary = Color(0xFF5C7F2B)
            )
        ),
        ThemeShopItem(
            style = AppThemeStyle.SUNSET,
            title = "Закатный рывок",
            description = "Тёплые акценты для вечерней работы.",
            price = 120,
            preview = ThemePreviewColors(
                background = Color(0xFFFFF0E7),
                card = Color(0xFFFFFFFF),
                primary = Color(0xFFC65332),
                secondary = Color(0xFF8A5A00)
            )
        ),
        ThemeShopItem(
            style = AppThemeStyle.COSMOS,
            title = "Космос",
            description = "Холодные акценты для долгих серий.",
            price = 180,
            preview = ThemePreviewColors(
                background = Color(0xFFEDE9FF),
                card = Color(0xFFFFFFFF),
                primary = Color(0xFF5A4ACB),
                secondary = Color(0xFF087B91)
            )
        ),
        ThemeShopItem(
            style = AppThemeStyle.OCEAN,
            title = "Океан",
            description = "Синие и бирюзовые акценты для спокойного темпа.",
            price = 140,
            preview = ThemePreviewColors(
                background = Color(0xFFE4F4FF),
                card = Color(0xFFFFFFFF),
                primary = Color(0xFF086CA8),
                secondary = Color(0xFF00796B)
            )
        ),
        ThemeShopItem(
            style = AppThemeStyle.SAKURA,
            title = "Сакура",
            description = "Мягкие розовые акценты для лёгкого режима.",
            price = 160,
            preview = ThemePreviewColors(
                background = Color(0xFFFFEEF4),
                card = Color(0xFFFFFFFF),
                primary = Color(0xFFB83268),
                secondary = Color(0xFF9A5A00)
            )
        ),
        ThemeShopItem(
            style = AppThemeStyle.GRAPHITE,
            title = "Графит",
            description = "Сдержанная нейтральная палитра без ярких цветов.",
            price = 220,
            preview = ThemePreviewColors(
                background = Color(0xFFE9EDF2),
                card = Color(0xFFFFFFFF),
                primary = Color(0xFF3F4A56),
                secondary = Color(0xFF66717F)
            )
        )
    )
}

@Composable
private fun ProfileSettingsDialog(
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit,
    themeStyle: AppThemeStyle,
    unlockedThemeStyles: Set<AppThemeStyle>,
    onThemeStyleChange: (AppThemeStyle) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Настройки") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Тема приложения",
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
                    text = "Оформление",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                themeShopItems()
                    .filter { item -> item.style in unlockedThemeStyles }
                    .forEach { item ->
                        ThemeOptionRow(
                            title = item.title,
                            selected = themeStyle == item.style,
                            onClick = { onThemeStyleChange(item.style) }
                        )
                    }
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
