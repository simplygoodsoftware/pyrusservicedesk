package com.pyrus.pyrusservicedesk.core

import android.util.Log
import com.pyrus.pyrusservicedesk._ref.helpers.DownloadHelper
import com.pyrus.pyrusservicedesk.sdk.data.FileManager
import com.pyrus.pyrusservicedesk.sdk.repositories.DraftRepository
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.SdDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class CleanDataUseCase(
    private val coreScope: CoroutineScope,
    private val sdDatabase: SdDatabase,
    private val fileManager: FileManager,
    private val downloadHelper: DownloadHelper,
    private val draftRepository: DraftRepository,
) {

    operator fun invoke() {
        coreScope.launch(Dispatchers.IO) {
            sdDatabase.clearAllTables()
            fileManager.clearTempDir()
            downloadHelper.deleteAllDownloadedFiles()
            draftRepository.clearDrafts()
            Log.d("SDS", "cleanData done")
        }
    }


}