package com.example.daywise.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.daywise.data.local.entity.WeightEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WeightEntryEntity): Long

    @Query("DELETE FROM weight_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM weight_entries ORDER BY timestamp DESC")
    fun getAllDescending(): Flow<List<WeightEntryEntity>>

    @Query("SELECT * FROM weight_entries ORDER BY timestamp DESC LIMIT 1")
    fun getLatest(): Flow<WeightEntryEntity?>

    @Query("SELECT * FROM weight_entries WHERE timestamp >= :startDate AND timestamp <= :endDate ORDER BY timestamp ASC")
    fun getByDateRange(startDate: Long, endDate: Long): Flow<List<WeightEntryEntity>>
}
