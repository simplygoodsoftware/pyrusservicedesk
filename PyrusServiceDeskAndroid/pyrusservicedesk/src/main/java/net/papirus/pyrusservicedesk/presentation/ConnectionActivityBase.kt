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
import net.papirus.pyrusservicedesk.utils.getColor
import net.papirus.pyrusservicedesk.utils.getViewModel

private const val ANIMATION_DURATION = 200L

internal abstract class ConnectionActivityBase<T: ConnectionViewModelBase>(viewModelClass: Class<T>)
    : ActivityBase() {

    protected val viewModel: T by getViewModel(viewModelClass)

    abstract val refresherViewId:Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        progress_bar?.progressDrawable?.setColorFilter(
                getColor(this, R.attr.colorAccentSecondary),
                PorterDuff.Mode.SRC_IN)
        reconnect.setOnClickListener { reconnect() }
        findViewById<DirectedSwipeRefresh>(refresherViewId)?.setOnRefreshListener {
            viewModel.loadData()
        }
    }

    override fun observeData() {
        super.observeData()
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
            Observer { progress -> progress?.let { updateProgress(it) }
            }
        )
    }

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

    protected open fun reconnect() {
        viewModel.loadData()
    }
}