package com.example.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.model.UserProfile
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "AuthRepository"

class AuthRepository(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val auth: FirebaseAuth = Firebase.auth
    private val credentialManager = CredentialManager.create(context)

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isSigningIn = MutableStateFlow(false)
    val isSigningIn: StateFlow<Boolean> = _isSigningIn.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
            if (firebaseAuth.currentUser != null) {
                syncUserProfile(firebaseAuth.currentUser!!)
            }
        }
    }

    private fun getWebClientId(): String? {
        return try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            Log.e(TAG, "Missing default_web_client_id resource", e)
            null
        }
    }

    fun attemptSilentAutoSignIn(onResult: (Boolean) -> Unit = {}) {
        val user = auth.currentUser
        if (user != null) {
            onResult(true)
            return
        }

        val clientId = getWebClientId() ?: run {
            onResult(false)
            return
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    auth.signInWithCredential(authCredential).await()
                    onResult(true)
                } else {
                    onResult(false)
                }
            } catch (e: Exception) {
                Log.d(TAG, "Silent auto sign-in not available: ${e.message}")
                onResult(false)
            }
        }
    }

    fun signInWithGoogle(
        activity: Activity,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val clientId = getWebClientId()
        if (clientId.isNullOrEmpty()) {
            val msg = "Google Sign-In configuration missing: default_web_client_id not found"
            _authError.value = msg
            onError(msg)
            return
        }

        _isSigningIn.value = true
        _authError.value = null

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = auth.signInWithCredential(authCredential).await()
                    val user = authResult.user
                    if (user != null) {
                        syncUserProfile(user)
                    }
                    _isSigningIn.value = false
                    onSuccess()
                } else {
                    val msg = "Unexpected credential format received"
                    _isSigningIn.value = false
                    _authError.value = msg
                    onError(msg)
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
                _isSigningIn.value = false
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In error", e)
                val msg = e.localizedMessage ?: "Sign in failed"
                _isSigningIn.value = false
                _authError.value = msg
                onError(msg)
            }
        }
    }

    fun signOut(onComplete: () -> Unit = {}) {
        auth.signOut()
        scope.launch {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear credential state", e)
            } finally {
                _currentUser.value = null
                onComplete()
            }
        }
    }

    fun clearError() {
        _authError.value = null
    }

    private fun syncUserProfile(user: FirebaseUser) {
        scope.launch {
            try {
                val dbId = context.getString(R.string.firestore_database_id)
                val firestore = FirebaseFirestore.getInstance(dbId)
                val userRef = firestore.collection("users").document(user.uid)
                val profile = UserProfile(
                    userId = user.uid,
                    displayName = user.displayName ?: "Creator",
                    email = user.email ?: "",
                    photoUrl = user.photoUrl?.toString() ?: ""
                )
                userRef.set(profile).await()
            } catch (e: Exception) {
                Log.w(TAG, "Error syncing user profile: ${e.message}")
            }
        }
    }
}
