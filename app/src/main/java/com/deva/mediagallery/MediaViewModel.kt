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
        firestore.collection("media").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("DEVAPPLOG", "Failed to listen for media updates: ${error.message}")
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val media = snapshot.documents.mapNotNull { it.toObject(MediaItemData::class.java) }
                Log.d("DEVAPPLOG", "Live update received: ${media.size} items")

                _mediaList.value = media

                viewModelScope.launch {
                    mediaDao.clearMedia()
                    mediaDao.insertMedia(media)
                }
            }
        }
    }
}
