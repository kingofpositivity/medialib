package com.deva.mediagallery

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.deva.mediagallery.ui.theme.MediaLibraryTheme
import com.deva.mediagallery.viewmodel.AuthViewModel
import com.deva.mediagallery.viewmodel.MediaViewModel
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()
    private val mediaViewModel: MediaViewModel by viewModels()

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach {
            Log.d("Permissions", "${it.key} = ${it.value}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            Log.d("AuthDebug", "Current user: ${auth.currentUser?.email}")
        }

        try {
            requestStoragePermissions()

            setContent {
                MediaLibraryTheme {
                    val navController = rememberNavController()
                    val isUserLoggedIn = FirebaseAuth.getInstance().currentUser != null

                    NavHost(
                        navController = navController,
                        startDestination = if (isUserLoggedIn) "gallery" else "login"
                    ) {
                        composable("login") {
                            LoginScreen(navController, authViewModel)
                        }
                        composable("gallery") {
                            val mediaViewModel: MediaViewModel = viewModel()
                            MediaGalleryScreen(navController, mediaViewModel)
                        }

                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error initializing UI", e)
        }
    }

    private fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            requestPermissions.launch(
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                )
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions.launch(
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
                )
            )
        } else {
            requestPermissions.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
        }
    }
}
