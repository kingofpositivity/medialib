package com.deva.mediagallery

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.deva.mediagallery.ui.theme.MediaLibraryTheme
import com.deva.mediagallery.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            Log.d("AuthDebug", "Current user: ${auth.currentUser?.email}")
        }

        try {
            setContent {
                MediaLibraryTheme {
                    val navController = rememberNavController()

                    // Ensure viewModel is properly used
                    val authViewModel: AuthViewModel = viewModel()

                    val isUserLoggedIn = FirebaseAuth.getInstance().currentUser != null

                    NavHost(
                        navController = navController,
                        startDestination = if (isUserLoggedIn) "gallery" else "login"
                    ) {
                        composable("login") {
                            LoginScreen(navController, authViewModel)
                        }
                        composable("gallery") {
                            MediaGalleryScreen(navController)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error initializing UI", e)
        }
    }
}
