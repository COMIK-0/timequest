package com.example.timequest.notifications

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.timequest.R
import com.example.timequest.data.local.AppDatabase
import com.example.timequest.domain.TaskPrioritizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                TaskReminderScheduler.createNotificationChannel(context)
                when (intent.action) {
                    TaskReminderScheduler.ACTION_TASK_DEADLINE -> showTaskDeadline(context, intent)
                    TaskReminderScheduler.ACTION_DAILY_PLAN -> showDailyPlan(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun showTaskDeadline(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(TaskReminderScheduler.EXTRA_TASK_ID, -1L)
        if (taskId == -1L) return
        val task = AppDatabase.getDatabase(context).taskDao().getTaskById(taskId) ?: return
        if (task.isCompleted) return
        if (task.dueDate?.isToday() != true) return

        showNotification(
            context = context,
            notificationId = task.id.toInt(),
            title = context.getString(R.string.notification_deadline_title),
            text = task.title
        )
    }

    private suspend fun showDailyPlan(context: Context) {
        val activeTasks = AppDatabase.getDatabase(context).taskDao().getActiveTasks().first()
        val todayTasks = activeTasks.filter { task ->
            task.dueDate?.isToday() == true
        }
        if (todayTasks.isEmpty()) return

        val topTask = TaskPrioritizer.sortSmart(todayTasks).first()
        showNotification(
            context = context,
            notificationId = 10_001,
            title = context.getString(R.string.notification_daily_title),
            text = context.getString(R.string.notification_daily_text, todayTasks.size, topTask.title)
        )
    }

    private fun showNotification(
        context: Context,
        notificationId: Int,
        title: String,
        text: String
    ) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notification = NotificationCompat.Builder(context, TaskReminderScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun Long.isToday(): Boolean {
        return Instant.ofEpochMilli(this)
            .atZone(ZoneId.systemDefault())
            .toLocalDate() == LocalDate.now()
    }
}
