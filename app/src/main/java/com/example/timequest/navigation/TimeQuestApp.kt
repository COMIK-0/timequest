package com.example.timequest.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.timequest.presentation.components.TimeQuestBottomBar
import com.example.timequest.presentation.tasks.FocusSessionUiState
import com.example.timequest.presentation.tasks.TaskViewModel
import com.example.timequest.ui.theme.AppThemeMode
import com.example.timequest.ui.theme.AppThemeStyle
import kotlinx.coroutines.delay

@Composable
fun TimeQuestApp(
    taskViewModel: TaskViewModel,
    focusOpenRequest: Int,
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
    val navController = rememberNavController()
    val navigationGuardState = remember { NavigationGuardState() }
    val focusSession by taskViewModel.focusSession.collectAsState()
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val currentBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = currentBackStackEntry?.destination?.route
    val selectedBottomRoute = if (currentRoute == AppDestination.TaskEditor.routeWithArg) {
        currentBackStackEntry.arguments?.getString(AppDestination.TaskEditor.returnToArg)
    } else {
        currentRoute
    }
    val bottomRoutes = setOf(
        AppDestination.Dashboard.route,
        AppDestination.Tasks.route,
        AppDestination.Calendar.route,
        AppDestination.Profile.route,
        AppDestination.DayPlanner.route,
        AppDestination.TaskEditor.routeWithArg
    )
    val showFocusShortcut = focusSession != null &&
        currentRoute != AppDestination.FocusQuest.route &&
        currentRoute != AppDestination.TaskEditor.routeWithArg

    LaunchedEffect(focusSession?.taskId, focusSession?.isRunning) {
        while (focusSession != null) {
            nowMillis = System.currentTimeMillis()
            delay(1_000L)
        }
    }

    LaunchedEffect(focusOpenRequest) {
        if (focusOpenRequest > 0) {
            navController.navigate(AppDestination.FocusQuest.route) {
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (currentRoute in bottomRoutes) {
                TimeQuestBottomBar(
                    navController = navController,
                    selectedRouteOverride = selectedBottomRoute,
                    onNavigationRequested = navigationGuardState::requestNavigation
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            TimeQuestNavHost(
                navController = navController,
                innerPadding = innerPadding,
                taskViewModel = taskViewModel,
                navigationGuardState = navigationGuardState,
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                themeStyle = themeStyle,
                unlockedThemeStyles = unlockedThemeStyles,
                spentXp = spentXp,
                onThemeStyleChange = onThemeStyleChange,
                onThemeStylePurchase = onThemeStylePurchase,
                morningNotificationEnabled = morningNotificationEnabled,
                eveningNotificationEnabled = eveningNotificationEnabled,
                taskNotificationsEnabled = taskNotificationsEnabled,
                onMorningNotificationChange = onMorningNotificationChange,
                onEveningNotificationChange = onEveningNotificationChange,
                onTaskNotificationsChange = onTaskNotificationsChange
            )
            if (showFocusShortcut) {
                FocusShortcutButton(
                    session = focusSession,
                    nowMillis = nowMillis,
                    onClick = {
                        navController.navigate(AppDestination.FocusQuest.route) {
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(innerPadding)
                        .padding(start = 16.dp, bottom = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun FocusShortcutButton(
    session: FocusSessionUiState?,
    nowMillis: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val remainingSeconds = session?.remainingSeconds(nowMillis) ?: 0
    val label = if (session?.isRunning == true) {
        formatTimer(remainingSeconds)
    } else {
        "Пауза ${formatTimer(remainingSeconds)}"
    }
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Box(
            modifier = Modifier.padding(end = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Timer,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(text = label)
    }
}

private fun FocusSessionUiState.remainingSeconds(nowMillis: Long): Int {
    return if (isRunning && endAtMillis != null) {
        ((endAtMillis - nowMillis) / 1_000L).toInt().coerceAtLeast(0)
    } else {
        remainingSeconds.coerceAtLeast(0)
    }
}

private fun formatTimer(seconds: Int): String {
    val safeSeconds = seconds.coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val secondsPart = safeSeconds % 60
    return "%02d:%02d".format(minutes, secondsPart)
}

typealias NavigationGuardHandler = (action: () -> Unit) -> Unit

class NavigationGuardState {
    private var handler by mutableStateOf<NavigationGuardHandler?>(null)

    fun updateHandler(newHandler: NavigationGuardHandler?) {
        handler = newHandler
    }

    fun requestNavigation(action: () -> Unit) {
        handler?.invoke(action) ?: action()
    }
}
