package net.papirus.pyrusservicedesk.ui

import android.arch.lifecycle.Observer
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.View
import com.example.pyrusservicedesk.R
import kotlinx.android.synthetic.main.psd_activity_tickets.*
import kotlinx.android.synthetic.main.psd_no_connection.*
import net.papirus.pyrusservicedesk.ui.view.swiperefresh.DirectedSwipeRefresh
import net.papirus.pyrusservicedesk.ui.viewmodel.ConnectionViewModelBase
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
                        if (!it)
                            no_connection.visibility = View.VISIBLE
                        else
                            reconnect()
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

    private fun reconnect() {
        if (viewModel.getIsNetworkConnectedLiveDate().value == false)
            return
        no_connection.visibility = View.GONE
        viewModel.loadData()
    }
}