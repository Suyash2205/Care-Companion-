package com.carecompanion.app.notify

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/** Receives FCM pushes (SOS + missed-dose alerts to guardians). Fleshed out in the SOS phase. */
class CareMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // TODO(sos-phase): persist token to users.fcm_token when signed in.
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: "Care Companion"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        Notifications.showAlert(this, title, body)
    }
}
