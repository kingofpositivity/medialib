package com.deva.mediagallery

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.deva.mediagallery.viewmodel.MediaViewModel

@Composable
fun MediaGalleryScreen(navController: NavHostController, mediaViewModel: MediaViewModel) {    val mediaList by mediaViewModel.mediaList.collectAsState()
    val context = LocalContext.current

    // File Picker to Select Image
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            Log.d("DEVAPPLOG", "Selected Image URI: $it")
            mediaViewModel.uploadMedia(it, context) // Upload image and refresh gallery
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(
            onClick = { imagePickerLauncher.launch("image/*") }, // Open File Picker
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Upload Media")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (mediaList.isEmpty()) {
            Text("No media available.", modifier = Modifier.padding(16.dp))
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize()) {
                items(mediaList.size) { index ->
                    val media = mediaList[index]
                    Card(
                        modifier = Modifier
                            .padding(8.dp)
                            .clickable { /* Navigate to details screen if needed */ }
                    ) {
                        AsyncImage(model = media.url, contentDescription = "Media Item")
                    }
                }
            }
        }
    }
}
