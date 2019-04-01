package net.papirus.pyrusservicedesk.presentation

import android.arch.lifecycle.Observer
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import com.example.pyrusservicedesk.R
import kotlinx.android.synthetic.main.psd_activity_tickets.*
import kotlinx.android.synthetic.main.psd_no_connection.*
import net.papirus.pyrusservicedesk.presentation.ui.view.swiperefresh.DirectedSwipeRefresh
import net.papirus.pyrusservicedesk.presentation.viewmodel.ConnectionViewModelBase
import net.papirus.pyrusservicedesk.utils.ConfigUtils
import net.papirus.pyrusservicedesk.utils.getColor
import net.papirus.pyrusservicedesk.utils.getViewModel

private const val ANIMATION_DURATION = 200L

/**
 * Base class for activities that are able to show progress and connection error.
 * Appropriate view model for this activity is lazily loaded by [getViewModel].
 */
internal abstract class ConnectionActivityBase<T: ConnectionViewModelBase>(viewModelClass: Class<T>)
    : ActivityBase() {

    /**
     * Instance of the view model that is defined by the particular type of the activity.
     */
    protected val viewModel: T by getViewModel(viewModelClass)

    /**
     * Optional id of the swiperefreshlayout that is used for launch loading the data from [viewModel]
     */
    abstract val refresherViewId:Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        progress_bar?.progressDrawable?.setColorFilter(
                getColor(this, R.attr.colorAccentSecondary),
                PorterDuff.Mode.SRC_IN)
        reconnect.setOnClickListener { reconnect() }
        reconnect.setTextColor(ConfigUtils.getAccentColor(this))
        (findViewById(refresherViewId) as? DirectedSwipeRefresh)?.setOnRefreshListener {
            viewModel.loadData()
        }
    }

    override fun startObserveData() {
        super.startObserveData()
        viewModel.getIsNetworkConnectedLiveDate().observe(
                this,
                Observer { isConnected ->
                    isConnected?.let {
                        no_connection.visibility = if (it) GONE else VISIBLE
                    }
                }
        )
        viewModel.getLoadingProgressLiveData().observe(
            this,
            Observer { progress ->
                progress?.let { updateProgress(it) }
            }
        )
    }

    /**
     * Changes progress of the progress bar and handles its appearance and disappearance.
     */
    protected open fun updateProgress(newProgress: Int) {
        when {
            newProgress >= resources.getInteger(R.integer.psd_progress_max_value) -> {
                progress_bar
                    .animate()
                    .alpha(0f)
                    .setDuration(ANIMATION_DURATION)
                    .start()
            }
            else -> progress_bar.alpha = 1f
        }
        progress_bar.progress = newProgress
    }

    /**
     * Called when user tries to reconnect to the network.
     */
    protected open fun reconnect() {
        viewModel.loadData()
    }
}