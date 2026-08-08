package com.carecompanion.app.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.carecompanion.app.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** What the caller needs to show/store after a successful Google sign-in. */
data class GoogleSignInResult(
    val uid: String,
    val email: String?,
    val displayName: String?,
)

/**
 * Google Sign-In via Credential Manager (the current API — GoogleSignInClient is
 * deprecated). Phone OTP was removed: linking an elder to their profile is now done
 * with a one-time invite code instead of matching phone numbers.
 */
@Singleton
class FirebaseAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    val currentUid: String? get() = auth.currentUser?.uid
    val isSignedIn: Boolean get() = auth.currentUser != null
    val currentEmail: String? get() = auth.currentUser?.email
    val currentDisplayName: String? get() = auth.currentUser?.displayName

    /**
     * Show the Google account picker and sign in to Firebase with the chosen account.
     *
     * [activityContext] must be an Activity context — Credential Manager renders a
     * system dialog and cannot use the application context.
     *
     * @param filterByAuthorized when true, only accounts already used with this app are
     *   offered (a faster returning-user path). We pass false so every Google account on
     *   the device is offered, including first-time sign-ins.
     */
    suspend fun signInWithGoogle(
        activityContext: Context,
        filterByAuthorized: Boolean = false,
    ): GoogleSignInResult {
        check(BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()) {
            "Google sign-in is not configured (missing web client id in google-services.json)"
        }

        val option = GetGoogleIdOption.Builder()
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setFilterByAuthorizedAccounts(filterByAuthorized)
            .setAutoSelectEnabled(false)
            .build()

        val response = CredentialManager.create(context)
            .getCredential(activityContext, GetCredentialRequest.Builder().addCredentialOption(option).build())

        val googleCredential = GoogleIdTokenCredential.createFrom(response.credential.data)
        val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
        val user = auth.signInWithCredential(firebaseCredential).await().user
            ?: error("Sign-in did not return a user")

        return GoogleSignInResult(
            uid = user.uid,
            email = user.email ?: googleCredential.id,
            displayName = user.displayName ?: googleCredential.displayName,
        )
    }

    fun signOut() = auth.signOut()
}
