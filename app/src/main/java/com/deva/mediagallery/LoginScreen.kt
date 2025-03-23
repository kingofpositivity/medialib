package com.deva.mediagallery

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.deva.mediagallery.viewmodel.AuthViewModel

@Composable
fun LoginScreen(navController: NavController, authViewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isUserLoggedIn by authViewModel.isUserLoggedIn.collectAsState()

    // ✅ Observe login state and navigate if successful
    LaunchedEffect(isUserLoggedIn) {
        if (isUserLoggedIn) {
            Log.d("DEVAPPLOG", "Login successful. Navigating to gallery")
            navController.navigate("gallery")
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                Log.d("DEVAPPLOG", "Attempting login for email: $email")
                authViewModel.signIn(
                    email,
                    password,
                    onSuccess = {
                        val userId = authViewModel.getCurrentUser()?.uid ?: "Unknown"
                        Log.d("DEVAPPLOG", "Firebase Login Success: User ID = $userId")
                        Log.d("DEVAPPLOG", "Navigating to gallery")
                        navController.navigate("gallery")
                    },
                    onError = { message ->
                        Log.e("DEVAPPLOG", "Login failed: $message")
                        errorMessage = message
                    }
                )
            }
        ) {
            Text("Login")
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}
