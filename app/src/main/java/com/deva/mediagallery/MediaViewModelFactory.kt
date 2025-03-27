import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.deva.mediagallery.data.MediaDao

class MediaViewModelFactory(private val mediaDao: MediaDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MediaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MediaViewModel(mediaDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
