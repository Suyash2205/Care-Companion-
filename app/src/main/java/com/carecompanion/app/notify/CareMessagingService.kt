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
        val title = message.notification?.title ?: message.data["title"] ?: "Care Companion"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        val kind = message.data["kind"].orEmpty()

        // An SOS is a life-safety alert and must ALWAYS break through: a guardian who
        // muted routine notifications must still learn their elder triggered an emergency.
        // Every other alert kind (e.g. missed dose) respects the in-app preference.
        val isEmergency = kind.contains("sos", ignoreCase = true)
        if (!isEmergency) {
            val enabled = getSharedPreferences("cc_guardian_settings", MODE_PRIVATE)
                .getBoolean("notifications_enabled", true)
            if (!enabled) return
        }

        Notifications.showAlert(this, title, body)
    }
}
