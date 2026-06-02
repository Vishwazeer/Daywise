package com.example.daywise.ui.screens.goals

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

data class GoalsUiState(
    val calorieGoal: Int = 2000,
    val carbsPercent: Int = 50,
    val proteinPercent: Int = 25,
    val fatPercent: Int = 25,
    val error: String? = null
)

class GoalsViewModel(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    val calorieGoal = preferencesManager.dailyCalorieGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2000)

    val carbsPercent = preferencesManager.carbsPercent
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 50)

    val proteinPercent = preferencesManager.proteinPercent
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 25)

    val fatPercent = preferencesManager.fatPercent
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 25)

    val uiState: StateFlow<GoalsUiState> = combine(
        calorieGoal,
        carbsPercent,
        proteinPercent,
        fatPercent
    ) { calorie, carbs, protein, fat ->
        GoalsUiState(
            calorieGoal = calorie,
            carbsPercent = carbs,
            proteinPercent = protein,
            fatPercent = fat
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        GoalsUiState()
    )

    fun updateCalorieGoal(calories: Int) {
        viewModelScope.launch {
            preferencesManager.setDailyCalorieGoal(calories)
        }
    }

    fun updateMacroPercentages(carbs: Int, protein: Int, fat: Int): Boolean {
        if (carbs + protein + fat == 100) {
            viewModelScope.launch {
                preferencesManager.setCarbsPercent(carbs)
                preferencesManager.setProteinPercent(protein)
                preferencesManager.setFatPercent(fat)
            }
            return true
        }
        return false
    }
}
