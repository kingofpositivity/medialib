package com.deva.mediagallery.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MediaDao {
    @Query("SELECT * FROM media ORDER BY name DESC")
    suspend fun getAllMedia(): List<MediaItemData>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: List<MediaItemData>) // Accepts List instead of vararg

    @Query("DELETE FROM media")
    suspend fun clearMedia()
}
