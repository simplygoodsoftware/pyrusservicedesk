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
internal class UploadFileHook {

    private var recentProgress = 0
    private var cancellationListener: (() -> Unit)? = null
    private var progressListener: ((Int) -> Unit)? = null
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
     * Append [onCancel] on uploading cancellation.
     */
    @Synchronized
    fun setCancelListener(onCancel: () -> Unit) {
        this.cancellationListener = onCancel
    }

    /**
     * Assigns [listener] on progress and automatically delivers recent stored progress.
     */
    @Synchronized
    fun setProgressListener(listener: (progress: Int) -> Unit) {
        this.progressListener = listener
        listener.invoke(recentProgress)
    }

    /**
     * Clear progress subscriber.
     */
    @Synchronized
    fun unsubscribeFromProgress() {
        progressListener = null
    }

    /**
     * Switches to [isCancelled] == true and notifies cancellation subscribers
     */
    @Synchronized
    fun cancelUploading() {
        isCancelled = true
        cancellationListener?.invoke()
        destroy()
    }

    /**
     * Notifies progress subscribers that progress has been changed to [newProgressPercent]
     */
    @Synchronized
    fun onProgressPercentChanged(newProgressPercent: Int){
        recentProgress = newProgressPercent
        val progressSubscriber = progressListener
        uiHandler.post{ progressSubscriber?.invoke(recentProgress) }
    }

    @Synchronized
    private fun destroy() {
        cancellationListener = null
        progressListener = null
    }
}