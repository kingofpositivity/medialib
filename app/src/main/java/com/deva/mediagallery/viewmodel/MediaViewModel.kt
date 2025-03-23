package com.deva.mediagallery.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deva.mediagallery.data.MediaItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*

class MediaViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance().reference
    private val _mediaList = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaList: StateFlow<List<MediaItem>> get() = _mediaList

    init {
        fetchMedia()
    }

    fun fetchMedia() {
        viewModelScope.launch {
            firestore.collection("media").get()
                .addOnSuccessListener { snapshot ->
                    val media = snapshot.documents.mapNotNull { it.toObject(MediaItem::class.java) }
                    _mediaList.value = media
                    Log.d("DEVAPPLOG", "Fetched ${media.size} media items")
                }
                .addOnFailureListener { e ->
                    Log.e("DEVAPPLOG", "Failed to fetch media: ${e.message}")
                }
        }
    }

    fun uploadMedia(uri: Uri, context: Context) {
        val fileName = "media_${UUID.randomUUID()}.jpg"
        val fileRef = storage.child("media/$fileName")

        fileRef.putFile(uri)
            .addOnSuccessListener {
                fileRef.downloadUrl.addOnSuccessListener { downloadUrl: Uri ->  // Explicitly typed
                    Log.d("DEVAPPLOG", "Upload success: $downloadUrl")
                    saveMediaToFirestore(downloadUrl.toString())
                    Toast.makeText(context, "Upload Successful!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("DEVAPPLOG", "Upload failed: ${e.message}")
                Toast.makeText(context, "Upload Failed!", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveMediaToFirestore(url: String) {
        val newMedia = MediaItem(
            id = UUID.randomUUID().toString(),
            url = url,
            name = "Uploaded Image",
            size = 0L,
            uploadDate = System.currentTimeMillis()
        )

        firestore.collection("media").document(newMedia.id)
            .set(newMedia)
            .addOnSuccessListener {
                Log.d("DEVAPPLOG", "Media saved to Firestore")
                fetchMedia() // Refresh the gallery
            }
            .addOnFailureListener { e ->
                Log.e("DEVAPPLOG", "Failed to save media: ${e.message}")
            }
    }
}
