package com.example.daywise

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Onboarding : NavKey
@Serializable data object Chat : NavKey
@Serializable data object WeeklySummary : NavKey
@Serializable data object WeightTracker : NavKey
@Serializable data object DailyGoals : NavKey
@Serializable data object Reminders : NavKey
@Serializable data object Settings : NavKey
