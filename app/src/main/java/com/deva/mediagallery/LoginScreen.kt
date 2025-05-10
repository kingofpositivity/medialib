package com.deva.mediagallery

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import android.util.Patterns
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.deva.mediagallery.viewmodel.AuthViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun LoginScreen(navController: NavController, authViewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork
    val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
    var isPasswordVisible by remember { mutableStateOf(false) } // Track password visibility
    val isInternetAvailable = networkCapabilities != null && networkCapabilities.hasCapability(
        NetworkCapabilities.NET_CAPABILITY_INTERNET
    )

    var isDayTime by remember { mutableStateOf(true) } // Toggle State

    val isUserLoggedIn by authViewModel.isUserLoggedIn.collectAsState()

    LaunchedEffect(isUserLoggedIn) {
        if (isUserLoggedIn) {
            val userId = authViewModel.getCurrentUser()?.uid ?: return@LaunchedEffect
            Log.d("DEVAPPLOG", "Login successful. Navigating to gallery/$userId")
            navController.navigate("gallery/$userId") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    // Background Image & Theme Colors
    val bgImage = if (isDayTime) R.drawable.day else R.drawable.night
    val textColor = if (isDayTime) Color.Black else Color.White
    val buttonColor = if (isDayTime) Color.Black else Color(0xFFFFAC07) // Gold color for night mode


    // Ensure that content doesn't collide with the status bar or system UI


    val backgroundOverlay =
        if (isDayTime) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f)

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = bgImage),
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Overlay for visibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundOverlay)
        )

        // Toggle Theme Button (Top-Right)
        Box(
            modifier = Modifier
                .fillMaxWidth() // Ensures the Box fills the width of the screen
                .padding(16.dp) // Adds padding to the entire Box
                .padding(top = 64.dp), // Adds margin from the top
            contentAlignment = Alignment.TopEnd // Aligns content to the top-right corner
        ) {
            Text(
                text = if (isDayTime) "🌙 Night Mode" else "☀️ Day Mode",
                fontSize = 16.sp,
                color = textColor,
                modifier = Modifier
                    .clickable { isDayTime = !isDayTime } // Toggle Theme
                    .padding(8.dp)
            )
        }

        // Login Form
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome to Media Library Application!",
                fontSize = 24.sp,
                color = textColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it.trim() },
                label = { Text("Email", color = textColor) },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(color = textColor),
                isError = errorMessage != null
            )

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it.trim() },
                label = { Text("Password", color = textColor) },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(color = textColor),
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            painter = painterResource(id = if (isPasswordVisible) R.drawable.ic_eye_open else R.drawable.ic_eye_closed),
                            contentDescription = if (isPasswordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                isError = errorMessage != null
            )


            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {

                    if (!isInternetAvailable) {
                        errorMessage = "This app requires an internet connection to login."
                        return@Button
                    }
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
                                navController.navigate("gallery/$userId") {
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            onError = { message ->
                                Log.e("DEVAPPLOG", "Login failed: $message")
                                // Custom error message
                                errorMessage = when {
                                    message.contains("ERROR_WRONG_PASSWORD") -> "Incorrect password. Please check and try again."
                                    message.contains("ERROR_USER_NOT_FOUND") -> "No user found with this email. Please check your email or contact admin at agmin@samplea.com."
                                    else ->
                                        "Please check your email and password. If you don’t have a login, please contact admin at admin@sample.com"
                                }
                            }
                        )
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
            ) {
                Text("Login", color = Color.White)
            }

            errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

