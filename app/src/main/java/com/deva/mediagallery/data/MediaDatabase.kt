package com.deva.mediagallery.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [MediaItem::class], version = 1, exportSchema = false)
abstract class MediaDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
}

