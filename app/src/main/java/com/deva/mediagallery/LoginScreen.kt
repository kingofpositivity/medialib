package com.deva.mediagallery

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.deva.mediagallery.viewmodel.AuthViewModel
import android.util.Patterns

@Composable
fun LoginScreen(navController: NavController, authViewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isUserLoggedIn by authViewModel.isUserLoggedIn.collectAsState()

    LaunchedEffect(isUserLoggedIn) {
        if (isUserLoggedIn) {
            Log.d("DEVAPPLOG", "Login successful. Navigating to gallery")
            navController.navigate("gallery")
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it.trim() },
            label = { Text("Email") },
            singleLine = true,
            isError = errorMessage != null
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it.trim() },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            isError = errorMessage != null
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                errorMessage = when {
                    email.isBlank() -> "Please enter an email."
                    !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Invalid email format."
                    password.isBlank() -> "Please enter a password."
                    else -> null
                }

                if (errorMessage == null) {
                    Log.d("DEVAPPLOG", "Attempting login for email: $email")
                    authViewModel.signIn(
                        email,
                        password,
                        onSuccess = {
                            val userId = authViewModel.getCurrentUser()?.uid ?: "Unknown"
                            Log.d("DEVAPPLOG", "Firebase Login Success: User ID = $userId")
                            navController.navigate("gallery")
                        },
                        onError = { message ->
                            Log.e("DEVAPPLOG", "Login failed: $message")
                            errorMessage = message
                        }
                    )
                }
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
