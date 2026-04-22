package com.pyrus.pyrusservicedesk.core

import android.app.Application
import androidx.media3.common.Player
import androidx.media3.session.MediaSession

internal class MediaSessionManager() {
    fun createMediaSessionWithRetry(
        application: Application,
        player: Player,
    ): MediaSession {

        try {
            return MediaSession.Builder(application, player)
                .setId(MEDIA_SESSION_ID)
                .build()
        }
        catch (e: Exception) {
            throw IllegalStateException("Failed to create MediaSession", e)
        }
    }

    companion object {
        private const val MEDIA_SESSION_ID = "psd_media_session"
    }
}