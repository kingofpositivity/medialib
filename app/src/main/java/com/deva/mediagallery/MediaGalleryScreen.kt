package com.deva.mediagallery

import MediaViewModelFactory
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.database.getStringOrNull
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.deva.mediagallery.data.MediaDatabase
import com.deva.mediagallery.data.MediaItemData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaGalleryScreen(
    navController: NavHostController,
    userId: String,
    mediaViewModel: MediaViewModel
) {
    // Get the current context
    val context = LocalContext.current

    // Initialize Room database instance
    val db = MediaDatabase.getDatabase(context)

    // Create ViewModel instance using factory pattern
    val viewModel: MediaViewModel =
        viewModel(factory = MediaViewModelFactory(db.mediaDao(), userId))

    // Collect media list from ViewModel state
    val mediaList by viewModel.mediaList.collectAsState()

    // Coroutine scope for background operations
    val coroutineScope = rememberCoroutineScope()

    // Track selected media for preview
    var selectedMedia by remember { mutableStateOf<MediaItemData?>(null) }

    // Track media upload status
    var isUploading by remember { mutableStateOf(false) }

    // Debug log for fetched media items
    Log.d("MediaGallery", "Total media fetched: ${mediaList.size}")
    mediaList.forEach { media ->
        val isVideo = media.url.endsWith(".mp4") || media.url.endsWith(".mov")
        Log.d("MediaGallery", "Media Type: ${if (isVideo) "Video" else "Photo"}, URL: ${media.url}")
    }

    // Initialize Firebase Authentication
    val firebaseAuth = remember { mutableStateOf(FirebaseAuth.getInstance()) }
    val ownerId = firebaseAuth.value.currentUser?.uid ?: "anonymous" // Get logged-in user ID

    // Observe lifecycle events to refresh media list
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.clearMedia()  // Clears Room database
                viewModel.fetchMedia(userId)  // Fetches updated Firestore data
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Handle media selection from device storage

    val pickMedia =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let {
                Log.d("MediaPicker", "Selected Media: $it")
                isUploading = true
                Toast.makeText(context, "Uploading...", Toast.LENGTH_SHORT).show()
                coroutineScope.launch {
                    uploadMediaToFirebase(
                        it,
                        FirebaseStorage.getInstance(),
                        FirebaseFirestore.getInstance(),
                        viewModel,
                        context,
                        ownerId
                    )
                    isUploading = false
                }
            }
        }

    // Scaffold layout with floating action button for media upload
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GALLERY") },
                actions = {
                    LogoutButton(onLogout = {
                        firebaseAuth.value.signOut()
                        // ✅ Navigate to login and clear the back stack
                        navController.navigate("login") {
                            popUpTo("gallery") { inclusive = true }
                        }
                    })
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
            }) {
                Icon(Icons.Default.Add, contentDescription = "Upload Media")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            // Show loading indicator during upload
            if (isUploading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            // Display message if media list is empty
            if (mediaList.isEmpty()) {
                Text(
                    "Please click the Add button at the bottom to upload media.",
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                // Display media in a grid layout
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(120.dp),
                    modifier = Modifier.padding(8.dp)
                ) {
                    items(mediaList) { media ->
                        val isVideo = media.url.contains(".mp4") || media.url.contains(".mov")

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)  // Makes sure it's a square
                                .padding(6.dp)
                                .clip(RoundedCornerShape(12.dp))  // Rounded corners
                                .border(
                                    1.dp,
                                    Color.Gray.copy(alpha = 0.3f),
                                    RoundedCornerShape(12.dp)
                                ) // Subtle border
                                .background(Color.Black)
                                .shadow(4.dp, RoundedCornerShape(12.dp))  // Soft shadow
                                .clickable {
                                    navController.navigate("mediaDetail/${media.id}") // Navigate to media detail screen
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isVideo) {
                                Box(

                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                        .clip(RectangleShape), // Ensures the video stays within bounds
                                    contentAlignment = Alignment.Center
                                ) {
                                    VideoPreview(media.url) // Show video preview

                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp) // Set play button size
                                                .shadow(
                                                    8.dp,
                                                    shape = CircleShape
                                                ) // Add shadow for visibility
                                                .background(
                                                    Color.Black.copy(alpha = 0.4f),
                                                    CircleShape
                                                ), // Dark background for contrast
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Play Video",
                                                tint = Color.White,
                                                modifier = Modifier.size(32.dp) // Adjust icon size
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Display image preview
                                AsyncImage(
                                    model = media.url,
                                    contentDescription = media.name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp),
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


}

@Composable
fun LogoutButton(onLogout: () -> Unit) {
    IconButton(onClick = onLogout) {
        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
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
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp) // Set preview size
    )
}

suspend fun uploadMediaToFirebase(
    uri: Uri,
    storage: FirebaseStorage,
    firestore: FirebaseFirestore,
    viewModel: MediaViewModel,
    context: Context,
    ownerId: String // Add owner ID for multi-user support
) {
    val resolver = context.contentResolver

    // Get file metadata
    val fileSize = resolver.openAssetFileDescriptor(uri, "r")?.length ?: 0L
    val fileName = resolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME)
        cursor.moveToFirst()
        cursor.getStringOrNull(nameIndex) ?: "unknown"
    } ?: "unknown"

    val mimeType = resolver.getType(uri) ?: "unknown"
    val isVideo = mimeType.startsWith("video")
    val fileExtension = if (isVideo) "mp4" else "jpg"
    val uniqueFileName = "media/${UUID.randomUUID()}.$fileExtension"
    val storageRef = storage.reference.child(uniqueFileName)

    // Get video metadata (duration, width, height)
    var duration: Long? = null
    var width: Int? = null
    var height: Int? = null
    if (isVideo) {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, uri)
        duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
        width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt()
        height =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt()
        retriever.release()
    }

    storageRef.putFile(uri)
        .addOnSuccessListener {
            // Retrieve download URL after successful upload
            storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                val newMediaRef = firestore.collection("media").document()
                val mediaItemData = MediaItemData(
                    id = newMediaRef.id,
                    name = fileName,
                    url = downloadUrl.toString(),
                    type = if (isVideo) "video" else "photo",
                    size = fileSize,
                    uploadedAt = System.currentTimeMillis(), // Store upload timestamp
                    duration = duration,
                    width = width,
                    height = height,
                    mimeType = mimeType,
                    ownerId = ownerId // Store owner ID
                )

                // Save media metadata in Firestore
                newMediaRef.set(mediaItemData)
                    .addOnSuccessListener {
                        Log.d("Firebase", "Media uploaded successfully with ID: ${newMediaRef.id}")
                        Toast.makeText(context, "Upload Successful", Toast.LENGTH_SHORT).show()
                        viewModel.fetchMedia(ownerId) // Refresh media list
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



