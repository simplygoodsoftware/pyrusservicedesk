package net.papirus.pyrusservicedesk.ui.viewmodel

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.support.v4.net.ConnectivityManagerCompat
import net.papirus.pyrusservicedesk.PyrusServiceDesk
import net.papirus.pyrusservicedesk.broadcasts.ReceiverBase
import net.papirus.pyrusservicedesk.repository.updates.UpdateSubscriber

internal abstract class ConnectionViewModelBase(private val serviceDesk: PyrusServiceDesk)
    : ViewModel(),
        UpdateSubscriber {

    protected val repository = serviceDesk.repository
    protected val isNetworkConnected = MutableLiveData<Boolean>()

    private val connectivity: ConnectivityManager =
            serviceDesk.application.getSystemService(Context.CONNECTIVITY_SERVICE)
                    as ConnectivityManager
    private val networkReceiver = NetworkReceiver()

    init {
        serviceDesk.application.registerReceiver(networkReceiver, networkReceiver.getIntentFilter())
        isNetworkConnected.value = connectivity.activeNetworkInfo?.isConnected ?: false
    }

    override fun onCleared() {
        super.onCleared()
        repository.unsubscribeFromUpdates(this)
        serviceDesk.application.unregisterReceiver(networkReceiver)
    }

    fun getIsNetworkConnectedLiveDate(): LiveData<Boolean> = isNetworkConnected

    /**
     * Inheritors have to call this at the end of init to be properly subscribed on repository updates
     */
    protected fun onInitialized() {
        repository.subscribeToUpdates(this)
    }

    abstract fun loadData()

    inner class NetworkReceiver : ReceiverBase() {
        override fun getIntentFilter() = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)

        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                isNetworkConnected.value =
                        ConnectivityManagerCompat
                                .getNetworkInfoFromBroadcast(connectivity, it)?.isConnected ?: false
            }
        }
    }
}
