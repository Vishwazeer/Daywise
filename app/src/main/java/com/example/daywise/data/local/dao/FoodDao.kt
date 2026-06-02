package com.example.daywise.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.daywise.data.local.entity.FoodEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: FoodEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<FoodEntryEntity>)

    @Query("DELETE FROM food_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM food_entries WHERE timestamp >= :startOfDay AND timestamp < :endOfDay ORDER BY timestamp DESC")
    fun getEntriesForDateRange(startOfDay: Long, endOfDay: Long): Flow<List<FoodEntryEntity>>

    @Query("""
        SELECT 
            COALESCE(SUM(calories), 0) AS totalCalories,
            COALESCE(SUM(proteinG), 0.0) AS totalProtein,
            COALESCE(SUM(carbsG), 0.0) AS totalCarbs,
            COALESCE(SUM(fatG), 0.0) AS totalFat
        FROM food_entries 
        WHERE timestamp >= :startOfDay AND timestamp < :endOfDay
    """)
    fun getDailyTotals(startOfDay: Long, endOfDay: Long): Flow<DailyFoodTotals>

    @Query("SELECT * FROM food_entries ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEntries(limit: Int = 20): Flow<List<FoodEntryEntity>>
}

data class DailyFoodTotals(
    val totalCalories: Int,
    val totalProtein: Float,
    val totalCarbs: Float,
    val totalFat: Float
)
