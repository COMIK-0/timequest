package com.example.timequest.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

object TaskReminderScheduler {
    const val ACTION_TASK_DEADLINE = "com.example.timequest.ACTION_TASK_DEADLINE"
    const val ACTION_DAILY_PLAN = "com.example.timequest.ACTION_DAILY_PLAN"
    const val EXTRA_TASK_ID = "task_id"
    const val CHANNEL_ID = "task_reminders"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Напоминания о задачах",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun scheduleTaskDeadline(context: Context, taskId: Long, dueDate: Long?) {
        if (dueDate == null) return

        val triggerAt = Instant.ofEpochMilli(dueDate)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .atTime(LocalTime.of(9, 0))
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        if (triggerAt <= System.currentTimeMillis()) return

        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = ACTION_TASK_DEADLINE
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }

    fun scheduleDailyPlan(context: Context) {
        val now = LocalDate.now().atTime(LocalTime.now())
        var nextTime = LocalDate.now().atTime(LocalTime.of(9, 0))
        if (!nextTime.isAfter(now)) {
            nextTime = nextTime.plusDays(1)
        }

        val triggerAt = nextTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = ACTION_DAILY_PLAN
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            10_001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }
}
