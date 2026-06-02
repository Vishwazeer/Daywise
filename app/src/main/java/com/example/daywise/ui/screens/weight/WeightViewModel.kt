package com.example.daywise.ui.screens.weight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daywise.data.local.entity.WeightEntryEntity
import com.example.daywise.data.preferences.PreferencesManager
import com.example.daywise.data.repository.DaywiseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

enum class TimePeriod {
    WEEK, MONTH, YEAR, ALL
}

data class WeightUiState(
    val entries: List<WeightEntryEntity> = emptyList(),
    val filteredEntries: List<WeightEntryEntity> = emptyList(),
    val currentWeightKg: Float = 0f,
    val targetWeightKg: Float = 70f,
    val selectedPeriod: TimePeriod = TimePeriod.WEEK,
    val isLoading: Boolean = false
)

class WeightViewModel(
    private val repository: DaywiseRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(TimePeriod.WEEK)
    val selectedPeriod: StateFlow<TimePeriod> = _selectedPeriod.asStateFlow()

    val targetWeightKg: StateFlow<Float> = preferencesManager.targetWeightKg
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 70f)

    val currentWeightKg: StateFlow<Float> = preferencesManager.currentWeightKg
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 80f)

    val uiState: StateFlow<WeightUiState> = combine(
        repository.getWeightEntries(),
        targetWeightKg,
        currentWeightKg,
        _selectedPeriod
    ) { entries, target, current, period ->
        val filtered = filterEntries(entries, period)
        WeightUiState(
            entries = entries,
            filteredEntries = filtered,
            currentWeightKg = current,
            targetWeightKg = target,
            selectedPeriod = period,
            isLoading = false
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        WeightUiState(isLoading = true)
    )

    fun selectPeriod(period: TimePeriod) {
        _selectedPeriod.value = period
    }

    fun addWeight(weightKg: Float) {
        viewModelScope.launch {
            repository.addWeightEntry(weightKg)
        }
    }

    private fun filterEntries(entries: List<WeightEntryEntity>, period: TimePeriod): List<WeightEntryEntity> {
        val cutoff = when (period) {
            TimePeriod.WEEK -> getCutoffMillis(7)
            TimePeriod.MONTH -> getCutoffMillis(30)
            TimePeriod.YEAR -> getCutoffMillis(365)
            TimePeriod.ALL -> 0L
        }
        // Ordered ascending for chart, descending for list.
        // We will return ordered ascending here so chart draws left-to-right correctly,
        // and reverse it in the UI when displaying list.
        return entries.filter { it.timestamp >= cutoff }.sortedBy { it.timestamp }
    }

    private fun getCutoffMillis(daysAgo: Int): Long {
        val cal = Calendar.getInstance(TimeZone.getDefault()).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -daysAgo)
        }
        return cal.timeInMillis
    }
}
