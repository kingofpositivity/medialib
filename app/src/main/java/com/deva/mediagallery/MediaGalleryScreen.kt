package com.deva.mediagallery

import com.deva.mediagallery.data.MediaDatabase
import MediaViewModel
import MediaViewModelFactory
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.room.Room

@OptIn(ExperimentalMaterial3Api::class)
@Composable

fun MediaGalleryScreen(navController: NavHostController,mediaViewModel: MediaViewModel) {
    val context = LocalContext.current
    val db = remember { Room.databaseBuilder(context, MediaDatabase::class.java, "media_db").build() }
    val mediaDao = remember { db.mediaDao() }

    // ✅ Correct way to initialize ViewModel
    val viewModel: MediaViewModel = viewModel(
        factory = MediaViewModelFactory(mediaDao)
    )

    val mediaList by viewModel.mediaList.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { /* Upload logic */ }) {
                Icon(Icons.Default.Add, contentDescription = "Upload Media")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            if (mediaList.isEmpty()) {
                Text("No media available.", modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn {
                    items(mediaList) { media ->
                        Text(text = media.name)
                    }
                }
            }
        }
    }
}
