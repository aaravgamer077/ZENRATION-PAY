package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

private var activeAutoSignInJob: Job? = null

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener =
    FirebaseAuth.AuthStateListener { auth ->
      trySend(auth.currentUser)
    }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

private tailrec fun Context.findActivity(): Activity? =
  when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
  }

private fun resolveServerClientId(context: Context): String? {
  val direct =
    runCatching { context.getString(R.string.default_web_client_id).trim() }.getOrNull()
  if (!direct.isNullOrBlank()) return direct

  val prefix = runCatching { context.getString(R.string.oauth_web_client_prefix).trim() }.getOrNull()
  if (!prefix.isNullOrBlank()) {
    val hostSuffix = listOf("apps", "googleusercontent", "com").joinToString(".")
    return "$prefix.$hostSuffix"
  }
  return null
}

/**
 * Silent Auto-Sign-In on App Startup using GetGoogleIdOption in its own GetCredentialRequest.
 */
fun attemptAutoSignIn(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onUnauthenticated: () -> Unit,
  scope: CoroutineScope,
) {
  if (Firebase.auth.currentUser != null) {
    onAuthSuccess()
    return
  }
  val clientId = resolveServerClientId(context)
  if (clientId.isNullOrBlank()) {
    onUnauthenticated()
    return
  }

  val activityContext = context.findActivity() ?: context
  val googleIdOption =
    GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(true)
      .setServerClientId(clientId)
      .setAutoSelectEnabled(true)
      .build()

  val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

  activeAutoSignInJob?.cancel()
  activeAutoSignInJob =
    scope.launch {
      try {
        val result =
          withTimeoutOrNull(2000L) {
            credentialManager.getCredential(activityContext, request)
          }
        if (result == null) {
          onUnauthenticated()
          return@launch
        }
        val credential = result.credential
        if (
          credential is CustomCredential &&
            credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
          val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
          val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
          Firebase.auth.signInWithCredential(authCredential).await()
          onAuthSuccess()
        } else {
          onUnauthenticated()
        }
      } catch (e: Exception) {
        onUnauthenticated()
      }
    }
}

/**
 * Interactive Sign-In using GetSignInWithGoogleOption in its own GetCredentialRequest.
 */
fun onGoogleSignInClicked(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onAuthError: (String) -> Unit,
  scope: CoroutineScope,
  onAuthCancelled: () -> Unit = {},
) {
  // Cancel any in-flight background auto-sign-in so it never collides with interactive login
  activeAutoSignInJob?.cancel()
  activeAutoSignInJob = null

  val clientId = resolveServerClientId(context)
  if (clientId.isNullOrBlank()) {
    onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
    return
  }

  val activityContext = context.findActivity() ?: context
  val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
  val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

  scope.launch {
    try {
      val result = credentialManager.getCredential(activityContext, request)
      val credential = result.credential
      if (
        credential is CustomCredential &&
          credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
      ) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        Firebase.auth.signInWithCredential(authCredential).await()
        onAuthSuccess()
      } else {
        onAuthError("Unexpected credential type received from Google Sign-In")
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
      val msg = e.message.orEmpty()
      if (
        msg.contains("28444") ||
          msg.contains("Developer console", ignoreCase = true) ||
          msg.contains("10:", ignoreCase = true)
      ) {
        onAuthError(
          "Google Sign-In rejected APK certificate (Code 28444). Please install the updated APK signed with the registered Firebase debug keystore."
        )
      } else {
        onAuthCancelled()
      }
    } catch (e: Exception) {
      Log.e("Auth", "Primary Google Sign-In failed, attempting GoogleIdOption fallback", e)
      try {
        val fallbackOption =
          GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(false)
            .build()
        val fallbackRequest =
          GetCredentialRequest.Builder().addCredentialOption(fallbackOption).build()
        val fallbackResult = credentialManager.getCredential(activityContext, fallbackRequest)
        val credential = fallbackResult.credential
        if (
          credential is CustomCredential &&
            credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
          val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
          val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
          Firebase.auth.signInWithCredential(authCredential).await()
          onAuthSuccess()
        } else {
          onAuthError(e.localizedMessage ?: "Sign in failed. Please try again.")
        }
      } catch (cancelEx: GetCredentialCancellationException) {
        Log.w("Auth", "Fallback Google Sign-In cancelled: ${cancelEx.message}", cancelEx)
        onAuthCancelled()
      } catch (fallbackEx: Exception) {
        Log.e("Auth", "Fallback Google Sign-In also failed", fallbackEx)
        onAuthError(fallbackEx.localizedMessage ?: e.localizedMessage ?: "Sign in failed. Please try again.")
      }
    }
  }
}

/**
 * Sign-Out Flow: signs out of Firebase Auth AND clears Credential Manager state.
 */
fun signOut(
  credentialManager: CredentialManager,
  onSignOutComplete: () -> Unit,
  scope: CoroutineScope,
) {
  activeAutoSignInJob?.cancel()
  activeAutoSignInJob = null
  Firebase.auth.signOut()
  scope.launch {
    try {
      credentialManager.clearCredentialState(ClearCredentialStateRequest())
    } catch (e: Exception) {
      Log.e("Auth", "Failed to clear credential state", e)
    } finally {
      onSignOutComplete()
    }
  }
}
