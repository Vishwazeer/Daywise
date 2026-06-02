package com.example.daywise.ui.screens.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daywise.data.preferences.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class RemindersUiState(
    val morningEnabled: Boolean = true,
    val afternoonEnabled: Boolean = true,
    val eveningEnabled: Boolean = true,
    val morningTime: String = "09:00",
    val afternoonTime: String = "13:00",
    val eveningTime: String = "19:00"
)

class RemindersViewModel(
    private val context: Context,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val enabledFlow = combine(
        preferencesManager.morningReminderEnabled,
        preferencesManager.afternoonReminderEnabled,
        preferencesManager.eveningReminderEnabled
    ) { m, a, e -> Triple(m, a, e) }

    private val timeFlow = combine(
        preferencesManager.morningReminderTime,
        preferencesManager.afternoonReminderTime,
        preferencesManager.eveningReminderTime
    ) { m, a, e -> Triple(m, a, e) }

    val uiState: StateFlow<RemindersUiState> = combine(enabledFlow, timeFlow) { enabled, time ->
        RemindersUiState(
            morningEnabled = enabled.first,
            afternoonEnabled = enabled.second,
            eveningEnabled = enabled.third,
            morningTime = time.first,
            afternoonTime = time.second,
            eveningTime = time.third
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        RemindersUiState()
    )

    fun toggleMorning(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setMorningReminderEnabled(enabled)
            updateAlarm("morning", enabled, uiState.value.morningTime)
        }
    }

    fun toggleAfternoon(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setAfternoonReminderEnabled(enabled)
            updateAlarm("afternoon", enabled, uiState.value.afternoonTime)
        }
    }

    fun toggleEvening(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setEveningReminderEnabled(enabled)
            updateAlarm("evening", enabled, uiState.value.eveningTime)
        }
    }

    fun updateMorningTime(time: String) {
        viewModelScope.launch {
            preferencesManager.setMorningReminderTime(time)
            if (uiState.value.morningEnabled) {
                updateAlarm("morning", true, time)
            }
        }
    }

    fun updateAfternoonTime(time: String) {
        viewModelScope.launch {
            preferencesManager.setAfternoonReminderTime(time)
            if (uiState.value.afternoonEnabled) {
                updateAlarm("afternoon", true, time)
            }
        }
    }

    fun updateEveningTime(time: String) {
        viewModelScope.launch {
            preferencesManager.setEveningReminderTime(time)
            if (uiState.value.eveningEnabled) {
                updateAlarm("evening", true, time)
            }
        }
    }

    private fun updateAlarm(type: String, enabled: Boolean, timeStr: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("reminder_type", type)
        }
        val requestCode = type.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (!enabled) {
            alarmManager.cancel(pendingIntent)
            return
        }

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
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        } catch (e: SecurityException) {
            // Log alarm scheduling permission error (Android 12+ standard warning)
        }
    }
}
