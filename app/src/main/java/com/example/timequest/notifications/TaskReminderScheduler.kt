package com.example.timequest.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.timequest.MainActivity
import com.example.timequest.R
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.absoluteValue

object TaskReminderScheduler {
    const val ACTION_TASK_DEADLINE = "com.example.timequest.ACTION_TASK_DEADLINE"
    const val ACTION_DAILY_PLAN = "com.example.timequest.ACTION_DAILY_PLAN"
    const val ACTION_DEBUG_NOTIFICATION = "com.example.timequest.ACTION_DEBUG_NOTIFICATION"
    const val ACTION_FOCUS_FINISHED = "com.example.timequest.ACTION_FOCUS_FINISHED"
    const val ACTION_FOCUS_COMPLETE = "com.example.timequest.ACTION_FOCUS_COMPLETE"
    const val ACTION_FOCUS_NOT_DONE = "com.example.timequest.ACTION_FOCUS_NOT_DONE"
    const val EXTRA_TASK_ID = "task_id"
    const val EXTRA_OPEN_FOCUS = "open_focus"
    const val CHANNEL_ID = "task_reminders"
    private const val FOCUS_NOTIFICATION_ID = 40_000
    private const val FOCUS_FINISHED_NOTIFICATION_ID = 40_001
    private const val EXACT_ALARM_PREFS = "exact_alarm_permission"
    private const val KEY_EXACT_ALARM_REQUESTED = "exact_alarm_requested"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Напоминания о задачах",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun ensureExactAlarmPermission(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (alarmManager.canScheduleExactAlarms()) return

        val prefs = context.getSharedPreferences(EXACT_ALARM_PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_EXACT_ALARM_REQUESTED, false)) return

        prefs.edit().putBoolean(KEY_EXACT_ALARM_REQUESTED, true).apply()
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun scheduleTaskDeadline(
        context: Context,
        taskId: Long,
        dueDate: Long?,
        scheduledStartTime: Long? = null
    ) {
        cancelTaskReminder(context, taskId)

        val triggerAt = if (scheduledStartTime != null) {
            scheduledStartTime - 5 * 60_000L
        } else {
            if (dueDate == null) return
            Instant.ofEpochMilli(dueDate)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .atTime(LocalTime.of(9, 0))
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }

        if (triggerAt <= System.currentTimeMillis()) return

        val pendingIntent = taskReminderPendingIntent(context, taskId)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (scheduledStartTime != null) {
            if (canScheduleExactAlarm(context, alarmManager)) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    fun cancelTaskReminder(context: Context, taskId: Long) {
        val pendingIntent = taskReminderPendingIntent(context, taskId)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    fun scheduleDebugNotification(context: Context) {
        val pendingIntent = debugReminderPendingIntent(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = System.currentTimeMillis() + 30_000L
        if (canScheduleExactAlarm(context, alarmManager)) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    fun scheduleFocusFinished(context: Context, taskId: Long, endAtMillis: Long?) {
        cancelFocusFinished(context)
        if (endAtMillis == null || endAtMillis <= System.currentTimeMillis()) return

        val pendingIntent = focusFinishedPendingIntent(context, taskId)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (canScheduleExactAlarm(context, alarmManager)) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAtMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAtMillis, pendingIntent)
        }
    }

    fun cancelFocusFinished(context: Context) {
        val pendingIntent = focusFinishedPendingIntent(context, 0L)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    fun showFocusNotification(
        context: Context,
        taskTitle: String,
        endAtMillis: Long?,
        remainingSeconds: Int? = null
    ) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val safeRemainingSeconds = remainingSeconds
            ?: endAtMillis?.let { ((it - System.currentTimeMillis()) / 1_000L).toInt() }
            ?: 0
        val timerView = focusTimerRemoteViews(
            context = context,
            taskTitle = taskTitle,
            remainingSeconds = safeRemainingSeconds,
            isRunning = endAtMillis != null
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Фокус на задаче")
            .setContentText(
                if (endAtMillis != null) {
                    "$taskTitle • осталось времени"
                } else {
                    "$taskTitle • пауза: ${formatTimer(remainingSeconds ?: 0)}"
                }
            )
            .setCustomContentView(timerView)
            .setCustomBigContentView(timerView)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setContentIntent(openAppIntent(context, openFocus = true))

        NotificationManagerCompat.from(context).notify(FOCUS_NOTIFICATION_ID, builder.build())
    }

    fun cancelFocusNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(FOCUS_NOTIFICATION_ID)
    }

    fun cancelFocusFinishedNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(FOCUS_FINISHED_NOTIFICATION_ID)
    }

    fun showFocusFinishedNotification(context: Context, taskId: Long, taskTitle: String) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val text = "Фокус завершен. Отметьте результат задачи."
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Время фокуса закончилось")
            .setContentText(taskTitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$taskTitle\n$text"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppIntent(context, openFocus = true))
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.checkbox_on_background,
                "Выполнено",
                focusActionPendingIntent(context, ACTION_FOCUS_COMPLETE, taskId)
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Не выполнено",
                focusActionPendingIntent(context, ACTION_FOCUS_NOT_DONE, taskId)
            )
            .build()

        NotificationManagerCompat.from(context).notify(FOCUS_FINISHED_NOTIFICATION_ID, notification)
    }

    private fun taskReminderPendingIntent(context: Context, taskId: Long): PendingIntent {
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = ACTION_TASK_DEADLINE
            putExtra(EXTRA_TASK_ID, taskId)
        }
        return PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun debugReminderPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = ACTION_DEBUG_NOTIFICATION
        }
        return PendingIntent.getBroadcast(
            context,
            DEBUG_TASK_ID.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun focusFinishedPendingIntent(context: Context, taskId: Long): PendingIntent {
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = ACTION_FOCUS_FINISHED
            putExtra(EXTRA_TASK_ID, taskId)
        }
        return PendingIntent.getBroadcast(
            context,
            FOCUS_FINISHED_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun focusActionPendingIntent(context: Context, action: String, taskId: Long): PendingIntent {
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_TASK_ID, taskId)
        }
        return PendingIntent.getBroadcast(
            context,
            (FOCUS_FINISHED_NOTIFICATION_ID + action.hashCode()).absoluteValue,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun openAppIntent(context: Context, openFocus: Boolean = false): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_FOCUS, openFocus)
        }
        return PendingIntent.getActivity(
            context,
            20_001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun canScheduleExactAlarm(context: Context, alarmManager: AlarmManager): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
    }

    private fun focusTimerRemoteViews(
        context: Context,
        taskTitle: String,
        remainingSeconds: Int,
        isRunning: Boolean
    ): RemoteViews {
        val safeRemainingSeconds = remainingSeconds.coerceAtLeast(0)
        val baseMillis = SystemClock.elapsedRealtime() + safeRemainingSeconds * 1_000L
        return RemoteViews(context.packageName, R.layout.notification_focus_timer).apply {
            setTextViewText(R.id.focus_notification_task, taskTitle)
            setChronometer(R.id.focus_notification_timer, baseMillis, null, isRunning)
            setChronometerCountDown(R.id.focus_notification_timer, true)
        }
    }

    private fun formatTimer(seconds: Int): String {
        val safeSeconds = seconds.coerceAtLeast(0)
        val minutes = safeSeconds / 60
        val secondsPart = safeSeconds % 60
        return "%02d:%02d".format(minutes, secondsPart)
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
    private const val DEBUG_TASK_ID = -30_000L
}
