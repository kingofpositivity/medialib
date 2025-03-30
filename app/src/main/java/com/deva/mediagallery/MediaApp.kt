package com.deva.mediagallery;
import com.deva.mediagallery.data.MediaDatabase

import android.app.Application
import androidx.room.Room

class MediaApp : Application() {

    val database: MediaDatabase by lazy {
        Room.databaseBuilder(
            this,
            MediaDatabase::class.java,
            "media_database"
        ).fallbackToDestructiveMigration()
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        // No need to initialize `database` here; it's handled by `by lazy`
    }
}
