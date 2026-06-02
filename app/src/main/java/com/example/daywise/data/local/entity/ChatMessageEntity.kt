package com.example.daywise.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val content: String,
    val isUser: Boolean,
    val messageType: String = "text" // text, food_log, exercise_log, system
)
