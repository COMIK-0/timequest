package com.example.timequest.navigation

import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.timequest.presentation.components.TimeQuestBottomBar
import com.example.timequest.presentation.tasks.TaskViewModel

@Composable
fun TimeQuestApp(taskViewModel: TaskViewModel) {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val bottomRoutes = setOf(
        AppDestination.Dashboard.route,
        AppDestination.Tasks.route,
        AppDestination.Calendar.route,
        AppDestination.Profile.route,
        AppDestination.DayPlanner.route
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (currentRoute in bottomRoutes) {
                TimeQuestBottomBar(navController = navController)
            }
        }
    ) { innerPadding ->
        TimeQuestNavHost(
            navController = navController,
            innerPadding = innerPadding,
            taskViewModel = taskViewModel
        )
    }
}
