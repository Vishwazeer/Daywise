package com.example.daywise.ui.screens.weekly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daywise.data.local.entity.ExerciseEntryEntity
import com.example.daywise.data.local.entity.FoodEntryEntity
import com.example.daywise.data.preferences.PreferencesManager
import com.example.daywise.data.repository.DaywiseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

// ── Data classes ────────────────────────────────────────────────────────────

data class DaySummary(
    val dayLabel: String,          // "Sun", "Mon", etc.
    val dateMillis: Long,
    val foodCalories: Int = 0,
    val exerciseCalories: Int = 0,
    val remainingCalories: Int = 0,
    val carbsG: Float = 0f,
    val proteinG: Float = 0f,
    val fatG: Float = 0f,
    val hasData: Boolean = false
)

data class WeeklySummaryUiState(
    val selectedWeekIndex: Int = 0,
    val weekStartDate: Long = 0L,
    val weekEndDate: Long = 0L,
    val dateRangeText: String = "",
    val dailyGoal: Int = 2000,
    val days: List<DaySummary> = emptyList(),
    val totalFoodCalories: Int = 0,
    val totalExerciseCalories: Int = 0,
    val totalRemainingCalories: Int = 0,
    val totalCarbs: Float = 0f,
    val totalProtein: Float = 0f,
    val totalFat: Float = 0f,
    val daysTracked: Int = 0,
    val caloriesUnderOver: Int = 0,
    val isUnderBudget: Boolean = true,
    val isLoading: Boolean = true
)

val TAB_LABELS = listOf("This week", "Last week", "2 weeks ago", "3 weeks ago")

// ── ViewModel ───────────────────────────────────────────────────────────────

class WeeklySummaryViewModel(
    private val repository: DaywiseRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeeklySummaryUiState())
    val uiState: StateFlow<WeeklySummaryUiState> = _uiState.asStateFlow()

    val dailyCalorieGoal: StateFlow<Int> = preferencesManager.dailyCalorieGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2000)

    init {
        selectWeek(0)
    }

    fun selectWeek(index: Int) {
        _uiState.update { it.copy(selectedWeekIndex = index, isLoading = true) }
        loadWeekData(index)
    }

    private fun loadWeekData(weeksAgo: Int) {
        viewModelScope.launch {
            val goal = dailyCalorieGoal.value
            val (weekStart, weekEnd) = getWeekBounds(weeksAgo)

            val dateFormat = SimpleDateFormat("EEE, d MMM, yyyy", Locale.getDefault())
            val dateRangeText = "${dateFormat.format(weekStart)} - ${dateFormat.format(weekEnd - 1)}"

            val (foodEntries, exerciseEntries) = repository.getWeeklyData(weekStart, weekEnd)

            val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            val days = mutableListOf<DaySummary>()

            val cal = Calendar.getInstance(TimeZone.getDefault()).apply {
                timeInMillis = weekStart
            }

            for (i in 0..6) {
                val dayStart = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, 1)
                val dayEnd = cal.timeInMillis

                val dayFood = foodEntries.filter { it.timestamp in dayStart until dayEnd }
                val dayExercise = exerciseEntries.filter { it.timestamp in dayStart until dayEnd }

                val foodCal = dayFood.sumOf { it.calories }
                val exCal = dayExercise.sumOf { it.caloriesBurned }
                val remaining = goal - foodCal + exCal
                val hasData = dayFood.isNotEmpty() || dayExercise.isNotEmpty()

                days.add(
                    DaySummary(
                        dayLabel = dayNames[i],
                        dateMillis = dayStart,
                        foodCalories = foodCal,
                        exerciseCalories = exCal,
                        remainingCalories = remaining,
                        carbsG = dayFood.sumOf { it.carbsG.toDouble() }.toFloat(),
                        proteinG = dayFood.sumOf { it.proteinG.toDouble() }.toFloat(),
                        fatG = dayFood.sumOf { it.fatG.toDouble() }.toFloat(),
                        hasData = hasData
                    )
                )
            }

            val totalFood = days.sumOf { it.foodCalories }
            val totalExercise = days.sumOf { it.exerciseCalories }
            val totalRemaining = days.sumOf { it.remainingCalories }
            val daysTracked = days.count { it.hasData }
            val weeklyBudget = goal * 7
            val caloriesUnderOver = weeklyBudget - totalFood + totalExercise

            _uiState.update {
                it.copy(
                    weekStartDate = weekStart,
                    weekEndDate = weekEnd,
                    dateRangeText = dateRangeText,
                    dailyGoal = goal,
                    days = days,
                    totalFoodCalories = totalFood,
                    totalExerciseCalories = totalExercise,
                    totalRemainingCalories = totalRemaining,
                    totalCarbs = days.sumOf { d -> d.carbsG.toDouble() }.toFloat(),
                    totalProtein = days.sumOf { d -> d.proteinG.toDouble() }.toFloat(),
                    totalFat = days.sumOf { d -> d.fatG.toDouble() }.toFloat(),
                    daysTracked = daysTracked,
                    caloriesUnderOver = caloriesUnderOver,
                    isUnderBudget = caloriesUnderOver >= 0,
                    isLoading = false
                )
            }
        }
    }

    /**
     * Returns (startOfWeek, endOfWeek) in millis for [weeksAgo] weeks ago.
     * Week starts on Sunday.
     */
    private fun getWeekBounds(weeksAgo: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance(TimeZone.getDefault()).apply {
            firstDayOfWeek = Calendar.SUNDAY
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // Go to the start of the current week (Sunday)
            while (get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                add(Calendar.DAY_OF_YEAR, -1)
            }
            // Go back by weeksAgo weeks
            add(Calendar.WEEK_OF_YEAR, -weeksAgo)
        }
        val weekStart = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 7)
        val weekEnd = cal.timeInMillis
        return weekStart to weekEnd
    }
}
