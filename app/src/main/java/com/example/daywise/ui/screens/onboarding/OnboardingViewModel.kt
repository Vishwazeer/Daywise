package com.example.daywise.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daywise.data.preferences.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ── Domain enums ────────────────────────────────────────────────────────────

enum class Gender(val label: String) {
    Male("Male"),
    Female("Female"),
}

enum class WeightGoal(val label: String, val emoji: String) {
    Lose("Lose Weight", "🔻"),
    Maintain("Maintain Weight", "⚖\uFE0F"),
    Gain("Gain Weight", "🔺"),
}

enum class AggressionLevel(
    val label: String,
    val kgPerWeek: Float,
    val dailyCalorieAdjustment: Int,
) {
    Relaxed("Relaxed", 0.25f, 250),
    Moderate("Moderate", 0.5f, 500),
    Aggressive("Aggressive", 0.75f, 750),
}

enum class BmiCategory(val label: String) {
    Underweight("Underweight"),
    Normal("Normal"),
    Overweight("Overweight"),
    Obese("Obese"),
}

// ── UI State ────────────────────────────────────────────────────────────────

data class OnboardingUiState(
    val currentStep: Int = 0,
    val gender: Gender? = null,
    val ageText: String = "",
    val heightText: String = "",
    val weightText: String = "",
    val goal: WeightGoal? = null,
    val aggression: AggressionLevel? = null,
    val targetWeightText: String = "",
    val isSaving: Boolean = false,
) {

    // Total number of steps varies based on goal selection
    val totalSteps: Int
        get() = if (goal == WeightGoal.Maintain) 5 else 6

    // Parsed numeric values (safe defaults)
    val age: Int get() = ageText.toIntOrNull() ?: 0
    val height: Float get() = heightText.toFloatOrNull() ?: 0f
    val weight: Float get() = weightText.toFloatOrNull() ?: 0f
    val targetWeight: Float get() = targetWeightText.toFloatOrNull() ?: weight

    // ── Calculations ────────────────────────────────────────────────────

    val bmi: Float
        get() {
            if (height <= 0f || weight <= 0f) return 0f
            val heightM = height / 100f
            return weight / (heightM * heightM)
        }

    val bmiCategory: BmiCategory
        get() = when {
            bmi < 18.5f -> BmiCategory.Underweight
            bmi < 25f -> BmiCategory.Normal
            bmi < 30f -> BmiCategory.Overweight
            else -> BmiCategory.Obese
        }

    val targetBmi: Float
        get() {
            if (height <= 0f || targetWeight <= 0f) return 0f
            val heightM = height / 100f
            return targetWeight / (heightM * heightM)
        }

    val targetBmiCategory: BmiCategory
        get() = when {
            targetBmi < 18.5f -> BmiCategory.Underweight
            targetBmi < 25f -> BmiCategory.Normal
            targetBmi < 30f -> BmiCategory.Overweight
            else -> BmiCategory.Obese
        }

    /** BMR via Mifflin-St Jeor equation */
    val bmr: Int
        get() {
            if (weight <= 0f || height <= 0f || age <= 0) return 0
            val base = (10f * weight) + (6.25f * height) - (5f * age)
            return when (gender) {
                Gender.Male -> (base + 5f).roundToInt()
                Gender.Female -> (base - 161f).roundToInt()
                null -> (base - 78f).roundToInt() // midpoint fallback
            }
        }

    /** TDEE = BMR × 1.30 (lightly active baseline) */
    val tdee: Int get() = (bmr * 1.30f).roundToInt()

    /** Daily calorie target accounting for goal + aggression */
    val dailyCalorieTarget: Int
        get() {
            if (tdee <= 0) return 0
            return when (goal) {
                WeightGoal.Lose -> (tdee - (aggression?.dailyCalorieAdjustment ?: 0))
                    .coerceAtLeast(1200)
                WeightGoal.Gain -> tdee + (aggression?.dailyCalorieAdjustment ?: 0)
                WeightGoal.Maintain -> tdee
                null -> tdee
            }
        }

    /** Protein grams – 25 % of calories, 4 cal/g */
    val proteinGrams: Int get() = ((dailyCalorieTarget * 0.25f) / 4f).roundToInt()

    /** Carb grams – 50 % of calories, 4 cal/g */
    val carbsGrams: Int get() = ((dailyCalorieTarget * 0.50f) / 4f).roundToInt()

    /** Fat grams – 25 % of calories, 9 cal/g */
    val fatGrams: Int get() = ((dailyCalorieTarget * 0.25f) / 9f).roundToInt()

    /** Estimated weeks to reach target weight */
    val estimatedWeeks: Int
        get() {
            if (goal == WeightGoal.Maintain) return 0
            val diff = kotlin.math.abs(weight - targetWeight)
            val rate = aggression?.kgPerWeek ?: 0.5f
            return if (rate > 0f) (diff / rate).roundToInt() else 0
        }

    // ── Validation helpers ──────────────────────────────────────────────

    val isBasicInfoValid: Boolean
        get() = gender != null && age in 10..120 && height in 50f..300f && weight in 20f..500f

    val isTargetWeightValid: Boolean
        get() = targetWeight in 20f..500f

    /** Whether the "Next" button should be enabled for the current step */
    val canAdvance: Boolean
        get() = when (currentStep) {
            0 -> true
            1 -> isBasicInfoValid
            2 -> goal != null
            3 -> if (goal == WeightGoal.Maintain) isTargetWeightValid else aggression != null
            4 -> isTargetWeightValid
            5 -> true
            else -> false
        }
}

