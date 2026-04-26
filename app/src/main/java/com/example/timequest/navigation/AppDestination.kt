package com.example.timequest.navigation

sealed class AppDestination(
    val route: String,
    val label: String
) {
    data object Dashboard : AppDestination("dashboard", "Главная")
    data object Tasks : AppDestination("tasks", "Задачи")
    data object Calendar : AppDestination("calendar", "Календарь")
    data object Profile : AppDestination("profile", "Профиль")

    object DayPlanner {
        const val route = "day_planner"
        const val dateMillisArg = "dateMillis"
        const val routeWithArg = "$route?$dateMillisArg={$dateMillisArg}"

        fun createRoute(dateMillis: Long? = null): String {
            return if (dateMillis != null) {
                "$route?$dateMillisArg=$dateMillis"
            } else {
                route
            }
        }
    }

    object TaskEditor {
        const val route = "task_editor"
        const val taskIdArg = "taskId"
        const val routeWithArg = "$route?$taskIdArg={$taskIdArg}"

        fun createRoute(taskId: Long? = null): String {
            return if (taskId != null) {
                "$route?$taskIdArg=$taskId"
            } else {
                route
            }
        }
    }

    object FocusQuest {
        const val route = "focus_quest"
    }
}
