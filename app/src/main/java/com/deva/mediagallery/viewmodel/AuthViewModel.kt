package com.deva.mediagallery.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()


    private val _userId = MutableStateFlow(firebaseAuth.currentUser?.uid ?: "")
    val userId: StateFlow<String> get() = _userId

    init {
        // Listen for auth state changes
        firebaseAuth.addAuthStateListener { auth ->
            viewModelScope.launch {
                _userId.value = auth.currentUser?.uid ?: ""
            }
        }
    }

    private val _isUserLoggedIn = MutableStateFlow(firebaseAuth.currentUser != null)
    val isUserLoggedIn: StateFlow<Boolean> get() = _isUserLoggedIn

    fun signIn(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _isUserLoggedIn.value = true
                    _userId.value = firebaseAuth.currentUser?.uid ?: ""
                    onSuccess()
                } else {
                    val errorMsg = task.exception?.message ?: "Login failed"
                    Log.e("DEVAPPLOG", "Sign-in error: $errorMsg")
                    onError(errorMsg)
                }
            }
    }

    fun signOut() {
        try {
            firebaseAuth.signOut()
            _isUserLoggedIn.value = false
            _userId.value = ""
            Log.d("DEVAPPLOG", "User signed out successfully")
        } catch (e: Exception) {
            Log.e("DEVAPPLOG", "Error during sign-out", e)
        }
    }

    fun getCurrentUser(): FirebaseUser? {
        val user = firebaseAuth.currentUser
        if (user != null) {
            Log.d("DEVAPPLOG", "Current user: ${user.uid}, Email: ${user.email}")
        } else {
            Log.d("DEVAPPLOG", "No user is currently logged in")
        }
        return user
    }
}
