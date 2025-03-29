package com.deva.mediagallery

import MediaViewModel
import MediaViewModelFactory
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.deva.mediagallery.data.MediaDatabase
import com.deva.mediagallery.data.MediaItemData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaGalleryScreen(navController: NavHostController, mediaViewModel: MediaViewModel) {
    val context = LocalContext.current
    val db = MediaDatabase.getDatabase(context)
    val viewModel: MediaViewModel = viewModel(factory = MediaViewModelFactory(db.mediaDao()))
    val mediaList by viewModel.mediaList.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var selectedMedia by remember { mutableStateOf<MediaItemData?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    Log.d("MediaGallery", "Total media fetched: ${mediaList.size}")
    mediaList.forEach { media ->
        val isVideo = media.url.endsWith(".mp4") || media.url.endsWith(".mov")  // Extend for other formats
        Log.d("MediaGallery", "Media Type: ${if (isVideo) "Video" else "Photo"}, URL: ${media.url}")
}
    val pickMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            Log.d("MediaPicker", "Selected Media: $it")
            isUploading = true
            Toast.makeText(context, "Uploading...", Toast.LENGTH_SHORT).show()
            coroutineScope.launch {
                uploadMediaToFirebase(it, FirebaseStorage.getInstance(), FirebaseFirestore.getInstance(), viewModel, context)
                isUploading = false
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { pickMedia.launch("*/*") }) {
                Icon(Icons.Default.Add, contentDescription = "Upload Media")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            if (isUploading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            if (mediaList.isEmpty()) {
                Text("No media available.", modifier = Modifier.padding(16.dp))
            } else {
                LazyVerticalGrid(columns = GridCells.Adaptive(120.dp), modifier = Modifier.padding(8.dp)) {
                    items(mediaList) { media ->
                        val isVideo = media.url.contains(".mp4") || media.url.contains(".mov") // Add more formats if needed

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp)
                                .clickable { selectedMedia = media },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isVideo) {
                                VideoPreview(media.url) // Show video preview instead of thumbnail                            }
                            }  else {
                                AsyncImage(
                                    model = media.url,
                                    contentDescription = media.name,
                                    modifier = Modifier.fillMaxWidth(),
                                    contentScale = ContentScale.Crop,
                                    alignment = Alignment.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedMedia?.let { media ->
        FullScreenPreview(media) { selectedMedia = null }
    }
}

@Composable
fun VideoPlayer(videoUrl: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AndroidView(
                factory = {
                    PlayerView(it).apply {
                        player = exoPlayer
                        useController = true // Show player controls
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}


@Composable
fun FullScreenPreview(media: MediaItemData, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (media.url.endsWith(".mp4")) {
                FullScreenVideoPlayer(media.url,onDismiss)
            } else {
                AsyncImage(
                    model = media.url,
                    contentDescription = media.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.Center
                )
            }
        }
    }
}



@Composable
fun VideoPreview(videoUrl: String) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = false // Ensure it doesn't autoplay in the grid
        }
    }

    AndroidView(
        factory = {
            PlayerView(it).apply {
                player = exoPlayer
                useController = false // Hide controls for preview
            }
        },
        modifier = Modifier.fillMaxWidth().height(120.dp) // Set preview size
    )
}
@Composable
fun FullScreenVideoOrImage(media: MediaItemData, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (media.url.endsWith(".mp4")) {
                FullScreenVideoPlayer(media.url, onDismiss)
            } else {
                AsyncImage(
                    model = media.url,
                    contentDescription = media.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.Center
                )
            }
        }
    }
}

@Composable
fun FullScreenVideoPlayer(videoUrl: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AndroidView(
                factory = {
                    PlayerView(it).apply {
                        player = exoPlayer
                        useController = true
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}



suspend fun uploadMediaToFirebase(
    uri: Uri,
    storage: FirebaseStorage,
    firestore: FirebaseFirestore,
    viewModel: MediaViewModel,
    context: android.content.Context
) {
    val isVideo = context.contentResolver.getType(uri)?.startsWith("video") == true
    val fileExtension = if (isVideo) "mp4" else "jpg"
    val fileName = "media/${UUID.randomUUID()}.$fileExtension"
    val storageRef = storage.reference.child(fileName)

    storageRef.putFile(uri)
        .addOnProgressListener { taskSnapshot ->
            val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
            Log.d("UploadProgress", "Upload is $progress% complete")
            Toast.makeText(context, "Upload in progress... $progress%", Toast.LENGTH_SHORT).show()
        }
        .addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                val mediaItemData = MediaItemData(
                    name = "Uploaded Media",
                    url = downloadUrl.toString(),
                    type = if (isVideo) "video" else "photo" // ✅ Store type in Firestore
                )
                firestore.collection("media").add(mediaItemData)
                    .addOnSuccessListener {
                        Log.d("Firebase", "Media uploaded successfully")
                        Toast.makeText(context, "Upload Successful", Toast.LENGTH_SHORT).show()
                        viewModel.fetchMedia()
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firebase", "Error adding media to Firestore", e)
                        Toast.makeText(context, "Upload Failed", Toast.LENGTH_SHORT).show()
                    }
            }
        }
        .addOnFailureListener { e ->
            Log.e("Firebase", "Upload failed", e)
            Toast.makeText(context, "Upload Failed", Toast.LENGTH_SHORT).show()
        }
}

