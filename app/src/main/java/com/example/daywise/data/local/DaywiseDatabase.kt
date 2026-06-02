package com.example.daywise.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.daywise.data.local.dao.ChatMessageDao
import com.example.daywise.data.local.dao.ExerciseDao
import com.example.daywise.data.local.dao.FoodDao
import com.example.daywise.data.local.dao.WeightDao
import com.example.daywise.data.local.entity.ChatMessageEntity
import com.example.daywise.data.local.entity.ExerciseEntryEntity
import com.example.daywise.data.local.entity.FoodEntryEntity
import com.example.daywise.data.local.entity.WeightEntryEntity

@Database(
    entities = [
        FoodEntryEntity::class,
        ExerciseEntryEntity::class,
        WeightEntryEntity::class,
        ChatMessageEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class DaywiseDatabase : RoomDatabase() {

    abstract fun foodDao(): FoodDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun weightDao(): WeightDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        @Volatile
        private var INSTANCE: DaywiseDatabase? = null

        fun getInstance(context: Context): DaywiseDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DaywiseDatabase::class.java,
                    "daywise_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
