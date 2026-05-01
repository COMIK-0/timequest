package com.example.timequest.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.timequest.presentation.calendar.CalendarScreen
import com.example.timequest.presentation.dashboard.DashboardScreen
import com.example.timequest.presentation.dayplanner.DayPlannerScreen
import com.example.timequest.presentation.focus.FocusQuestScreen
import com.example.timequest.presentation.profile.ProfileScreen
import com.example.timequest.presentation.taskedit.AddTaskWizardScreen
import com.example.timequest.presentation.tasks.TaskViewModel
import com.example.timequest.presentation.tasks.TasksScreen
import com.example.timequest.ui.theme.AppThemeMode
import com.example.timequest.ui.theme.AppThemeStyle

@Composable
fun TimeQuestNavHost(
    navController: NavHostController,
    innerPadding: PaddingValues,
    taskViewModel: TaskViewModel,
    navigationGuardState: NavigationGuardState,
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
    NavHost(
        navController = navController,
        startDestination = AppDestination.Dashboard.route,
        modifier = Modifier
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding)
    ) {
        composable(AppDestination.Dashboard.route) {
            DashboardScreen(
                taskViewModel = taskViewModel,
                onOpenTasks = {
                    navController.navigate(AppDestination.Tasks.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = false
                        }
                        launchSingleTop = true
                        restoreState = false
                    }
                },
                onAddTask = {
                    navController.navigate(
                        AppDestination.TaskEditor.createRoute(returnTo = AppDestination.Dashboard.route)
                    ) {
                        launchSingleTop = true
                    }
                },
                onOpenDayPlanner = { dateMillis ->
                    navController.navigate(AppDestination.DayPlanner.createRoute(dateMillis)) {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(
            route = AppDestination.DayPlanner.routeWithArg,
            arguments = listOf(
                navArgument(AppDestination.DayPlanner.dateMillisArg) {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val initialDateMillis = backStackEntry.arguments
                ?.getLong(AppDestination.DayPlanner.dateMillisArg)
                ?.takeIf { it != -1L }

            DayPlannerScreen(
                taskViewModel = taskViewModel,
                initialDateMillis = initialDateMillis,
                onNavigateBack = { navController.popBackStack() },
                onAddTaskForDate = { dateMillis ->
                    navController.navigate(
                        AppDestination.TaskEditor.createRoute(
                            dueDateMillis = dateMillis,
                            returnTo = AppDestination.DayPlanner.route
                        )
                    )
                },
                onPlanSaved = {
                    navController.popBackStack(
                        route = AppDestination.Dashboard.route,
                        inclusive = false
                    )
                }
            )
        }
        composable(AppDestination.Tasks.route) {
            TasksScreen(
                taskViewModel = taskViewModel,
                onAddTask = {
                    navController.navigate(
                        AppDestination.TaskEditor.createRoute(returnTo = AppDestination.Tasks.route)
                    )
                },
                onEditTask = { taskId ->
                    navController.navigate(
                        AppDestination.TaskEditor.createRoute(
                            taskId = taskId,
                            returnTo = AppDestination.Tasks.route
                        )
                    )
                },
                onStartFocus = {
                    navController.navigate(AppDestination.FocusQuest.route) {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(AppDestination.FocusQuest.route) {
            FocusQuestScreen(
                taskViewModel = taskViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = AppDestination.TaskEditor.routeWithArg,
            arguments = listOf(
                navArgument(AppDestination.TaskEditor.taskIdArg) {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument(AppDestination.TaskEditor.dueDateArg) {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument(AppDestination.TaskEditor.returnToArg) {
                    type = NavType.StringType
                    defaultValue = AppDestination.Tasks.route
                }
            )
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getLong(AppDestination.TaskEditor.taskIdArg)
                ?.takeIf { it != -1L }
            val dueDate = backStackEntry.arguments?.getLong(AppDestination.TaskEditor.dueDateArg)
                ?.takeIf { it != -1L }
            val returnTo = backStackEntry.arguments?.getString(AppDestination.TaskEditor.returnToArg)
                ?: AppDestination.Tasks.route

            AddTaskWizardScreen(
                taskId = taskId,
                initialDueDateMillis = dueDate,
                taskViewModel = taskViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigationGuardChanged = navigationGuardState::updateHandler,
                onTaskSaved = {
                    if (returnTo == AppDestination.DayPlanner.route) {
                        navController.popBackStack()
                    } else {
                        val returned = navController.popBackStack(
                            route = returnTo,
                            inclusive = false
                        )
                        if (!returned) {
                            navController.navigate(returnTo) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = false
                                }
                                launchSingleTop = true
                                restoreState = false
                            }
                        }
                    }
                }
            )
        }
        composable(AppDestination.Calendar.route) {
            CalendarScreen(
                taskViewModel = taskViewModel,
                onAddTaskForDate = { dateMillis ->
                    navController.navigate(
                        AppDestination.TaskEditor.createRoute(
                            dueDateMillis = dateMillis,
                            returnTo = AppDestination.Calendar.route
                        )
                    )
                },
                onOpenDayPlanner = { dateMillis ->
                    navController.navigate(AppDestination.DayPlanner.createRoute(dateMillis)) {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(AppDestination.Profile.route) {
            ProfileScreen(
                taskViewModel = taskViewModel,
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
        }
    }
}
