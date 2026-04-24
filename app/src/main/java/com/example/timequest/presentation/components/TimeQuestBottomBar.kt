package com.example.timequest.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.timequest.R
import com.example.timequest.navigation.AppDestination

@Composable
fun TimeQuestBottomBar(navController: NavHostController) {
    val currentBackStack = navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStack.value?.destination
    val items = listOf(
        BottomBarItem(
            destination = AppDestination.Dashboard,
            icon = Icons.Outlined.Dashboard,
            labelRes = R.string.dashboard_title
        ),
        BottomBarItem(
            destination = AppDestination.Tasks,
            icon = Icons.Outlined.TaskAlt,
            labelRes = R.string.tasks_title
        ),
        BottomBarItem(
            destination = AppDestination.Statistics,
            icon = Icons.Outlined.BarChart,
            labelRes = R.string.statistics_title
        ),
        BottomBarItem(
            destination = AppDestination.Profile,
            icon = Icons.Outlined.Person,
            labelRes = R.string.profile_title
        )
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        items.forEach { item ->
            val selected = currentDestination
                ?.hierarchy
                ?.any { it.route == item.destination.route } == true

            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(item.destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = false
                            }
                            launchSingleTop = true
                            restoreState = false
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = stringResource(item.labelRes)
                    )
                },
                label = {
                    Text(text = stringResource(item.labelRes))
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                )
            )
        }
    }
}

private data class BottomBarItem(
    val destination: AppDestination,
    val icon: ImageVector,
    val labelRes: Int
)
