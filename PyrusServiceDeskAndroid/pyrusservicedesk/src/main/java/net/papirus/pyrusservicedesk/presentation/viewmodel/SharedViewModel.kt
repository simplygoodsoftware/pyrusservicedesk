package net.papirus.pyrusservicedesk.presentation.viewmodel

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel

internal class SharedViewModel : ViewModel() {

    private var quitServiceDesk = MutableLiveData<Boolean>()

    fun getQuitServiceDeskLiveData():LiveData<Boolean> = quitServiceDesk

    fun quitServiceDesk() {
        quitServiceDesk.value = true
    }

}
