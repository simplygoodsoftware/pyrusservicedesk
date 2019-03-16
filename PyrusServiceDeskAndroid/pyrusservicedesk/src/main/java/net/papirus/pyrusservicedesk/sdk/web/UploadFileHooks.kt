package net.papirus.pyrusservicedesk.sdk.web

import android.os.Handler
import android.os.Looper

internal class UploadFileHooks {

    private var recentProgress = 0
    private var progressSubscription: ((Int) -> Unit)? = null
    private var cancelSubscription: (() -> Unit)? = null
    private val uiHandler = Handler(Looper.getMainLooper())

    fun subscribeOnCancel(subscription: () -> Unit) {
        cancelSubscription = subscription
    }

    fun unsubscribeFromCancel() {
        cancelSubscription = null
    }

    fun subscribeOnProgress(subscription: (progress: Int) -> Unit) {
        this.progressSubscription = subscription
        subscription.invoke(recentProgress)
    }

    fun unsubscribeFromProgress() {
        progressSubscription = null
    }

    fun cancelUploading() {
        cancelSubscription?.invoke()
    }

    fun onProgressPercentChanged(newProgressPercent: Int){
        recentProgress = newProgressPercent
        uiHandler.post{ progressSubscription?.invoke(recentProgress) }
    }
}