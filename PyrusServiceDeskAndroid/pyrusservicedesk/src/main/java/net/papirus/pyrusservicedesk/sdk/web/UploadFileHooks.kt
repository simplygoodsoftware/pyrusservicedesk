package net.papirus.pyrusservicedesk.sdk.web

import android.os.CancellationSignal.OnCancelListener
import android.os.Handler
import android.os.Looper
import com.example.pyrusservicedesk.R
import net.papirus.pyrusservicedesk.PyrusServiceDesk

internal class UploadFileHooks {

    private val MAX_PERCENT = PyrusServiceDesk.getInstance().application.resources.getInteger(R.integer.psd_progress_max_value)
    private var recentProgress = 0
    private val cancellationSubscribers = mutableSetOf<OnCancelListener>()
    private var progressSubscription: ((Int) -> Unit)? = null
    private val uiHandler = Handler(Looper.getMainLooper())

    var isCancelled = false
        @Synchronized get() = field
        @Synchronized private set(value){ field = value }

    @Synchronized
    fun subscribeOnCancel(subscription: OnCancelListener) {
        cancellationSubscribers.add(subscription)
    }

    @Synchronized
    private fun destroy() {
        cancellationSubscribers.clear()
        progressSubscription = null
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
        isCancelled = true
        cancellationSubscribers.forEach{ it.onCancel() }
        destroy()
    }

    @Synchronized
    fun onProgressPercentChanged(newProgressPercent: Int){
        recentProgress = newProgressPercent
        val progressSubscriber = progressSubscription
        uiHandler.post{ progressSubscriber?.invoke(recentProgress) }
        if (newProgressPercent >= MAX_PERCENT)
            destroy()
    }
}