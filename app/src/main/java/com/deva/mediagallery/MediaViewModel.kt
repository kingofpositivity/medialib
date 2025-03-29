import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deva.mediagallery.data.MediaDao
import com.deva.mediagallery.data.MediaItemData
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MediaViewModel(private val mediaDao: MediaDao) : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val _mediaList = MutableStateFlow<List<MediaItemData>>(emptyList())
    val mediaList: StateFlow<List<MediaItemData>> get() = _mediaList

    init {
        fetchMedia()
    }

    fun fetchMedia() {
        viewModelScope.launch {
            val localMedia = mediaDao.getAllMedia()
            if (localMedia.isNotEmpty()) {
                _mediaList.value = localMedia
                return@launch
            }

            firestore.collection("media").get()
                .addOnSuccessListener { snapshot ->
                    val media = snapshot.documents.mapNotNull { doc ->
                        val mediaItem = doc.toObject(MediaItemData::class.java)
                        if (mediaItem != null) {
                            val isVideo = mediaItem.url.endsWith(".mp4") || mediaItem.type == "video"
                            Log.d("MediaGallery", "Fetched ${if (isVideo) "Video" else "Photo"}: ${mediaItem.url}")
                        } else {
                            Log.e("MediaGallery", "Invalid media data in Firestore")
                        }
                        mediaItem
                    }
                    _mediaList.value = media
                }
                .addOnFailureListener { e ->
                    Log.e("DEVAPPLOG", "Failed to fetch media: ${e.message}")
                }

        }
    }
}
