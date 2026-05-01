package com.example.timequest.presentation.theme_shop

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.timequest.domain.ThemeShopItem

@Composable
fun ThemeShopCard(
    item: ThemeShopItem,
    isUnlocked: Boolean,
    isSelected: Boolean,
    availableXp: Int,
    onSelect: () -> Unit,
    onPurchaseRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
        },
        label = "theme card color"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "theme card border"
    )

    Surface(
        modifier = modifier.animateContentSize(),
        color = containerColor,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, borderColor)
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (item.price == 0) "Бесплатно" else "${item.price} XP",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                AnimatedContent(
                    targetState = themeStatusText(isUnlocked, isSelected, availableXp, item.price),
                    label = "theme status"
                ) { status ->
                    Text(
                        text = status,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            when {
                isUnlocked -> {
                    OutlinedButton(
                        onClick = onSelect,
                        enabled = !isSelected,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = if (isSelected) "Выбрано" else "Открыто")
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
fun ThemePreview(item: ThemeShopItem, modifier: Modifier = Modifier) {
    val preview = item.preview
    Surface(
        modifier = modifier,
        color = Color(preview.background),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(preview.card),
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(18.dp),
                        color = Color(preview.primary),
                        shape = MaterialTheme.shapes.extraSmall
                    ) {}
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(8.dp),
                            color = Color(preview.primary).copy(alpha = 0.8f),
                            shape = MaterialTheme.shapes.extraSmall
                        ) {}
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(0.45f)
                                .height(7.dp),
                            color = Color(preview.secondary).copy(alpha = 0.75f),
                            shape = MaterialTheme.shapes.extraSmall
                        ) {}
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(12.dp),
                    color = Color(preview.primary),
                    shape = MaterialTheme.shapes.extraSmall
                ) {}
                Surface(
                    modifier = Modifier
                        .weight(0.65f)
                        .height(12.dp),
                    color = Color(preview.secondary),
                    shape = MaterialTheme.shapes.extraSmall
                ) {}
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .background(
                        color = Color(preview.card).copy(alpha = 0.72f),
                        shape = MaterialTheme.shapes.small
                    )
            )
        }
    }
}

private fun themeStatusText(
    isUnlocked: Boolean,
    isSelected: Boolean,
    availableXp: Int,
    price: Int
): String {
    return when {
        isSelected -> "Выбрано"
        isUnlocked -> "Открыто"
        availableXp >= price -> "Купить"
        else -> "Не хватает XP"
    }
}
