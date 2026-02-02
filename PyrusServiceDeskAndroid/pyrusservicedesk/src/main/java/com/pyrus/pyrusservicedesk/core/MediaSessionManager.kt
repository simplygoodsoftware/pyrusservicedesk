package com.pyrus.pyrusservicedesk.core

import android.app.Application
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import java.util.UUID

internal class MediaSessionManager() {
    fun createMediaSessionWithRetry(application: Application, player: Player): MediaSession {
        var exception: Exception? = null

        for (i in 1..3) {
            try {
                return MediaSession.Builder(application, player)
                    .setId("psd_session_retry_${i}_" + UUID.randomUUID().toString())
                    .build()
            } catch (e: NullPointerException) {
                exception = e
                if (i < 3) {
                    Thread.sleep(100L * i)
                }
            }
        }

        throw IllegalStateException(
            "Failed to create MediaSession",
            exception
        )
    }
}