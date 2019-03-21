package net.papirus.pyrusservicedesk.sdk.web

import android.os.Handler
import android.os.Looper

internal class UploadFileHooks {

    private var recentProgress = 0
    private var progressSubscription: ((Int) -> Unit)? = null
    private var cancelSubscription: (() -> Unit)? = null
    private val uiHandler = Handler(Looper.getMainLooper())

    @Synchronized
    fun subscribeOnCancel(subscription: () -> Unit) {
        cancelSubscription = subscription
    }

    @Synchronized
    fun unsubscribeFromCancel() {
        cancelSubscription = null
    }

    @Synchronized
    fun subscribeOnProgress(subscription: (progress: Int) -> Unit) {
        this.progressSubscription = subscription
        subscription.invoke(recentProgress)
    }

    @Synchronized
    fun unsubscribeFromProgress() {
        progressSubscription = null
    }

    @Synchronized
    fun cancelUploading() {
        cancelSubscription?.invoke()
    }

    @Synchronized
    fun onProgressPercentChanged(newProgressPercent: Int){
        recentProgress = newProgressPercent
        uiHandler.post{ progressSubscription?.invoke(recentProgress) }
    }
}