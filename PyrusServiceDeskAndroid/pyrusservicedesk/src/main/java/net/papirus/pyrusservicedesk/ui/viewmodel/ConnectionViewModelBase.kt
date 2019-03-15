package net.papirus.pyrusservicedesk.ui.viewmodel

import android.animation.Animator
import android.animation.ValueAnimator
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Handler
import android.support.v4.net.ConnectivityManagerCompat
import android.view.animation.AccelerateInterpolator
import com.example.pyrusservicedesk.R
import net.papirus.pyrusservicedesk.PyrusServiceDesk
import net.papirus.pyrusservicedesk.broadcasts.ReceiverBase
import net.papirus.pyrusservicedesk.sdk.updates.UpdateSubscriber

private const val PROGRESS_START_VALUE = 40
private const val PROGRESS_INCREMENT_VALUE = 20
private const val PROGRESS_ANIMATION_DURATION_MS_DEFAULT = 1000L
private const val PROGRESS_ANIMATION_DURATION_MS_QUICK = 400L

internal abstract class ConnectionViewModelBase(private val serviceDesk: PyrusServiceDesk)
    : AndroidViewModel(serviceDesk.application),
        UpdateSubscriber {

    val organizationName = serviceDesk.clientName

    protected val repository = serviceDesk.repository
    protected val isNetworkConnected = MutableLiveData<Boolean>()
    private val connectivity: ConnectivityManager =
        serviceDesk.application.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager
    private val networkReceiver = NetworkReceiver()

    private val MAX_PROGRESS = serviceDesk.application.resources.getInteger(R.integer.psd_progress_max_value)
    private val loadingProgress = MutableLiveData<Int>()
    private val mainHandler = Handler(serviceDesk.application.mainLooper)

    private var recentPublishedProgress = 0

    private val publishProgressRunnable = Runnable {
        val onCompleted = object: Runnable {
            override fun run() {
                if (recentPublishedProgress < MAX_PROGRESS - PROGRESS_INCREMENT_VALUE)
                    publishProgress(
                        recentPublishedProgress + PROGRESS_INCREMENT_VALUE,
                        PROGRESS_ANIMATION_DURATION_MS_DEFAULT,
                        this)
            }
        }
        publishProgress(PROGRESS_START_VALUE, PROGRESS_ANIMATION_DURATION_MS_DEFAULT, onCompleted)
    }

    init {
        serviceDesk.application.registerReceiver(networkReceiver, networkReceiver.getIntentFilter())
        isNetworkConnected.value = connectivity.activeNetworkInfo?.isConnected ?: false
    }

    override fun onCleared() {
        super.onCleared()
        repository.unsubscribeFromUpdates(this)
        serviceDesk.application.unregisterReceiver(networkReceiver)
        mainHandler.removeCallbacks(publishProgressRunnable)
    }

    fun getIsNetworkConnectedLiveDate(): LiveData<Boolean> = isNetworkConnected
    fun getLoadingProgressLiveData(): LiveData<Int> = loadingProgress

    abstract fun loadData()

    /**
     * Inheritors have to call this at the end of init to be properly subscribed on repository updates
     */
    protected fun onInitialized() {
        repository.subscribeToUpdates(this)
    }

    protected fun replayProgress() {
        loadingProgress.value = 0
        recentPublishedProgress = 0
        mainHandler.post(publishProgressRunnable)
    }

    protected fun onDataLoaded() {
        publishProgress(MAX_PROGRESS, PROGRESS_ANIMATION_DURATION_MS_QUICK, null)
    }

    protected fun publishProgress(progress: Int) {
        when (progress) {
            MAX_PROGRESS -> onDataLoaded()
            else -> publishProgress(progress, PROGRESS_ANIMATION_DURATION_MS_DEFAULT, null)
        }
    }

    private fun publishProgress(progress: Int, durationMs: Long, onCompleted: Runnable?) {
        recentPublishedProgress = progress
        ValueAnimator.ofInt(loadingProgress.value ?: 0, progress).apply {
            duration = durationMs
            interpolator = AccelerateInterpolator()
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {
                }

                override fun onAnimationCancel(animation: Animator?) {
                }

                override fun onAnimationStart(animation: Animator?) {
                }

                override fun onAnimationEnd(animation: Animator?) {
                    onCompleted?.run()
                }
            })
            addUpdateListener {
                (animatedValue as Int).let {
                    if (recentPublishedProgress > progress)
                        cancel()
                    loadingProgress.value = it
                }
            }
        }.also{ it.start() }
    }

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
