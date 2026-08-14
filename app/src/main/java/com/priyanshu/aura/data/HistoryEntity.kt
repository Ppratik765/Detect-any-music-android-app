package com.priyanshu.aura.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history_table")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val artist: String,
    val spotifyId: String?,
    val youtubeId: String?,
    val timestamp: Long = System.currentTimeMillis()
)
