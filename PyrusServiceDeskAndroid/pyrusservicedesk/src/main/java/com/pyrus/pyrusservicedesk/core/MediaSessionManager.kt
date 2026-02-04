package com.pyrus.pyrusservicedesk.core

import android.app.Application
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import java.util.UUID

internal class MediaSessionManager() {
    fun createMediaSessionWithRetry(application: Application, player: Player): MediaSession {
        var exception: Exception? = null

        for (i in 1..MAX_RETRIES) {
            try {
                return MediaSession.Builder(application, player)
                    .setId("psd_session_retry_${i}_" + UUID.randomUUID().toString())
                    .build()
            } catch (e: Exception) {
                exception = e
                if (i < MAX_RETRIES) {
                    Thread.sleep(RETRY_DELAY_MS * i)
                }
            }
        }

        throw IllegalStateException(
            "Failed to create MediaSession",
            exception
        )
    }

    companion object {
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 100L
    }
}