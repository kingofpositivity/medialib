package com.deva.mediagallery

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deva.mediagallery.data.MediaDao
import com.deva.mediagallery.data.MediaItemData
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MediaViewModel(private val mediaDao: MediaDao, private val userId: String) :
    ViewModel() {    // Firestore instance for fetching media data
    private val firestore = FirebaseFirestore.getInstance()

    // StateFlow to store media list
    private val _mediaList = MutableStateFlow<List<MediaItemData>>(emptyList())
    val mediaList: StateFlow<List<MediaItemData>> get() = _mediaList

    init {
        fetchMedia(userId) // Fetch media when ViewModel initializes
    }

    fun clearMedia() {
        viewModelScope.launch {
            mediaDao.deleteAllMedia() // Remove all media from Room database
        }
    }

    fun deleteMediaById(mediaId: String, userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            mediaDao.deleteById(mediaId) // Remove media from Room
            fetchMedia(userId) // Refresh UI immediately
        }
    }

    fun fetchMedia(userId: String) {
        viewModelScope.launch {
            val localMedia = mediaDao.getAllMedia() // Load from local Room database
            if (localMedia.isNotEmpty()) {
                _mediaList.value = localMedia // Update UI with local data
                return@launch
            }

            // Fetch media from Firestore if local data is unavailable
            firestore.collection("media")
                .whereEqualTo("ownerId", userId) // Filter only current user's media
                .get()

                .addOnSuccessListener { snapshot ->
                    val media = snapshot.documents.mapNotNull { doc ->
                        val mediaItem = doc.toObject(MediaItemData::class.java)
                        if (mediaItem != null) {
                            val isVideo =
                                mediaItem.url.endsWith(".mp4") || mediaItem.type == "video"
                            Log.d(
                                "MediaGallery",
                                "Fetched ${if (isVideo) "Video" else "Photo"}: ${mediaItem.url}"
                            )
                        } else {
                            Log.e("MediaGallery", "Invalid media data in Firestore")
                        }
                        mediaItem
                    }
                    _mediaList.value = media // Update UI with fetched Firestore data
                }
                .addOnFailureListener { e ->
                    Log.e("DEVAPPLOG", "Failed to fetch media: ${e.message}")
                }
        }
    }
}
