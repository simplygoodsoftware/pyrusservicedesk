package net.papirus.pyrusservicedesk.presentation.viewmodel

import android.animation.Animator
import android.animation.ValueAnimator
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.net.ConnectivityManager
import android.os.Handler
import android.view.animation.AccelerateInterpolator
import com.example.pyrusservicedesk.R
import kotlinx.coroutines.*
import net.papirus.pyrusservicedesk.PyrusServiceDesk

private const val PROGRESS_START_VALUE = 40
private const val PROGRESS_INCREMENT_VALUE = 20
private const val PROGRESS_ANIMATION_DURATION_MS_DEFAULT = 1000L
private const val PROGRESS_ANIMATION_DURATION_MS_QUICK = 400L

internal abstract class ConnectionViewModelBase(serviceDesk: PyrusServiceDesk)
    : AndroidViewModel(serviceDesk.application),
        CoroutineScope {

    protected val requests = serviceDesk.requestFactory
    protected val liveUpdates = serviceDesk.liveUpdates
    protected val localDataProvider = serviceDesk.localDataProvider
    protected val isNetworkConnected = MutableLiveData<Boolean>()
    private val connectivity: ConnectivityManager =
        serviceDesk.application.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager

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

    override val coroutineContext = Dispatchers.IO + Job()

    init {
        isNetworkConnected.value = connectivity.activeNetworkInfo?.isConnected ?: false
    }

    override fun onCleared() {
        super.onCleared()
        mainHandler.removeCallbacks(publishProgressRunnable)
        coroutineContext.cancel()
    }

    fun getIsNetworkConnectedLiveDate(): LiveData<Boolean> = isNetworkConnected
    fun getLoadingProgressLiveData(): LiveData<Int> = loadingProgress

    fun loadData(){
        replayProgress()
        onLoadData()
    }

    protected abstract fun onLoadData()

    private fun replayProgress() {
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
}
