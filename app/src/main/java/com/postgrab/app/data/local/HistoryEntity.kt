package com.postgrab.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val source: String,
    val sourceType: String,
    val name: String,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val mime: String,
    val status: String,
    val savedUri: String?,
    val error: String?,
    val createdAt: Long,
    val updatedAt: Long
)
