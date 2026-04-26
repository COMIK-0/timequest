package com.example.timequest

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.timequest.data.local.AppDatabase
import com.example.timequest.data.repository.TaskRepository
import com.example.timequest.navigation.TimeQuestApp
import com.example.timequest.notifications.TaskReminderScheduler
import com.example.timequest.presentation.tasks.TaskViewModel
import com.example.timequest.ui.theme.TimeQuestTheme

class MainActivity : ComponentActivity() {
    private val notificationPrefs by lazy {
        getSharedPreferences("notification_permission", MODE_PRIVATE)
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        notificationPrefs.edit().putBoolean(KEY_PERMISSION_REQUESTED, true).apply()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TaskReminderScheduler.createNotificationChannel(this)
        TaskReminderScheduler.ensureExactAlarmPermission(this)
        TaskReminderScheduler.scheduleDailyPlan(this)
        requestNotificationPermissionIfNeeded()

        val repository = TaskRepository(
            AppDatabase.getDatabase(applicationContext).taskDao()
        )

        setContent {
            val taskViewModel: TaskViewModel = viewModel(
                factory = TaskViewModel.Factory(repository)
            )

            TimeQuestTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TimeQuestApp(taskViewModel = taskViewModel)
                }
            }
        }
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
    }
}
