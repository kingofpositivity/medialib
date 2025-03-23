package com.deva.mediagallery.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deva.mediagallery.data.MediaItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MediaViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val _mediaList = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaList: StateFlow<List<MediaItem>> get() = _mediaList

    init {
        fetchMedia()
    }

    private fun fetchMedia() {
        viewModelScope.launch {
            firestore.collection("media").get()
                .addOnSuccessListener { snapshot ->
                    val media = snapshot.documents.mapNotNull { it.toObject(MediaItem::class.java) }
                    _mediaList.value = media
                }
                .addOnFailureListener {
                    // Handle error
                }
        }
    }
}
