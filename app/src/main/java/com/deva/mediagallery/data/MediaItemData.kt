package com.deva.mediagallery.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media")
data class MediaItemData(
    @PrimaryKey val id: String = "", // Firestore ID
    val name: String = "",
    val url: String = "",
    val type: String = "",
    val size: Long = 0L, // Size in bytes
    val uploadedAt: Long = System.currentTimeMillis(), // Timestamp of upload
    val duration: Long? = null, // Duration in ms (for videos)
    val width: Int? = null, // Media width in pixels
    val height: Int? = null, // Media height in pixels
    val mimeType: String? = null, // MIME type (e.g., "image/jpeg")
    val description: String? = null, // Optional description
    val isFavorite: Boolean = false, // User favorite status
    val ownerId: String? = null // User ID of uploader
) {
    constructor() : this(
        id = "",
        name = "",
        url = "",
        type = "",
        size = 0L,
        uploadedAt = System.currentTimeMillis(),
        duration = null,
        width = null,
        height = null,
        mimeType = null,
        description = null,
        isFavorite = false,
        ownerId = null
    )}

