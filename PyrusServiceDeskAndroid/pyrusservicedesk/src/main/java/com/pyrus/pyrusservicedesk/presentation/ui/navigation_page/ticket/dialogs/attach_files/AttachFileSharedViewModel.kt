package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.dialogs.attach_files

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.net.Uri

/**
 * Shared view model of the ticket activity.
 * Used for sharing state between activity and child fragments.
 */
internal class AttachFileSharedViewModel: ViewModel() {

    private val filePickedData = MutableLiveData<Uri?>()

    /**
     * Callback to be invoked when user picked a file in UI that is responsible for file picking.
     *
     * @param fileUri URI of the file that was picked.
     */
    fun onFilePicked(fileUri: Uri) {
        filePickedData.value = fileUri
        // Posting null is necessary to clear current value to prevent publishing recent uri again if
        // observer is connected.
        // Without post only null is received to an observer.
        filePickedData.postValue(null)
    }

    /**
     * Provides live data that delivers result of picking the file.
     * Result can contain null value that is used for cancelling the current data that is stored in
     * the live data. This provides one-shot behaviour.
     */
    fun getFilePickedLiveData(): LiveData<Uri?> = filePickedData
}