package com.deva.mediagallery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.deva.mediagallery.viewmodel.MediaViewModel

@Composable
fun MediaGalleryScreen(navController: NavController, mediaViewModel: MediaViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val mediaList by mediaViewModel.mediaList.collectAsState()

    LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize()) {
        items(mediaList.size) { index ->
            val media = mediaList[index]
            Card(
                modifier = Modifier
                    .padding(8.dp)
                    .clickable { /* Navigate to details screen */ }
            ) {
                AsyncImage(model = media.url, contentDescription = "Media Item")
            }
        }
    }
}
