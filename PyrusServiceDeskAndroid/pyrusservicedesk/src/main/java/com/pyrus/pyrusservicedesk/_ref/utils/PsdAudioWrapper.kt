package com.pyrus.pyrusservicedesk._ref.utils

import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.uiInjector
import com.pyrus.pyrusservicedesk._ref.data.AudioData
import kotlinx.coroutines.flow.Flow

class PsdAudioWrapper : SdAudioWrapper {

    override fun getAudioDataFlow(url: String): Flow<AudioData> {
        val audioWrapper = uiInjector().audioWrapper
        return audioWrapper.getAudioDataFlow(url)
    }

    override fun playAudio(uri: String, fullUrl: String) {
        val audioWrapper = uiInjector().audioWrapper
        audioWrapper.playAudio(uri, fullUrl) //TODO kate for Helpy
    }

    override fun stop() {
        val audioWrapper = uiInjector().audioWrapper
        audioWrapper.stop()
    }

    override fun waitForSeek(position: Long, url: String) {
        val audioWrapper = uiInjector().audioWrapper
        audioWrapper.waitForSeek(position, url)
    }
}

interface SdAudioWrapper {

    fun getAudioDataFlow(url: String): Flow<AudioData>

    fun stop()

    fun waitForSeek(position: Long, url: String)

    fun playAudio(uri: String, fullUrl: String)

}