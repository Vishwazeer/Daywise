package com.example.daywise.ui.screens.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.daywise.MainActivity
import com.example.daywise.R
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("alarm_id") ?: return
        val reminderType = intent.getStringExtra("reminder_type") ?: "Reminder"
        val message = intent.getStringExtra("message") ?: when (reminderType.lowercase()) {
            "morning" -> "Rise and shine, champion! 🌅 A new day means new gains. Open Daywise and log that delicious breakfast!"
            "afternoon" -> "Mid-day energy check! ⚡ Did you feed your goals today? Log your lunch and keep the momentum going!"
            "evening" -> "Sun is setting, but your progress isn't! 🌙 What did you eat for dinner? Got a workout in? Let's write it down."
            else -> "Your goals are waiting for you! 🌟 Log your food or exercise in Daywise to keep crushing your day!"
        }
        val timeStr = intent.getStringExtra("time_str") ?: "09:00"

        // 1. Show notification
        showNotification(context, reminderType, message)

        // 2. Reschedule for next day (24 hours later) exactly
        rescheduleNextAlarm(context, id, reminderType, timeStr, message)
    }

    private fun rescheduleNextAlarm(context: Context, id: String, label: String, timeStr: String, message: String) {
        AlarmHelper.scheduleAlarm(context, id, label, timeStr, message)
    }

    private fun showNotification(context: Context, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "daywise_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily tracking reminders"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            title.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Daywise — $title Reminder")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(title.hashCode(), builder.build())
    }
}
