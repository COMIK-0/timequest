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
        const val dueDateArg = "dueDate"
        const val returnToArg = "returnTo"
        const val routeWithArg = "$route?$taskIdArg={$taskIdArg}&$dueDateArg={$dueDateArg}&$returnToArg={$returnToArg}"

        fun createRoute(
            taskId: Long? = null,
            dueDateMillis: Long? = null,
            returnTo: String = Tasks.route
        ): String {
            val taskPart = taskId ?: -1L
            val dueDatePart = dueDateMillis ?: -1L
            return "$route?$taskIdArg=$taskPart&$dueDateArg=$dueDatePart&$returnToArg=$returnTo"
        }
    }

    object FocusQuest {
        const val route = "focus_quest"
    }
}
