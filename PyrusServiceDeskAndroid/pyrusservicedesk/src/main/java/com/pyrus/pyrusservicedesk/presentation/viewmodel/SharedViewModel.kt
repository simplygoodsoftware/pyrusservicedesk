package com.pyrus.pyrusservicedesk.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * For sharing state that user has completely quit the service desk or there need to upgrade
 * the feed.
 */
internal class SharedViewModel : ViewModel() {

    private val quitServiceDesk = MutableLiveData<Boolean>()
    private val updateServiceDesk = MutableLiveData<Any>()

    /**
     * Provides live data to be subscribed to events that user completely quit service desk.
     */
    fun getQuitServiceDeskLiveData(): LiveData<Boolean> = quitServiceDesk

    /**
     * Should be called when it necessary to completely quit service desk.
     */
    fun quitServiceDesk() = quitServiceDesk.postValue(true)

    /**
     * Should be called before service desk start.
     */
    fun clearQuitServiceDesk() = quitServiceDesk.postValue(false)

    /**
     * Provides live data to be subscribed to events that app want to update feed.
     */
    fun getUpdateServiceDeskLiveData(): LiveData<Any> = updateServiceDesk

    /**
     * Triggers manually update of PyrusServiceDesk's feed.
     */
    fun triggerUpdate() = updateServiceDesk.postValue(Any())

}
