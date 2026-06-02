package com.example.daywise.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_entries")
data class FoodEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val name: String,
    val calories: Int,
    val proteinG: Float,
    val carbsG: Float,
    val fatG: Float,
    val servingSize: String = "",
    val rawInput: String = ""
)
