package com.example.daywise.ui.screens.reminders

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.daywise.data.preferences.PreferencesManager
import com.example.daywise.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context.applicationContext) }
    val viewModel: RemindersViewModel = viewModel {
        RemindersViewModel(context.applicationContext, prefs)
    }

    val state by viewModel.uiState.collectAsState()

    fun showTimePicker(currentTimeStr: String, onTimeSelected: (String) -> Unit) {
        val parts = currentTimeStr.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        TimePickerDialog(
            context,
            { _, selectedHour, selectedMinute ->
                val timeString = String.format("%02d:%02d", selectedHour, selectedMinute)
                onTimeSelected(timeString)
            },
            hour,
            minute,
            false // Use 12h AM/PM picker as Journable reference shows "9:00 AM" etc.
        ).show()
    }

    fun formatTo12Hour(time24h: String): String {
        val parts = time24h.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val isPm = hour >= 12
        val hour12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        val minStr = String.format("%02d", minute)
        val amPm = if (isPm) "PM" else "AM"
        return "$hour12:$minStr $amPm"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reminders", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Morning
            ReminderCard(
                label = "Morning",
                timeFormatted = formatTo12Hour(state.morningTime),
                enabled = state.morningEnabled,
                onToggle = { viewModel.toggleMorning(it) },
                onTimeClick = { showTimePicker(state.morningTime) { viewModel.updateMorningTime(it) } }
            )

            // Afternoon
            ReminderCard(
                label = "Afternoon",
                timeFormatted = formatTo12Hour(state.afternoonTime),
                enabled = state.afternoonEnabled,
                onToggle = { viewModel.toggleAfternoon(it) },
                onTimeClick = { showTimePicker(state.afternoonTime) { viewModel.updateAfternoonTime(it) } }
            )

            // Evening
            ReminderCard(
                label = "Evening",
                timeFormatted = formatTo12Hour(state.eveningTime),
                enabled = state.eveningEnabled,
                onToggle = { viewModel.toggleEvening(it) },
                onTimeClick = { showTimePicker(state.eveningTime) { viewModel.updateEveningTime(it) } }
            )
        }
    }
}

@Composable
fun ReminderCard(
    label: String,
    timeFormatted: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onTimeClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onTimeClick)
            ) {
                Text(label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(timeFormatted, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
            Checkbox(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
            )
        }
    }
}
