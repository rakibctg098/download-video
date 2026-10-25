package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    val title: String,
    val platform: String,
    val contentType: String? = null,
    val fileSizeBytes: Long? = null,
    val status: String,
    val localUri: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
