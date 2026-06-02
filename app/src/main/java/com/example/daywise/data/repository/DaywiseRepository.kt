package com.example.daywise.data.repository

import com.example.daywise.data.local.dao.ChatMessageDao
import com.example.daywise.data.local.dao.ExerciseDao
import com.example.daywise.data.local.dao.FoodDao
import com.example.daywise.data.local.dao.WeightDao
import com.example.daywise.data.local.entity.ChatMessageEntity
import com.example.daywise.data.local.entity.ExerciseEntryEntity
import com.example.daywise.data.local.entity.FoodEntryEntity
import com.example.daywise.data.local.entity.WeightEntryEntity
import com.example.daywise.data.preferences.PreferencesManager
import com.example.daywise.data.remote.GeminiService
import com.example.daywise.data.remote.ParsedInput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.TimeZone

/**
 * Central repository for all Daywise data operations.
 *
 * Coordinates between Gemini AI parsing, Room persistence, and DataStore preferences.
 */
class DaywiseRepository(
    private val foodDao: FoodDao,
    private val exerciseDao: ExerciseDao,
    private val weightDao: WeightDao,
    private val chatMessageDao: ChatMessageDao,
    private val preferencesManager: PreferencesManager,
    private val geminiService: GeminiService
) {

    // ── User Input Processing ─────────────────────────────────────────────────

    /**
     * Sends the user's natural-language text to Gemini for parsing, persists the
     * resulting food/exercise entries to Room, and saves both the user message
     * and the system response as chat messages.
     *
     * @return a human-readable response string for the chat UI
     */
    suspend fun processUserInput(text: String): String {
        val now = System.currentTimeMillis()

        // Save the user's chat message
        chatMessageDao.insert(
            ChatMessageEntity(
                timestamp = now,
                content = text,
                isUser = true,
                messageType = "text"
            )
        )

        val parsed = geminiService.parseInput(text)
        val responseMessage: String
        val messageType: String

        when (parsed) {
            is ParsedInput.FoodParsed -> {
                val entities = parsed.items.map { item ->
                    FoodEntryEntity(
                        timestamp = now,
                        name = item.name,
                        calories = item.calories,
                        proteinG = item.proteinG,
                        carbsG = item.carbsG,
                        fatG = item.fatG,
                        servingSize = item.servingSize,
                        rawInput = text
                    )
                }
                foodDao.insertAll(entities)

                val totalCal = parsed.items.sumOf { it.calories }
                val totalP = parsed.items.sumOf { it.proteinG.toDouble() }
                val totalC = parsed.items.sumOf { it.carbsG.toDouble() }
                val totalF = parsed.items.sumOf { it.fatG.toDouble() }

                val builder = java.lang.StringBuilder("✅ Logged:\n")
                parsed.items.forEach { item ->
                    val qtyStr = if (item.servingSize.isNotEmpty()) " - ${item.servingSize}" else ""
                    builder.append("• ${item.name}$qtyStr (${item.calories} kcal)\n")
                }
                builder.append("\nRecorded Macronutrients:\n")
                builder.append("🔥 Calories: $totalCal kcal\n")
                builder.append("🥩 Protein: ${totalP.toInt()}g\n")
                builder.append("🍞 Carbs: ${totalC.toInt()}g\n")
                builder.append("🥑 Fat: ${totalF.toInt()}g")

                responseMessage = builder.toString()
                messageType = "food_log"
            }

            is ParsedInput.ExerciseParsed -> {
                val entities = parsed.items.map { item ->
                    ExerciseEntryEntity(
                        timestamp = now,
                        name = item.name,
                        durationMinutes = item.durationMinutes,
                        caloriesBurned = item.caloriesBurned,
                        rawInput = text
                    )
                }
                exerciseDao.insertAll(entities)

                val totalBurned = parsed.items.sumOf { it.caloriesBurned }
                val builder = java.lang.StringBuilder("🏃 Logged:\n")
                parsed.items.forEach { item ->
                    builder.append("• ${item.name} (${item.durationMinutes} mins, -${item.caloriesBurned} kcal)\n")
                }
                builder.append("\n🔥 Total Burned: -$totalBurned kcal")

                responseMessage = builder.toString()
                messageType = "exercise_log"
            }

            is ParsedInput.Unknown -> {
                responseMessage = "⚠️ ${parsed.message}"
                messageType = "text"
            }

            is ParsedInput.Error -> {
                responseMessage = "⚠️ ${parsed.message}"
                messageType = "system"
            }
        }

        // Save the system response as a chat message
        chatMessageDao.insert(
            ChatMessageEntity(
                timestamp = System.currentTimeMillis(),
                content = responseMessage,
                isUser = false,
                messageType = messageType
            )
        )

        return responseMessage
    }

    // ── Food Entries ──────────────────────────────────────────────────────────

    fun getFoodEntriesForDate(dateMillis: Long): Flow<List<FoodEntryEntity>> {
        val (start, end) = dayBounds(dateMillis)
        return foodDao.getEntriesForDateRange(start, end)
    }

    suspend fun deleteFoodEntry(id: Long) {
        foodDao.deleteById(id)
    }

    // ── Exercise Entries ──────────────────────────────────────────────────────

    fun getExerciseEntriesForDate(dateMillis: Long): Flow<List<ExerciseEntryEntity>> {
        val (start, end) = dayBounds(dateMillis)
        return exerciseDao.getEntriesForDateRange(start, end)
    }

    suspend fun deleteExerciseEntry(id: Long) {
        exerciseDao.deleteById(id)
    }

    // ── Daily Nutrition Summary ───────────────────────────────────────────────

    /**
     * Combines food totals, exercise calories, and the user's calorie goal
     * into a single reactive [NutritionSummary] flow.
     */
    fun getDailyNutritionSummary(dateMillis: Long): Flow<NutritionSummary> {
        val (start, end) = dayBounds(dateMillis)
        return combine(
            foodDao.getDailyTotals(start, end),
            exerciseDao.getDailyCaloriesBurned(start, end),
            preferencesManager.dailyCalorieGoal
        ) { foodTotals, exerciseCal, calorieGoal ->
            NutritionSummary(
                totalCalories = foodTotals.totalCalories,
                totalProtein = foodTotals.totalProtein,
                totalCarbs = foodTotals.totalCarbs,
                totalFat = foodTotals.totalFat,
                exerciseCalories = exerciseCal,
                remainingCalories = calorieGoal - foodTotals.totalCalories + exerciseCal
            )
        }
    }

    // ── Weekly Data ───────────────────────────────────────────────────────────

    /**
     * Returns food and exercise entries within a date range for weekly summaries.
     */
    suspend fun getWeeklyData(
        startDate: Long,
        endDate: Long
    ): Pair<List<FoodEntryEntity>, List<ExerciseEntryEntity>> {
        val food = foodDao.getEntriesForDateRange(startDate, endDate).first()
        val exercise = exerciseDao.getEntriesForDateRange(startDate, endDate).first()
        return food to exercise
    }

    // ── Chat Messages ─────────────────────────────────────────────────────────

    fun getChatMessagesForDate(dateMillis: Long): Flow<List<ChatMessageEntity>> {
        val (start, end) = dayBounds(dateMillis)
        return chatMessageDao.getMessagesForDay(start, end)
    }

    // ── Weight Entries ────────────────────────────────────────────────────────

    suspend fun addWeightEntry(weightKg: Float) {
        weightDao.insert(
            WeightEntryEntity(
                timestamp = System.currentTimeMillis(),
                weightKg = weightKg
            )
        )
        preferencesManager.setCurrentWeightKg(weightKg)
    }

    fun getWeightEntries(): Flow<List<WeightEntryEntity>> {
        return weightDao.getAllDescending()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns the start-of-day (00:00:00.000) and end-of-day (next day 00:00:00.000)
     * epoch millis for the day containing [dateMillis].
     */
    private fun dayBounds(dateMillis: Long): Pair<Long, Long> {
        val calendar = Calendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val endOfDay = calendar.timeInMillis
        return startOfDay to endOfDay
    }
}
