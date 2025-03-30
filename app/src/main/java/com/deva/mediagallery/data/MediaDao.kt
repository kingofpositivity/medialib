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
    suspend fun insertMedia(media: List<MediaItemData>)

    @Query("DELETE FROM media")
    suspend fun deleteAllMedia()

    @Query("DELETE FROM media WHERE id = :mediaId")
    suspend fun deleteById(mediaId: String)


    @Query("DELETE FROM media")
    suspend fun clearMedia()
}
