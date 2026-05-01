package com.example.timequest.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.timequest.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                TaskReminderScheduler.createNotificationChannel(context)
                val activeTasks = AppDatabase.getDatabase(context).taskDao().getActiveTasks().first()
                activeTasks.forEach { task ->
                    TaskReminderScheduler.scheduleTaskDeadline(
                        context = context,
                        taskId = task.id,
                        dueDate = task.dueDate,
                        scheduledStartTime = task.scheduledStartTime
                    )
                }
                TaskReminderScheduler.scheduleDailyNotifications(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
