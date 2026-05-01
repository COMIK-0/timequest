package com.example.timequest

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.timequest.data.local.AppDatabase
import com.example.timequest.data.repository.TaskRepository
import com.example.timequest.navigation.TimeQuestApp
import com.example.timequest.notifications.NotificationSettings
import com.example.timequest.notifications.TaskReminderScheduler
import com.example.timequest.presentation.tasks.TaskViewModel
import com.example.timequest.ui.theme.AppThemeMode
import com.example.timequest.ui.theme.AppThemeStyle
import com.example.timequest.ui.theme.TimeQuestTheme

class MainActivity : ComponentActivity() {
    private var focusOpenRequest by mutableIntStateOf(0)

    private val notificationPrefs by lazy {
        getSharedPreferences("notification_permission", MODE_PRIVATE)
    }
    private val settingsPrefs by lazy {
        // TODO: Move theme settings to DataStore Preferences when the project is ready for a small persistence migration.
        getSharedPreferences("timequest_settings", MODE_PRIVATE)
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        notificationPrefs.edit().putBoolean(KEY_PERMISSION_REQUESTED, true).apply()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleFocusIntent(intent)
        TaskReminderScheduler.createNotificationChannel(this)
        TaskReminderScheduler.ensureExactAlarmPermission(this)
        TaskReminderScheduler.scheduleDailyNotifications(this)
        requestNotificationPermissionIfNeeded()

        val repository = TaskRepository(
            AppDatabase.getDatabase(applicationContext).taskDao()
        )

        setContent {
            val taskViewModel: TaskViewModel = viewModel(
                factory = TaskViewModel.Factory(repository)
            )
            var themeMode by remember { mutableStateOf(readThemeMode()) }
            var themeStyle by remember { mutableStateOf(readThemeStyle()) }
            var unlockedThemeStyles by remember { mutableStateOf(readUnlockedThemeStyles()) }
            var spentXp by remember { mutableStateOf(readSpentXp()) }
            var morningNotificationEnabled by remember {
                mutableStateOf(NotificationSettings.isMorningEnabled(this@MainActivity))
            }
            var eveningNotificationEnabled by remember {
                mutableStateOf(NotificationSettings.isEveningEnabled(this@MainActivity))
            }
            var taskNotificationsEnabled by remember {
                mutableStateOf(NotificationSettings.areTaskNotificationsEnabled(this@MainActivity))
            }

            TimeQuestTheme(
                themeMode = themeMode,
                themeStyle = themeStyle
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TimeQuestApp(
                        taskViewModel = taskViewModel,
                        focusOpenRequest = focusOpenRequest,
                        themeMode = themeMode,
                        onThemeModeChange = { newMode ->
                            themeMode = newMode
                            saveThemeMode(newMode)
                        },
                        themeStyle = themeStyle,
                        unlockedThemeStyles = unlockedThemeStyles,
                        spentXp = spentXp,
                        onThemeStyleChange = { newStyle ->
                            if (newStyle in unlockedThemeStyles) {
                                themeStyle = newStyle
                                saveThemeStyle(newStyle)
                            }
                        },
                        onThemeStylePurchase = { newStyle, price ->
                            if (newStyle !in unlockedThemeStyles) {
                                val updatedStyles = unlockedThemeStyles + newStyle
                                val updatedSpentXp = spentXp + price
                                unlockedThemeStyles = updatedStyles
                                spentXp = updatedSpentXp
                                themeStyle = newStyle
                                saveUnlockedThemeStyles(updatedStyles)
                                saveSpentXp(updatedSpentXp)
                                saveThemeStyle(newStyle)
                            }
                        },
                        morningNotificationEnabled = morningNotificationEnabled,
                        eveningNotificationEnabled = eveningNotificationEnabled,
                        taskNotificationsEnabled = taskNotificationsEnabled,
                        onMorningNotificationChange = { enabled ->
                            morningNotificationEnabled = enabled
                            NotificationSettings.setMorningEnabled(this@MainActivity, enabled)
                            if (enabled) {
                                TaskReminderScheduler.scheduleDailyNotifications(this@MainActivity)
                            } else {
                                TaskReminderScheduler.cancelMorningNotification(this@MainActivity)
                            }
                        },
                        onEveningNotificationChange = { enabled ->
                            eveningNotificationEnabled = enabled
                            NotificationSettings.setEveningEnabled(this@MainActivity, enabled)
                            if (enabled) {
                                TaskReminderScheduler.scheduleDailyNotifications(this@MainActivity)
                            } else {
                                TaskReminderScheduler.cancelEveningNotification(this@MainActivity)
                            }
                        },
                        onTaskNotificationsChange = { enabled ->
                            taskNotificationsEnabled = enabled
                            NotificationSettings.setTaskNotificationsEnabled(this@MainActivity, enabled)
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleFocusIntent(intent)
    }

    private fun handleFocusIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(TaskReminderScheduler.EXTRA_OPEN_FOCUS, false) == true) {
            focusOpenRequest += 1
        }
    }

    private fun readThemeMode(): AppThemeMode {
        val savedName = settingsPrefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name)
        return AppThemeMode.entries.firstOrNull { mode -> mode.name == savedName } ?: AppThemeMode.SYSTEM
    }

    private fun saveThemeMode(mode: AppThemeMode) {
        settingsPrefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    private fun readThemeStyle(): AppThemeStyle {
        val savedName = settingsPrefs.getString(KEY_THEME_STYLE, AppThemeStyle.CLASSIC.name)
        val savedStyle = AppThemeStyle.entries.firstOrNull { style -> style.name == savedName }
            ?: AppThemeStyle.CLASSIC
        return if (savedStyle in readUnlockedThemeStyles()) savedStyle else AppThemeStyle.CLASSIC
    }

    private fun saveThemeStyle(style: AppThemeStyle) {
        settingsPrefs.edit().putString(KEY_THEME_STYLE, style.name).apply()
    }

    private fun readUnlockedThemeStyles(): Set<AppThemeStyle> {
        val savedNames = settingsPrefs.getStringSet(KEY_UNLOCKED_THEME_STYLES, emptySet()).orEmpty()
        return savedNames
            .mapNotNull { name -> AppThemeStyle.entries.firstOrNull { style -> style.name == name } }
            .toSet() + AppThemeStyle.CLASSIC
    }

    private fun saveUnlockedThemeStyles(styles: Set<AppThemeStyle>) {
        settingsPrefs.edit()
            .putStringSet(KEY_UNLOCKED_THEME_STYLES, styles.map { it.name }.toSet())
            .apply()
    }

    private fun readSpentXp(): Int {
        return settingsPrefs.getInt(KEY_SPENT_XP, 0).coerceAtLeast(0)
    }

    private fun saveSpentXp(value: Int) {
        settingsPrefs.edit().putInt(KEY_SPENT_XP, value.coerceAtLeast(0)).apply()
    }

    private fun requestNotificationPermissionIfNeeded() {
        val alreadyRequested = notificationPrefs.getBoolean(KEY_PERMISSION_REQUESTED, false)
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            !alreadyRequested
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    companion object {
        private const val KEY_PERMISSION_REQUESTED = "post_notifications_requested"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_THEME_STYLE = "theme_style"
        private const val KEY_UNLOCKED_THEME_STYLES = "unlocked_theme_styles"
        private const val KEY_SPENT_XP = "spent_xp"
    }
}
