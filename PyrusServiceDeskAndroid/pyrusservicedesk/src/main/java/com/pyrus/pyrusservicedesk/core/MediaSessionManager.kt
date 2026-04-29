package com.pyrus.pyrusservicedesk.core

import android.app.Application
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import java.util.UUID

internal class MediaSessionManager {
    fun createMediaSession(
        application: Application,
        player: Player,
    ): MediaSession {
        var lastError: Throwable? = null
        repeat(MAX_ATTEMPTS) {
            try {
                return MediaSession
                    .Builder(application, player)
                    .setId("psd_session_${UUID.randomUUID()}")
                    .build()
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw IllegalStateException(
            "Failed to create MediaSession after $MAX_ATTEMPTS attempts",
            lastError,
        )
    }

    private companion object {
        private const val MAX_ATTEMPTS = 3
    }
}
