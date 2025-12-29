package com.pyrus.pyrusservicedesk.sdk.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import androidx.appcompat.app.AppCompatActivity.CONNECTIVITY_SERVICE
import com.pyrus.pyrusservicedesk.log.PLog

/**
 * Simple implementation of [ConnectivityManager.NetworkCallback].
 */
internal class SdNetworkCallback(
    appContext: Context,
    private val failDelay: FailDelay,
) : ConnectivityManager.NetworkCallback() {

    private val connectivityManager =
        appContext.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

    init {
        start()
    }

    fun start() {
        connectivityManager.registerNetworkCallback(
            NetworkRequest.Builder().build(),
            this
        )
    }

    fun stop() {
        connectivityManager.unregisterNetworkCallback(this)
    }

    override fun onAvailable(network: Network) {
        PLog.d(TAG, "onAvailable")
        failDelay.clear()
    }

    companion object {
        private const val TAG = "SdNetworkCallback"
    }

}