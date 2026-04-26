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

@Composable
fun TimeQuestNavHost(
    navController: NavHostController,
    innerPadding: PaddingValues,
    taskViewModel: TaskViewModel
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
                    navController.navigate(AppDestination.TaskEditor.createRoute()) {
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
                    navController.navigate(AppDestination.TaskEditor.createRoute())
                },
                onEditTask = { taskId ->
                    navController.navigate(AppDestination.TaskEditor.createRoute(taskId))
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
                }
            )
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getLong(AppDestination.TaskEditor.taskIdArg)
                ?.takeIf { it != -1L }

            AddTaskWizardScreen(
                taskId = taskId,
                taskViewModel = taskViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onTaskSaved = {
                    val returnedToTasks = navController.popBackStack(
                        route = AppDestination.Tasks.route,
                        inclusive = false
                    )
                    if (!returnedToTasks) {
                        navController.navigate(AppDestination.Tasks.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = false
                            }
                            launchSingleTop = true
                            restoreState = false
                        }
                    }
                }
            )
        }
        composable(AppDestination.Calendar.route) {
            CalendarScreen(taskViewModel = taskViewModel)
        }
        composable(AppDestination.Profile.route) {
            ProfileScreen(taskViewModel = taskViewModel)
        }
    }
}
