package com.pyrus.pyrusservicedesk._ref.utils

import android.media.MediaMetadataRetriever
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.datasource.HttpDataSource
import androidx.media3.session.MediaSession
import com.pyrus.pyrusservicedesk._ref.audio.opus.OpusFile
import com.pyrus.pyrusservicedesk._ref.audio.opus.OpusStatistics
import com.pyrus.pyrusservicedesk._ref.data.AudioData
import com.pyrus.pyrusservicedesk._ref.helpers.DownloadHelper
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.fingerprints.AudioStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.Executors


internal class AudioWrapper(
    private val session: MediaSession,
    private val downloadHelper: DownloadHelper,
    private val coroutineScope: CoroutineScope,
) {

    private var downloadingFileJob: Job? = null
    private var seekBarUpdateJob: Job? = null

    private val eventStateFlow = MutableStateFlow<Int>(0)

    // Id of current audio file in player
    private var currentAudioFileUrl: String? = null
    private val audioPositions = HashMap<String, Long>()
    private val audioDurations = HashMap<String, Long>()
    private val downloadingFiles = HashSet<String>()
    private var isPlaying = false
    private var isSeeking = false
    private var audioInFocus: String? = null
    private var mainActivityIsActive = false


    init {
        session.player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(
                mediaItem: MediaItem?,
                reason: Int,
            ) {
                val durationMs = session.player.duration
                val fileUrl = currentAudioFileUrl
                if (durationMs != C.TIME_UNSET && fileUrl != null) {
                    audioDurations[fileUrl] = durationMs
                    triggerEvent()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        val durationMs = session.player.duration
                        val fileUrl = currentAudioFileUrl
                        if (durationMs != C.TIME_UNSET && fileUrl != null) {
                            audioDurations[fileUrl] = durationMs
                            triggerEvent()
                        }
                    }

                    Player.STATE_ENDED -> {
                        val fileUrl = currentAudioFileUrl
                        if (fileUrl != null) {
                            audioPositions[fileUrl] = 0
                            triggerEvent()
                        }
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                this@AudioWrapper.isPlaying = isPlaying
                isSeeking = if (isPlaying) false else isSeeking
                if (!isSeeking) {
                    triggerEvent()
                }
                if (isPlaying) {
                    startProgressUpdates()
                }
                else {
                    stopProgressUpdates()
                }
            }

            //TODO kate
            override fun onPlayerError(error: PlaybackException) {
                when (val cause = error.cause) {
                    is HttpDataSource.InvalidResponseCodeException -> {
                        when (cause.responseCode) {
                            503 -> {
                                retryPlayback()
                            }

                            else -> {}
                        }
                    }
                }
            }

            private fun retryPlayback() {
                //TODO kate
            }
        })
    }

    fun getAudioDataFlow(url: String): Flow<AudioData> {
        return eventStateFlow.map {
            getAudioData(url)
        }
    }

    private fun getAudioData(url: String): AudioData {
        val fileIsExist = getAudioFile(url).exists()
        if (!fileIsExist && !downloadingFiles.contains(url)) return AudioData(
            position = 0,
            status = AudioStatus.None,
            audioFullTime = null,
            audioCurrentTime = null,
            url = url,
        )

        if (downloadingFiles.contains(url)) return AudioData(
            position = 0,
            status = AudioStatus.Processing,
            audioFullTime = null,
            audioCurrentTime = null,
            url = url,
        )

        if (currentAudioFileUrl == url && isPlaying) return AudioData(
            position = getPosition(url)?.toInt() ?: 0,
            status = AudioStatus.Playing,
            audioFullTime = getDuration(url),
            audioCurrentTime = getRemainingTime(url),
            url = url,
        )

        return AudioData(
            position = getPosition(url)?.toInt() ?: 0,
            status = AudioStatus.Paused,
            audioFullTime = getDuration(url),
            audioCurrentTime = getRemainingTime(url),
            url = url,
        )

    }

    private fun getDuration(url: String): Long? {
        val duration = audioDurations[url]
        if (currentAudioFileUrl != url) {
            return duration
        }
        return when {
            duration == null || duration < session.player.duration -> session.player.duration
            else -> duration
        }
    }

    private fun setAudioFileDuration(url: String) {
        Executors.newCachedThreadPool().submit {
            var statistics: OpusStatistics
            try {
                OpusFile(
                    getAudioFile(
                        url
                    )
                ).use { opus ->
                    statistics =
                        OpusStatistics(
                            opus
                        )
                    statistics.calculate()
                    synchronized(this) {
                        val durationInMillis: Double = (statistics.audioPacketsCount
                                * statistics.getAvgPacketDuration())
                        audioDurations[url] = durationInMillis.toLong()
                        triggerEvent()
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun File.getMediaDuration(): Long {
        if (!exists()) return 0
        val inputStream = FileInputStream(absolutePath)
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(inputStream.getFD())
        }
        catch(ex: Exception) {
            Log.w("AudioWrapper", "File.getMediaDuration(): exception=${ex.message}\nabsolutePath=$absolutePath")
        }

        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        retriever.release()
        inputStream.close()
        return duration?.toLongOrNull() ?: 0
    }

    private fun getPosition(url: String): Long? {
        val position = audioPositions[url]
        if (currentAudioFileUrl != url) {
            return position
        }
        return when {
            position != 0L || session.player.playbackState != STATE_ENDED -> {
                if (position != null && position < session.player.currentPosition) {
                    audioPositions[url] = session.player.currentPosition
                    session.player.currentPosition
                }
                else position
            }
            else -> position
        }
    }

    private fun getRemainingTime(url: String): Long {
        val position = getPosition(url) ?: 0
        val duration = getDuration(url) ?: 0
        return duration - position
    }

    private fun getAudioFile(uri: String): File {
        val audioFile = runCatching { File(uri) }.getOrNull()
        if (audioFile != null && audioFile.exists()) {
            return audioFile
        }
        val musicDir = File(downloadHelper.userAttachmentsDir(), "AudioAttachments")
        return File(musicDir, uri)
    }

    fun playAudio(uri: String, fullUrl: String) {
        val audioFile = getAudioFile(uri)
        audioInFocus = uri

        if (uri in downloadingFiles) {
            downloadingFileJob?.cancel()
            downloadingFiles.remove(uri)
            triggerEvent()
        }
        else if (audioFile.exists()) {
            togglePlayback(uri, audioFile, getPosition(uri) ?: 0)
        }
        else {
            downloadingFileJob = coroutineScope.launch(Dispatchers.IO) {
                downloadingFiles += uri
                triggerEvent()
                val downloadRequestTry = downloadHelper.downloadFile(audioFile, fullUrl, coroutineContext)
                downloadingFiles.remove(uri)
                if (downloadRequestTry.isSuccess()) {
                    val duration = getAudioFile(uri).getMediaDuration()
                    audioDurations[uri] = duration
                    triggerEvent()
                    withContext(Dispatchers.Main) {
                        if (audioInFocus == uri && mainActivityIsActive) {
                            playAudio(uri, fullUrl)
                        }
//                        if (currentAudioFileUrl == url) {
//                            playAudio(url)
//                        }
                    }
                }
                else {
                    triggerEvent()
                    downloadRequestTry.error.printStackTrace()
                }
            }
        }
    }

    private fun togglePlayback(url: String, audioFile: File, position: Long) {
        if (currentAudioFileUrl != url) {
            prepareAudio(url, audioFile, position)
        }
        if (currentAudioFileUrl != null
            && currentAudioFileUrl.equals(url)
        ) {
            if (session.player.isPlaying) {
                session.player.pause()
                return
            }
            // If current audio file already played and ended, play it again from start
            if (session.player.playbackState == Player.STATE_ENDED) {
                val audioUrl = currentAudioFileUrl
                if (audioUrl != null) {
                    audioPositions.remove(audioUrl)
                    triggerEvent()
                }
                session.player.seekTo(0, 0L)
            }

            if (session.player.playbackState == Player.STATE_IDLE) {
                session.player.prepare()
            }
            // Start playing
            session.player.play()
            return
        }
        stop()
        currentAudioFileUrl = url
        session.player.play()
    }

    private fun prepareAudio(uid: String, audioFile: File, position: Long) {
        // Stop current audio file and clear playlist
        currentAudioFileUrl?.let { stop() }
        currentAudioFileUrl = uid
        session.player.stop()
        session.player.clearMediaItems()

        // Add new audio to playlist
        session.player.addMediaItem(
            MediaItem.fromUri(audioFile.toURI().toString())
        )
        // Set start play position
        session.player.seekTo(0, position)

        session.player.prepare()
    }

    private fun startProgressUpdates() {
        seekBarUpdateJob?.cancel()
        seekBarUpdateJob = CoroutineScope(Dispatchers.Main).launch {
            while (session.player.isPlaying) {
                val audioUrl = currentAudioFileUrl
                if (audioUrl != null) {
                    audioPositions[audioUrl] = session.player.currentPosition
                    triggerEvent()
                }
                delay(20)
            }
        }
    }

    fun waitForSeek(position: Long, url: String) {
        audioPositions[url] = position
        if (currentAudioFileUrl == url) {
            isSeeking = true
            if (!session.player.isPlaying)
                session.player.playWhenReady = false
            session.player.seekTo(position)
        }
    }

    fun setAudioDurations(audioList: List<String?>) {
        for (audioUrl in audioList) {
            val start = audioUrl?.indexOf("DownloadFile/")?.plus("DownloadFile/".length) ?: 0
            val end = audioUrl?.indexOf("?user_id=") ?: 0
            val url = audioUrl?.substring(start, end)
            if (!url.isNullOrEmpty() && getAudioFile(url).exists()) {
                val duration = getAudioFile(url).getMediaDuration()
                audioDurations[url] = duration
            }
        }
    }

    fun updateMainActivityIsActive(isActive: Boolean) {
        mainActivityIsActive = isActive
    }

    private fun triggerEvent() {
        eventStateFlow.value = eventStateFlow.value + 1
    }

    private fun stopProgressUpdates() {
        seekBarUpdateJob?.cancel()
        seekBarUpdateJob = null
    }

    fun stop() {
        session.player.stop()
    }

    fun pause() {
        if (session.player.isPlaying) {
            session.player.pause()
        }
    }

    fun pauseIf(uri: String) {
        if (currentAudioFileUrl == uri) pause()
    }

    fun clearPositions() {
        audioPositions.clear()
        triggerEvent()
    }

    fun clearCurrentUrl() {
        currentAudioFileUrl = null
    }

}