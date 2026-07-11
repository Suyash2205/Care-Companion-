package com.carecompanion.app.notify

import com.carecompanion.app.data.remote.SupabaseService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Receives FCM pushes (SOS + missed-dose alerts to guardians) and keeps the token fresh. */
@AndroidEntryPoint
class CareMessagingService : FirebaseMessagingService() {

    @Inject lateinit var api: SupabaseService

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { api.patchUser("eq.$uid", mapOf("fcm_token" to token)) }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        // Respect the guardian's in-app "notifications" preference.
        val enabled = getSharedPreferences("cc_guardian_settings", MODE_PRIVATE)
            .getBoolean("notifications_enabled", true)
        if (!enabled) return
        val title = message.notification?.title ?: message.data["title"] ?: "Care Companion"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        Notifications.showAlert(this, title, body)
    }
}