// ── ViewModel ───────────────────────────────────────────────────────────────

class OnboardingViewModel(
    private val preferencesManager: PreferencesManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    // ── Step navigation ─────────────────────────────────────────────────

    fun nextStep() {
        _uiState.update { state ->
            val next = state.currentStep + 1
            // Skip aggression step for "Maintain"
            val adjusted = if (next == 3 && state.goal == WeightGoal.Maintain) 4 else next
            state.copy(currentStep = adjusted.coerceAtMost(5))
        }
    }

    fun previousStep() {
        _uiState.update { state ->
            val prev = state.currentStep - 1
            // Skip aggression step backward for "Maintain"
            val adjusted = if (prev == 3 && state.goal == WeightGoal.Maintain) 2 else prev
            state.copy(currentStep = adjusted.coerceAtLeast(0))
        }
    }

    // ── Input setters ───────────────────────────────────────────────────

    fun setGender(gender: Gender) {
        _uiState.update { it.copy(gender = gender) }
    }

    fun setAge(value: String) {
        // Allow only digits
        val filtered = value.filter { it.isDigit() }.take(3)
        _uiState.update { it.copy(ageText = filtered) }
    }

    fun setHeight(value: String) {
        val filtered = value.filter { it.isDigit() || it == '.' }
            .let { s ->
                // Allow at most one decimal point
                val dotIndex = s.indexOf('.')
                if (dotIndex >= 0) s.substring(0, dotIndex + 1) + s.substring(dotIndex + 1).replace(".", "")
                else s
            }.take(6)
        _uiState.update { it.copy(heightText = filtered) }
    }

    fun setWeight(value: String) {
        val filtered = value.filter { it.isDigit() || it == '.' }
            .let { s ->
                val dotIndex = s.indexOf('.')
                if (dotIndex >= 0) s.substring(0, dotIndex + 1) + s.substring(dotIndex + 1).replace(".", "")
                else s
            }.take(6)
        _uiState.update { it.copy(weightText = filtered) }
    }

    fun setGoal(goal: WeightGoal) {
        _uiState.update {
            it.copy(
                goal = goal,
                // Reset aggression when goal changes
                aggression = if (goal == WeightGoal.Maintain) null else it.aggression,
            )
        }
    }

    fun setAggression(aggression: AggressionLevel) {
        _uiState.update { it.copy(aggression = aggression) }
    }

    fun setTargetWeight(value: String) {
        val filtered = value.filter { it.isDigit() || it == '.' }
            .let { s ->
                val dotIndex = s.indexOf('.')
                if (dotIndex >= 0) s.substring(0, dotIndex + 1) + s.substring(dotIndex + 1).replace(".", "")
                else s
            }.take(6)
        _uiState.update { it.copy(targetWeightText = filtered) }
    }

    // ── Completion: persist all values ──────────────────────────────────

    fun completeOnboarding(onComplete: () -> Unit) {
        val state = _uiState.value
        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            preferencesManager.setIsMale(state.gender == Gender.Male)
            preferencesManager.setAge(state.age)
            preferencesManager.setHeightCm(state.height)
            preferencesManager.setCurrentWeightKg(state.weight)
            preferencesManager.setTargetWeightKg(state.targetWeight)
            preferencesManager.setWeightGoal(state.goal?.name?.lowercase() ?: "lose")
            preferencesManager.setDailyCalorieGoal(state.dailyCalorieTarget)
            preferencesManager.setCarbsPercent(50)
            preferencesManager.setProteinPercent(25)
            preferencesManager.setFatPercent(25)
            preferencesManager.setHasCompletedOnboarding(true)

            _uiState.update { it.copy(isSaving = false) }
            onComplete()
        }
    }
}
