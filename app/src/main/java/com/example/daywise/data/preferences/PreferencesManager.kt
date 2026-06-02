package com.example.daywise.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "daywise_preferences")

class PreferencesManager(private val context: Context) {

    // ── Keys ──────────────────────────────────────────────────────────────────

    private object Keys {
        val DAILY_CALORIE_GOAL = intPreferencesKey("daily_calorie_goal")
        val CARBS_PERCENT = intPreferencesKey("carbs_percent")
        val PROTEIN_PERCENT = intPreferencesKey("protein_percent")
        val FAT_PERCENT = intPreferencesKey("fat_percent")
        val TARGET_WEIGHT_KG = floatPreferencesKey("target_weight_kg")
        val CURRENT_WEIGHT_KG = floatPreferencesKey("current_weight_kg")
        val HEIGHT_CM = floatPreferencesKey("height_cm")
        val AGE = intPreferencesKey("age")
        val IS_MALE = booleanPreferencesKey("is_male")
        val ACTIVITY_LEVEL = stringPreferencesKey("activity_level")
        val WEIGHT_GOAL = stringPreferencesKey("weight_goal")
        val WEEKLY_GOAL_KG = floatPreferencesKey("weekly_goal_kg")
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val MORNING_REMINDER_ENABLED = booleanPreferencesKey("morning_reminder_enabled")
        val AFTERNOON_REMINDER_ENABLED = booleanPreferencesKey("afternoon_reminder_enabled")
        val EVENING_REMINDER_ENABLED = booleanPreferencesKey("evening_reminder_enabled")
        val MORNING_REMINDER_TIME = stringPreferencesKey("morning_reminder_time")
        val AFTERNOON_REMINDER_TIME = stringPreferencesKey("afternoon_reminder_time")
        val EVENING_REMINDER_TIME = stringPreferencesKey("evening_reminder_time")
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
    }

    // ── Flow Getters ──────────────────────────────────────────────────────────

    val dailyCalorieGoal: Flow<Int> = context.dataStore.data
        .map { it[Keys.DAILY_CALORIE_GOAL] ?: 2000 }

    val carbsPercent: Flow<Int> = context.dataStore.data
        .map { it[Keys.CARBS_PERCENT] ?: 50 }

    val proteinPercent: Flow<Int> = context.dataStore.data
        .map { it[Keys.PROTEIN_PERCENT] ?: 25 }

    val fatPercent: Flow<Int> = context.dataStore.data
        .map { it[Keys.FAT_PERCENT] ?: 25 }

    val targetWeightKg: Flow<Float> = context.dataStore.data
        .map { it[Keys.TARGET_WEIGHT_KG] ?: 70f }

    val currentWeightKg: Flow<Float> = context.dataStore.data
        .map { it[Keys.CURRENT_WEIGHT_KG] ?: 80f }

    val heightCm: Flow<Float> = context.dataStore.data
        .map { it[Keys.HEIGHT_CM] ?: 170f }

    val age: Flow<Int> = context.dataStore.data
        .map { it[Keys.AGE] ?: 25 }

    val isMale: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.IS_MALE] ?: true }

    val activityLevel: Flow<String> = context.dataStore.data
        .map { it[Keys.ACTIVITY_LEVEL] ?: "moderate" }

    val weightGoal: Flow<String> = context.dataStore.data
        .map { it[Keys.WEIGHT_GOAL] ?: "lose" }

    val weeklyGoalKg: Flow<Float> = context.dataStore.data
        .map { it[Keys.WEEKLY_GOAL_KG] ?: 0.5f }

    val hasCompletedOnboarding: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.HAS_COMPLETED_ONBOARDING] ?: false }

    val morningReminderEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.MORNING_REMINDER_ENABLED] ?: true }

    val afternoonReminderEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.AFTERNOON_REMINDER_ENABLED] ?: true }

    val eveningReminderEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.EVENING_REMINDER_ENABLED] ?: true }

    val morningReminderTime: Flow<String> = context.dataStore.data
        .map { it[Keys.MORNING_REMINDER_TIME] ?: "09:00" }

    val afternoonReminderTime: Flow<String> = context.dataStore.data
        .map { it[Keys.AFTERNOON_REMINDER_TIME] ?: "13:00" }

    val eveningReminderTime: Flow<String> = context.dataStore.data
        .map { it[Keys.EVENING_REMINDER_TIME] ?: "19:00" }

    val geminiApiKey: Flow<String> = context.dataStore.data
        .map { it[Keys.GEMINI_API_KEY] ?: "" }

    // ── Suspend Setters ───────────────────────────────────────────────────────

    suspend fun setDailyCalorieGoal(value: Int) {
        context.dataStore.edit { it[Keys.DAILY_CALORIE_GOAL] = value }
    }

    suspend fun setCarbsPercent(value: Int) {
        context.dataStore.edit { it[Keys.CARBS_PERCENT] = value }
    }

    suspend fun setProteinPercent(value: Int) {
        context.dataStore.edit { it[Keys.PROTEIN_PERCENT] = value }
    }

    suspend fun setFatPercent(value: Int) {
        context.dataStore.edit { it[Keys.FAT_PERCENT] = value }
    }

    suspend fun setTargetWeightKg(value: Float) {
        context.dataStore.edit { it[Keys.TARGET_WEIGHT_KG] = value }
    }

    suspend fun setCurrentWeightKg(value: Float) {
        context.dataStore.edit { it[Keys.CURRENT_WEIGHT_KG] = value }
    }

    suspend fun setHeightCm(value: Float) {
        context.dataStore.edit { it[Keys.HEIGHT_CM] = value }
    }

    suspend fun setAge(value: Int) {
        context.dataStore.edit { it[Keys.AGE] = value }
    }

    suspend fun setIsMale(value: Boolean) {
        context.dataStore.edit { it[Keys.IS_MALE] = value }
    }

    suspend fun setActivityLevel(value: String) {
        context.dataStore.edit { it[Keys.ACTIVITY_LEVEL] = value }
    }

    suspend fun setWeightGoal(value: String) {
        context.dataStore.edit { it[Keys.WEIGHT_GOAL] = value }
    }

    suspend fun setWeeklyGoalKg(value: Float) {
        context.dataStore.edit { it[Keys.WEEKLY_GOAL_KG] = value }
    }

    suspend fun setHasCompletedOnboarding(value: Boolean) {
        context.dataStore.edit { it[Keys.HAS_COMPLETED_ONBOARDING] = value }
    }

    suspend fun setMorningReminderEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.MORNING_REMINDER_ENABLED] = value }
    }

    suspend fun setAfternoonReminderEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.AFTERNOON_REMINDER_ENABLED] = value }
    }

    suspend fun setEveningReminderEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.EVENING_REMINDER_ENABLED] = value }
    }

    suspend fun setMorningReminderTime(value: String) {
        context.dataStore.edit { it[Keys.MORNING_REMINDER_TIME] = value }
    }

    suspend fun setAfternoonReminderTime(value: String) {
        context.dataStore.edit { it[Keys.AFTERNOON_REMINDER_TIME] = value }
    }

    suspend fun setEveningReminderTime(value: String) {
        context.dataStore.edit { it[Keys.EVENING_REMINDER_TIME] = value }
    }

    suspend fun setGeminiApiKey(value: String) {
        context.dataStore.edit { it[Keys.GEMINI_API_KEY] = value }
    }
}
