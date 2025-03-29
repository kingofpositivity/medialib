package com.deva.mediagallery
import MediaViewModel
import MediaViewModelFactory
import android.net.Uri
import android.util.Log
import com.deva.mediagallery.data.MediaItemData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID


import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.deva.mediagallery.data.MediaDatabase
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaGalleryScreen(navController: NavHostController, mediaViewModel: MediaViewModel) {
    val context = LocalContext.current
    val db = MediaDatabase.getDatabase(context) // FIXED: No remember here
    val viewModel: MediaViewModel = viewModel(factory = MediaViewModelFactory(db.mediaDao()))
    val mediaList by viewModel.mediaList.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) } // Loading state

    // Firebase instances
    val storage = FirebaseStorage.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    val pickMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            Log.d("MediaPicker", "Selected Image: $it")
            isLoading = true
            coroutineScope.launch {
                uploadMediaToFirebase(it, storage, firestore, viewModel)
                isLoading = false
            }
        }
    }

    LaunchedEffect(mediaList) {
        viewModel.fetchMedia() // Ensure list updates after upload
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { pickMedia.launch("image/*") }) {
                Icon(Icons.Default.Add, contentDescription = "Upload Media")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp)) // Show loading indicator
            } else if (mediaList.isEmpty()) {
                Text("No media available.", modifier = Modifier.padding(16.dp))
            } else {
                LazyVerticalGrid(columns = GridCells.Adaptive(120.dp), modifier = Modifier.padding(8.dp)) {
                    items(mediaList) { media ->
                        AsyncImage(
                            model = media.url,
                            contentDescription = media.name,
                            modifier = Modifier.fillMaxWidth().padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}

suspend fun uploadMediaToFirebase(
    uri: Uri,
    storage: FirebaseStorage,
    firestore: FirebaseFirestore,
    viewModel: MediaViewModel
) {
    val fileName = "media/${UUID.randomUUID()}.jpg"
    val storageRef = storage.reference.child(fileName)

    storageRef.putFile(uri)
        .addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                val mediaItemData = MediaItemData(name = "Uploaded Image", url = downloadUrl.toString())

                firestore.collection("media").add(mediaItemData)
                    .addOnSuccessListener {
                        Log.d("Firebase", "Media uploaded and stored in Firestore")
                        viewModel.fetchMedia() // 🔹 Force fetch after upload
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firebase", "Error adding media to Firestore", e)
                    }
            }
        }
        .addOnFailureListener { e ->
            Log.e("Firebase", "Upload failed", e)
        }
}
