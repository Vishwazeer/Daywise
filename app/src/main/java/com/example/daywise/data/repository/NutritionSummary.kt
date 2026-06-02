package com.example.daywise.data.repository

/**
 * Aggregated nutrition summary for a given day.
 */
data class NutritionSummary(
    val totalCalories: Int = 0,
    val totalProtein: Float = 0f,
    val totalCarbs: Float = 0f,
    val totalFat: Float = 0f,
    val exerciseCalories: Int = 0,
    val remainingCalories: Int = 0
)
