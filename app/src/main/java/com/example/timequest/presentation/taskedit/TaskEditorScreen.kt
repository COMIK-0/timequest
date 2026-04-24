package com.example.timequest.presentation.taskedit

import androidx.compose.runtime.Composable
import com.example.timequest.presentation.components.ScreenPlaceholder

@Composable
fun TaskEditorScreen(
    onNavigateBack: () -> Unit
) {
    ScreenPlaceholder(
        title = "Task Editor",
        subtitle = "Task creation and editing form will be placed here.",
        buttonText = "Back",
        onButtonClick = onNavigateBack
    )
}
