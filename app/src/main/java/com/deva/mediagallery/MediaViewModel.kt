import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deva.mediagallery.data.MediaDao
import com.deva.mediagallery.data.MediaItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MediaViewModel(private val mediaDao: MediaDao) : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val _mediaList = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaList: StateFlow<List<MediaItem>> get() = _mediaList

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
                    val media = snapshot.documents.mapNotNull { it.toObject(MediaItem::class.java) }
                    _mediaList.value = media

                    viewModelScope.launch {
                        mediaDao.clearMedia()
                        mediaDao.insertMedia(media)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("DEVAPPLOG", "Failed to fetch media: ${e.message}")
                }
        }
    }
}
