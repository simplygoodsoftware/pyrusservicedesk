package net.papirus.pyrusservicedesk.presentation.usecases.ticket

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.net.Uri

internal class TicketSharedViewModel: ViewModel() {

    private val filePickedData = MutableLiveData<Uri>()

    fun onFilePicked(fileUri: Uri) {
        filePickedData.value = fileUri
        // Posting null is necessary to clear current value to prevent publishing recent uri again if
        // observer is connected.
        // Without post only null is received to an observer.
        filePickedData.postValue(null)
    }

    fun getFilePickedLiveData(): LiveData<Uri> = filePickedData
}