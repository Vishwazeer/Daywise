package com.example.daywise.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.daywise.data.local.entity.ExerciseEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ExerciseEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<ExerciseEntryEntity>)

    @Query("DELETE FROM exercise_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM exercise_entries WHERE timestamp >= :startOfDay AND timestamp < :endOfDay ORDER BY timestamp DESC")
    fun getEntriesForDateRange(startOfDay: Long, endOfDay: Long): Flow<List<ExerciseEntryEntity>>

    @Query("SELECT COALESCE(SUM(caloriesBurned), 0) FROM exercise_entries WHERE timestamp >= :startOfDay AND timestamp < :endOfDay")
    fun getDailyCaloriesBurned(startOfDay: Long, endOfDay: Long): Flow<Int>

    @Query("SELECT * FROM exercise_entries ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEntries(limit: Int = 20): Flow<List<ExerciseEntryEntity>>
}
