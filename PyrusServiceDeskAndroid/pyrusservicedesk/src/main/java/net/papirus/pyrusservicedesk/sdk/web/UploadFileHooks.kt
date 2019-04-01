package net.papirus.pyrusservicedesk.sdk.web

import android.os.CancellationSignal.OnCancelListener
import android.os.Handler
import android.os.Looper
import com.example.pyrusservicedesk.R
import net.papirus.pyrusservicedesk.PyrusServiceDesk

/**
 * Class that is used for propagating events while file uploading in progress.
 * This exposes events on changing of the uploading progress and provides the opportunity
 * to cancel uploading.
 * All method invocations are thread safe.
 * Automatically disposes subscribers when cancellation was invoked or progress reached [MAX_PERCENT]
 */
internal class UploadFileHooks {

    private val MAX_PERCENT = PyrusServiceDesk.getInstance().application.resources.getInteger(R.integer.psd_progress_max_value)
    private var recentProgress = 0
    private val cancellationSubscribers = mutableSetOf<OnCancelListener>()
    private var progressSubscription: ((Int) -> Unit)? = null
    private val uiHandler = Handler(Looper.getMainLooper())

    /**
     * Property that defines whether uploading of the file was cancelled.
     */
    var isCancelled = false
        @Synchronized get() = field
        @Synchronized private set(value){ field = value }

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
        if (newProgressPercent >= MAX_PERCENT)
            destroy()
    }

    @Synchronized
    private fun destroy() {
        cancellationSubscribers.clear()
        progressSubscription = null
    }
}