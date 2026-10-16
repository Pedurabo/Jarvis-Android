package com.example.jarvis.core.auth

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(
    private val auth: FirebaseAuth
) {
    private val TAG = "AuthManager"

    private val _user = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val user: StateFlow<FirebaseUser?> = _user.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _user.value = firebaseAuth.currentUser
        }
    }

    val userId: String?
        get() = auth.currentUser?.uid

    val isAnonymous: Boolean
        get() = auth.currentUser?.isAnonymous ?: true

    suspend fun signInAnonymously(): Result<FirebaseUser> {
        return try {
            val result = auth.signInAnonymously().await()
            val user = result.user ?: throw Exception("Sign in failed: User is null")
            _user.value = user
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Anonymous sign in failed", e)
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }
}
