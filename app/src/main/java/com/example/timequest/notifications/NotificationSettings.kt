package com.example.timequest.notifications

import android.content.Context

object NotificationSettings {
    private const val PREFS_NAME = "timequest_notification_settings"
    private const val KEY_MORNING = "morning_notification_enabled"
    private const val KEY_EVENING = "evening_notification_enabled"
    private const val KEY_TASKS = "task_notifications_enabled"

    // TODO: Move notification settings to DataStore Preferences together with theme settings.

    fun isMorningEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_MORNING, true)
    }

    fun setMorningEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_MORNING, enabled).apply()
    }

    fun isEveningEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_EVENING, true)
    }

    fun setEveningEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_EVENING, enabled).apply()
    }

    fun areTaskNotificationsEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_TASKS, true)
    }

    fun setTaskNotificationsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_TASKS, enabled).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
