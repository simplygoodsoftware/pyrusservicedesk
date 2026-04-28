package com.pyrus.pyrusservicedesk.core

import android.app.Application
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import java.util.UUID

internal class MediaSessionManager() {
    fun createMediaSessionWithRetry(
        application: Application,
        player: Player,
    ): MediaSession {
        try {
            return MediaSession.Builder(application, player)
                .setId("psd_session" + UUID.randomUUID().toString())
                .build()
        }
        catch (e: Exception) {
            throw IllegalStateException(
                "Failed to create MediaSession",
                e
            )
        }
    }
}