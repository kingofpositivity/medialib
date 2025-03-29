package com.deva.mediagallery.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media")
data class MediaItemData(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String = "", // Add default values
    val url: String = ""   // Add default values
) {
    constructor() : this(0, "", "") // Add an explicit no-argument constructor
}
