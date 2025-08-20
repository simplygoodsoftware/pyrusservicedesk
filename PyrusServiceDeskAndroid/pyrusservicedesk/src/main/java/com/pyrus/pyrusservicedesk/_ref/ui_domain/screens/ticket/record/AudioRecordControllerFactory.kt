package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.record

import com.pyrus.audiocontroller.record.AudioRecordController
import java.io.File

internal class AudioRecordControllerFactory(
    private val cacheDir: File
) {

    fun create(): AudioRecordController {
        return AudioRecordController.init(File(cacheDir.toString() + File.separator + "Recordings"))
    }

}