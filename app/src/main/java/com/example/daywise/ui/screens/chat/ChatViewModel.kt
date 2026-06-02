package com.example.daywise.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daywise.data.local.entity.ChatMessageEntity
import com.example.daywise.data.repository.DaywiseRepository
import com.example.daywise.data.repository.NutritionSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar

class ChatViewModel(
    private val repository: DaywiseRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    val chatMessages: StateFlow<List<ChatMessageEntity>> = _selectedDate
        .flatMapLatest { date ->
            repository.getChatMessagesForDate(dateToMillis(date))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val nutritionSummary: StateFlow<NutritionSummary> = _selectedDate
        .flatMapLatest { date ->
            repository.getDailyNutritionSummary(dateToMillis(date))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NutritionSummary())

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                repository.processUserInput(text)
            } catch (e: Exception) {
                // Handled in repository by inserting system/error message
            } finally {
                _isProcessing.value = false
            }
        }
    }

    private fun dateToMillis(date: LocalDate): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, date.year)
            set(Calendar.MONTH, date.monthValue - 1)
            set(Calendar.DAY_OF_MONTH, date.dayOfMonth)
            set(Calendar.HOUR_OF_DAY, 12) // Use mid-day to avoid timezone offset shifts
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
}
