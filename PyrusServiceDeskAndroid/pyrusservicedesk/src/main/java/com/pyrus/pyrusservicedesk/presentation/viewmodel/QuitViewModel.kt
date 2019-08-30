package com.pyrus.pyrusservicedesk.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * For sharing state that user has completely quit the service desk.
 * All activities rect on this event by finishing itself.
 */
internal class QuitViewModel : ViewModel() {

    private var quitServiceDesk = MutableLiveData<Boolean>()

    /**
     * Provides live data to be subscribed to events that user completely quit service desk.
     */
    fun getQuitServiceDeskLiveData():LiveData<Boolean> = quitServiceDesk

    /**
     * Should be called when it necessary to completely quit service desk.
     */
    fun quitServiceDesk() {
        quitServiceDesk.value = true
        quitServiceDesk.postValue(null)
    }

}
