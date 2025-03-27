package com.deva.mediagallery.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_table")
data class MediaItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val url: String
)
