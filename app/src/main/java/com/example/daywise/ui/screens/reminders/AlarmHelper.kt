package com.example.daywise.ui.screens.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

object AlarmHelper {
    fun updateAlarm(context: Context, id: String, label: String, enabled: Boolean, timeStr: String, message: String) {
        val sharedPrefs = context.getSharedPreferences("daywise_alarms", Context.MODE_PRIVATE)
        val activeIds = sharedPrefs.getStringSet("active_ids", emptySet())?.toMutableSet() ?: mutableSetOf()

        if (!enabled) {
            cancelAlarm(context, id)
            activeIds.remove(id)
            sharedPrefs.edit().apply {
                putStringSet("active_ids", activeIds)
                remove("${id}_label")
                remove("${id}_enabled")
                remove("${id}_time")
                remove("${id}_message")
                apply()
            }
            return
        }

        // Save to preferences
        activeIds.add(id)
        sharedPrefs.edit().apply {
            putStringSet("active_ids", activeIds)
            putString("${id}_label", label)
            putBoolean("${id}_enabled", true)
            putString("${id}_time", timeStr)
            putString("${id}_message", message)
            apply()
        }

        scheduleAlarm(context, id, label, timeStr, message)
    }

    fun scheduleAlarm(context: Context, id: String, label: String, timeStr: String, message: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            putExtra("alarm_id", id)
            putExtra("reminder_type", label)
            putExtra("time_str", timeStr)
            putExtra("message", message)
        }
        val requestCode = id.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val parts = timeStr.split(":")
        if (parts.size != 2) return
        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            val showIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, com.example.daywise.MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmClockInfo = AlarmManager.AlarmClockInfo(calendar.timeInMillis, showIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            Log.d("DaywiseAlarm", "Alarm $id scheduled using setAlarmClock for $timeStr")
        } catch (e: SecurityException) {
            Log.e("DaywiseAlarm", "Failed to schedule alarm", e)
        }
    }

    private fun cancelAlarm(context: Context, id: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val requestCode = id.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d("DaywiseAlarm", "Alarm $id cancelled")
    }

    fun rescheduleAll(context: Context) {
        val sharedPrefs = context.getSharedPreferences("daywise_alarms", Context.MODE_PRIVATE)
        val activeIds = sharedPrefs.getStringSet("active_ids", emptySet()) ?: emptySet()
        Log.d("DaywiseAlarm", "Rescheduling ${activeIds.size} alarms after boot...")
        for (id in activeIds) {
            val label = sharedPrefs.getString("${id}_label", "Reminder") ?: "Reminder"
            val enabled = sharedPrefs.getBoolean("${id}_enabled", false)
            val timeStr = sharedPrefs.getString("${id}_time", null)
            val message = sharedPrefs.getString("${id}_message", null) ?: ""
            if (enabled && timeStr != null) {
                scheduleAlarm(context, id, label, timeStr, message)
            }
        }
    }
}
