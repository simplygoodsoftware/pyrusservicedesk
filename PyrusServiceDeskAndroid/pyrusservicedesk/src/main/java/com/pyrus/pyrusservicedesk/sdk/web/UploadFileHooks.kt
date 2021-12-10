package com.pyrus.pyrusservicedesk.sdk.web


import android.os.Handler
import android.os.Looper
import androidx.annotation.Keep

/**
 * Class that is used for propagating events while file uploading in progress.
 * This exposes events on changing of the uploading progress and provides the opportunity
 * to cancel uploading.
 * All method invocations are thread safe.
 */
@Keep
internal class UploadFileHooks {

    private var recentProgress = 0
    private val cancellationSubscribers = mutableSetOf<OnCancelListener>()
    private var progressSubscription: ((Int) -> Unit)? = null
    private val uiHandler = Handler(Looper.getMainLooper())

    /**
     * Property that defines whether uploading of the file was cancelled.
     */
    var isCancelled = false
        @Synchronized get
        @Synchronized private set

    @Synchronized
    fun resetProgress() {
        recentProgress = 0
    }

    /**
     * Append [subscription] on uploading cancellation.
     */
    @Synchronized
    fun subscribeOnCancel(subscription: OnCancelListener) {
        cancellationSubscribers.add(subscription)
    }

    /**
     * Assigns [subscription] on progress and automatically delivers recent stored progress.
     */
    @Synchronized
    fun subscribeOnProgress(subscription: (progress: Int) -> Unit) {
        this.progressSubscription = subscription
        subscription.invoke(recentProgress)
    }

    /**
     * Clear progress subscriber.
     */
    @Synchronized
    fun unsubscribeFromProgress() {
        progressSubscription = null
    }

    /**
     * Switches to [isCancelled] == true and notifies cancellation subscribers
     */
    @Synchronized
    fun cancelUploading() {
        isCancelled = true
        cancellationSubscribers.forEach{ it.onCancel() }
        destroy()
    }

    /**
     * Notifies progress subscribers that progress has been changed to [newProgressPercent]
     */
    @Synchronized
    fun onProgressPercentChanged(newProgressPercent: Int){
        recentProgress = newProgressPercent
        val progressSubscriber = progressSubscription
        uiHandler.post{ progressSubscriber?.invoke(recentProgress) }
    }

    @Synchronized
    private fun destroy() {
        cancellationSubscribers.clear()
        progressSubscription = null
    }
}

/**
 * Listens for cancellation.
 */
internal interface OnCancelListener {
    /**
     * Called when [UploadFileHooks.cancel] is invoked.
     */
    fun onCancel()
}