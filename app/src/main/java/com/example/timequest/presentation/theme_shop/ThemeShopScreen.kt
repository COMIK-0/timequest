package com.example.timequest.presentation.theme_shop

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.timequest.domain.ThemeShopItem
import com.example.timequest.presentation.components.AppCard
import com.example.timequest.presentation.components.SoftProgress
import com.example.timequest.ui.theme.AppThemeStyle

@Composable
fun ThemeShopScreen(
    totalXp: Int,
    availableXp: Int,
    spentXp: Int,
    items: List<ThemeShopItem>,
    unlockedThemeStyles: Set<AppThemeStyle>,
    selectedStyle: AppThemeStyle,
    onBack: () -> Unit,
    onSelect: (AppThemeStyle) -> Unit,
    onPurchase: (AppThemeStyle, Int) -> Unit
) {
    var pendingPurchase by remember { mutableStateOf<ThemeShopItem?>(null) }
    var purchaseMessage by remember { mutableStateOf<String?>(null) }
    val unlockedCount = items.count { item -> item.style in unlockedThemeStyles }

    pendingPurchase?.let { item ->
        ConfirmThemePurchaseDialog(
            item = item,
            availableXp = availableXp,
            onConfirm = {
                if (item.style !in unlockedThemeStyles && availableXp >= item.price) {
                    onPurchase(item.style, item.price)
                    purchaseMessage = "Тема открыта. Осталось ${(availableXp - item.price).coerceAtLeast(0)} XP."
                }
                pendingPurchase = null
            },
            onDismiss = { pendingPurchase = null }
        )
    }

    LaunchedEffect(purchaseMessage) {
        if (purchaseMessage != null) {
            kotlinx.coroutines.delay(2600)
            purchaseMessage = null
        }
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Магазин оформления",
                style = MaterialTheme.typography.headlineSmall
            )
            TextButton(onClick = onBack) {
                Text(text = "Назад")
            }
        }

        AppCard(highlighted = true) {
            Text(
                text = "Доступно для покупок",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AnimatedContent(targetState = availableXp, label = "available xp") { xp ->
                Text(
                    text = "$xp XP",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            DetailLine("Всего заработано", "$totalXp XP")
            DetailLine("Потрачено на оформление", "$spentXp XP")
            DetailLine("Открыто оформлений", "$unlockedCount из ${items.size}")
            SoftProgress(
                progress = unlockedCount.toFloat() / items.size,
                modifier = Modifier.fillMaxWidth()
            )
        }

        AnimatedVisibility(visible = purchaseMessage != null) {
            AppCard(highlighted = true) {
                Text(
                    text = purchaseMessage.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Text(
            text = "Оформления меняют цветовую палитру приложения и покупаются за XP.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { item ->
                    val isUnlocked = item.style in unlockedThemeStyles
                    ThemeShopCard(
                        item = item,
                        isUnlocked = isUnlocked,
                        isSelected = item.style == selectedStyle,
                        availableXp = availableXp,
                        onSelect = { onSelect(item.style) },
                        onPurchaseRequest = {
                            if (!isUnlocked && availableXp >= item.price) {
                                pendingPurchase = item
                            }
                        },
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
private fun ConfirmThemePurchaseDialog(
    item: ThemeShopItem,
    availableXp: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Купить оформление?") },
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

@Composable
private fun DetailLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        AnimatedContent(targetState = value, label = "shop detail") { animatedValue ->
            Text(
                text = animatedValue,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
