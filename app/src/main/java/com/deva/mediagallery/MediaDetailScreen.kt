import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.deva.mediagallery.MediaViewModel
import com.deva.mediagallery.data.MediaDatabase
import com.deva.mediagallery.data.MediaItemData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailScreen(mediaId: String, userId: String, navController: NavHostController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var media by remember { mutableStateOf<MediaItemData?>(null) }
    var fileSize by remember { mutableStateOf("Loading...") }
    var uploadedDate by remember { mutableStateOf("Loading...") }

    val db = MediaDatabase.getDatabase(context)
    val mediaViewModel: MediaViewModel =
        viewModel(factory = MediaViewModelFactory(db.mediaDao(), userId))


    val fileSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/jpeg")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                media?.url?.let { url -> saveFileToStorage(context, uri, url) }
            }
        }
    }

    LaunchedEffect(mediaId) {
        FirebaseFirestore.getInstance().collection("media").document(mediaId).get()
            .addOnSuccessListener { document ->
                document?.toObject(MediaItemData::class.java)?.let {
                    media = it
                    fileSize = "${document.getLong("size")?.div(1024)} KB"

                    val timestampMillis =
                        document.getLong("uploadedAt") ?: System.currentTimeMillis()
                    uploadedDate = java.text.SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()
                    ).format(java.util.Date(timestampMillis))
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to load media details", Toast.LENGTH_SHORT).show()
            }
    }

    media?.let { mediaItem ->
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Preview") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                Log.d("MediaDetailScreen", "Media URL: ${mediaItem.url}") //  Log media URL
                if (mediaItem.mimeType?.startsWith("video") == true) { //  Check mimeType for accuracy                    Log.d("MediaDetailScreen", "Detected Video File")
                    VideoPlayerComponent(mediaItem.url)
                } else {
                    Log.d("MediaDetailScreen", "Detected Image File")
                    Image(
                        painter = rememberAsyncImagePainter(mediaItem.url),
                        contentDescription = mediaItem.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(500.dp)
                    )
                }


                Spacer(modifier = Modifier.height(16.dp))

                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Name: ${mediaItem.name}", style = MaterialTheme.typography.bodyLarge)
                    Text("Size: $fileSize", style = MaterialTheme.typography.bodyMedium)
                    Text("Uploaded: $uploadedDate", style = MaterialTheme.typography.bodyMedium)

                    Spacer(modifier = Modifier.height(16.dp))

                    //  Download & Delete Buttons (Still Present)
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(onClick = { fileSaveLauncher.launch(mediaItem.name) }) {
                            Text("Download")
                        }

                        Button(
                            onClick = {

                                coroutineScope.launch {
                                    deleteMedia(
                                        mediaId,
                                        mediaItem.url,
                                        context,
                                        navController,
                                        mediaViewModel, userId
                                    ) //  Pass mediaViewModel
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}


suspend fun saveFileToStorage(context: Context, uri: Uri, fileUrl: String) {
    val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(fileUrl)

    try {
        val bytes = storageRef.getBytes(Long.MAX_VALUE).await()

        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(bytes)
                outputStream.flush()
            }
        }

        Toast.makeText(context, "File saved successfully", Toast.LENGTH_SHORT).show()
        Log.d("saveFileToStorage", "File saved at: $uri")

    } catch (e: Exception) {
        Log.e("saveFileToStorage", "Error saving file", e)
        Toast.makeText(context, "Failed to save file", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun VideoPlayerComponent(videoUrl: String) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(videoUrl)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp)
    )

    DisposableEffect(Unit) {
        onDispose {
            // 🔴 Stop and release player when exiting
            exoPlayer.playWhenReady = false
            exoPlayer.stop()
            exoPlayer.release()
        }
    }
}

suspend fun deleteMedia(
    mediaId: String,
    mediaUrl: String,
    context: Context,
    navController: NavHostController,
    viewModel: MediaViewModel, //  Pass ViewModel to update UI
    userId: String
) {
    val firestore = FirebaseFirestore.getInstance()
    val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(mediaUrl)

    Log.d("deleteMedia", "Deleting media: $mediaUrl")

    storageRef.delete()
        .addOnSuccessListener {
            Log.d("deleteMedia", "File deleted from storage")

            // 🔴 FIRST: Remove from Firestore
            firestore.collection("media").document(mediaId).delete()
                .addOnSuccessListener {
                    Log.d("deleteMedia", "Media deleted from Firestore")

                    // 🔴 SECOND: Remove from Room Database
                    viewModel.deleteMediaById(mediaId, userId) //  Delete from Room

                    Toast.makeText(context, "Media deleted", Toast.LENGTH_SHORT).show()

                    //  Refresh UI Immediately
                    viewModel.fetchMedia(userId) // 🔄 Fetch latest media list

                    navController.popBackStack() // Navigate back
                }
                .addOnFailureListener { e ->
                    Log.e("deleteMedia", "Failed to delete from Firestore", e)
                    Toast.makeText(context, "Failed to delete from Firestore", Toast.LENGTH_SHORT)
                        .show()
                }
        }
        .addOnFailureListener { e ->
            Log.e("deleteMedia", "Failed to delete from Storage", e)
            Toast.makeText(context, "Failed to delete from Storage", Toast.LENGTH_SHORT).show()
        }
}

