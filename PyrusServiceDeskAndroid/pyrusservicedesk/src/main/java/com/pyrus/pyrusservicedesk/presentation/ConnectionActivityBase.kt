package com.pyrus.pyrusservicedesk.presentation

import androidx.lifecycle.Observer
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.ProgressBar
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.presentation.viewmodel.ConnectionViewModelBase
import com.pyrus.pyrusservicedesk.utils.ConfigUtils
import com.pyrus.pyrusservicedesk.utils.getColorByAttrId
import com.pyrus.pyrusservicedesk.utils.getViewModel

private const val ANIMATION_DURATION = 200L

/**
 * Base class for activities that are able to show progress and connection error.
 * Appropriate view model for this activity is lazily loaded by [getViewModel].
 */

// TODO remove this class and replace all logic to new TicketActivity
internal abstract class ConnectionActivityBase<T: ConnectionViewModelBase>(viewModelClass: Class<T>)
    : ActivityBase() {

    /**
     * Instance of the view model that is defined by the particular type of the activity.
     */
    protected val viewModel: T by getViewModel(viewModelClass)

    /**
     * Optional id of the swiperefreshlayout that is used for launch loading the data from [viewModel]
     */
    protected abstract val refresherViewId:Int
    protected abstract val progressBarViewId: Int

    private var refresher: com.pyrus.pyrusservicedesk.presentation.ui.view.swiperefresh.DirectedSwipeRefresh? = null
    private var progressBar: ProgressBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        progressBar = findViewById(progressBarViewId)
        progressBar?.progressDrawable?.setColorFilter(
                getColorByAttrId(this, R.attr.colorAccentSecondary),
                PorterDuff.Mode.SRC_IN)
//        reconnectButton.setOnClickListener { reconnect() }
//        reconnectButton.setTextColor(ConfigUtils.getAccentColor(this))
        refresher = findViewById(refresherViewId)
        refresher?.setOnRefreshListener { viewModel.loadData() }
    }

    override fun startObserveData() {
        super.startObserveData()
        viewModel.getIsNetworkConnectedLiveData().observe(
            this,
            { isConnected ->
                isConnected?.let {
//                    no_connection.visibility = if (it) GONE else VISIBLE
                }
            }
        )
        viewModel.getLoadingProgressLiveData().observe(
            this,
            { progress ->
                progress?.let { updateProgress(it) }
            }
        )
        sharedViewModel.getUpdateServiceDeskLiveData().observe(
            this,
            {
                viewModel.loadData()
            }
        )
    }

    /**
     * Changes progress of the progress bar and handles its appearance and disappearance.
     */
    protected open fun updateProgress(newProgress: Int) {
        when {
            newProgress >= resources.getInteger(R.integer.psd_progress_max_value) -> {
                progressBar
                    ?.animate()
                    ?.alpha(0f)
                    ?.setDuration(ANIMATION_DURATION)
                    ?.start()
                refresher?.isRefreshing = false
            }
            else -> {
                refresher?.isRefreshing = true
                progressBar?.alpha = 1f
            }
        }
        progressBar?.progress = newProgress
    }

    /**
     * Called when user tries to reconnect to the network.
     */
    protected open fun reconnect() {
        viewModel.loadData()
    }
}