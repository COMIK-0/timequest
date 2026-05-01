package com.example.timequest.domain

import com.example.timequest.ui.theme.AppThemeStyle

data class ThemeShopItem(
    val style: AppThemeStyle,
    val title: String,
    val description: String,
    val price: Int,
    val preview: ThemePreviewPalette
)

data class ThemePreviewPalette(
    val background: Long,
    val card: Long,
    val primary: Long,
    val secondary: Long
)

object ThemeShopCatalog {
    val items = listOf(
        ThemeShopItem(
            style = AppThemeStyle.CLASSIC,
            title = "Классика",
            description = "Базовое оформление TimeQuest.",
            price = 0,
            preview = ThemePreviewPalette(
                background = 0xFFF6F8FB,
                card = 0xFFFFFFFF,
                primary = 0xFF276EF1,
                secondary = 0xFF3DBA84
            )
        ),
        ThemeShopItem(
            style = AppThemeStyle.FOREST,
            title = "Лесной фокус",
            description = "Зелёные акценты для спокойного планирования.",
            price = 80,
            preview = ThemePreviewPalette(
                background = 0xFFEAF5EF,
                card = 0xFFFFFFFF,
                primary = 0xFF207A4C,
                secondary = 0xFF5C7F2B
            )
        ),
        ThemeShopItem(
            style = AppThemeStyle.SUNSET,
            title = "Закатный рывок",
            description = "Тёплые акценты для вечерней работы.",
            price = 120,
            preview = ThemePreviewPalette(
                background = 0xFFFFF0E7,
                card = 0xFFFFFFFF,
                primary = 0xFFC65332,
                secondary = 0xFF8A5A00
            )
        ),
        ThemeShopItem(
            style = AppThemeStyle.COSMOS,
            title = "Космос",
            description = "Холодные акценты для долгих серий.",
            price = 180,
            preview = ThemePreviewPalette(
                background = 0xFFEDE9FF,
                card = 0xFFFFFFFF,
                primary = 0xFF5A4ACB,
                secondary = 0xFF087B91
            )
        ),
        ThemeShopItem(
            style = AppThemeStyle.OCEAN,
            title = "Океан",
            description = "Синие и бирюзовые акценты для спокойного темпа.",
            price = 140,
            preview = ThemePreviewPalette(
                background = 0xFFE4F4FF,
                card = 0xFFFFFFFF,
                primary = 0xFF086CA8,
                secondary = 0xFF00796B
            )
        ),
        ThemeShopItem(
            style = AppThemeStyle.SAKURA,
            title = "Сакура",
            description = "Мягкие розовые акценты для лёгкого режима.",
            price = 160,
            preview = ThemePreviewPalette(
                background = 0xFFFFEEF4,
                card = 0xFFFFFFFF,
                primary = 0xFFB83268,
                secondary = 0xFF9A5A00
            )
        ),
        ThemeShopItem(
            style = AppThemeStyle.GRAPHITE,
            title = "Графит",
            description = "Сдержанная нейтральная палитра без ярких цветов.",
            price = 220,
            preview = ThemePreviewPalette(
                background = 0xFFE9EDF2,
                card = 0xFFFFFFFF,
                primary = 0xFF3F4A56,
                secondary = 0xFF66717F
            )
        )
    )
}
