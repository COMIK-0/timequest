package com.example.timequest.navigation

sealed class AppDestination(
    val route: String,
    val label: String
) {
    data object Dashboard : AppDestination("dashboard", "Главная")
    data object Tasks : AppDestination("tasks", "Задачи")
    data object Statistics : AppDestination("statistics", "Статистика")
    data object Profile : AppDestination("profile", "Профиль")

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
}
