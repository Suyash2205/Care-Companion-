package com.carecompanion.app.auth

import android.app.Activity
import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Result of requesting an OTP: either auto-verified (instant) or a code was sent. */
sealed class OtpRequest {
    data class AutoVerified(val credential: PhoneAuthCredential) : OtpRequest()
    data class CodeSent(val verificationId: String) : OtpRequest()
}

@Singleton
class FirebaseAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    val currentUid: String? get() = auth.currentUser?.uid
    val isSignedIn: Boolean get() = auth.currentUser != null

    /** Request an OTP for [phone] (E.164). Test numbers auto-verify. */
    suspend fun requestOtp(activity: Activity, phone: String): OtpRequest =
        requestOtpOn(auth, activity, phone)

    /** Complete sign-in with the SMS code the user typed. */
    suspend fun signInWithCode(verificationId: String, code: String) {
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        auth.signInWithCredential(credential).await()
    }

    suspend fun signInWithCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential).await()
    }

    fun signOut() = auth.signOut()

    /**
     * Verify the ELDER's phone from the GUARDIAN's device without disturbing the
     * guardian's session, using a secondary FirebaseApp. Returns the elder's
     * Firebase UID (which the elder self-links on later), then signs the secondary
     * instance out. Used during elder-profile setup (verified-at-creation).
     */
    suspend fun verifyElderPhone(activity: Activity, phone: String, code: String, verificationId: String): String {
        val secondary = secondaryAuth()
        try {
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            val result = secondary.signInWithCredential(credential).await()
            return result.user?.uid ?: error("no elder uid")
        } finally {
            secondary.signOut()
        }
    }

    /** Request an OTP on the SECONDARY instance (used for elder verification). */
    suspend fun requestElderOtp(activity: Activity, phone: String): OtpRequest =
        requestOtpOn(secondaryAuth(), activity, phone)

    suspend fun verifyElderCredential(credential: PhoneAuthCredential): String {
        val secondary = secondaryAuth()
        try {
            val result = secondary.signInWithCredential(credential).await()
            return result.user?.uid ?: error("no elder uid")
        } finally {
            secondary.signOut()
        }
    }

    private fun secondaryAuth(): FirebaseAuth {
        val name = "elderVerify"
        val app = FirebaseApp.getApps(context).firstOrNull { it.name == name }
            ?: run {
                val primary = FirebaseApp.getInstance()
                FirebaseApp.initializeApp(context, primary.options, name)
            }
        return FirebaseAuth.getInstance(app)
    }

    private suspend fun requestOtpOn(target: FirebaseAuth, activity: Activity, phone: String): OtpRequest =
        suspendCancellableCoroutine { cont ->
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    if (cont.isActive) cont.resume(OtpRequest.AutoVerified(credential))
                }
                override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                    if (cont.isActive) cont.resumeWithException(e)
                }
                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    if (cont.isActive) cont.resume(OtpRequest.CodeSent(verificationId))
                }
            }
            val options = PhoneAuthOptions.newBuilder(target)
                .setPhoneNumber(phone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build()
            PhoneAuthProvider.verifyPhoneNumber(options)
        }
}
