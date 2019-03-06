package net.papirus.pyrusservicedesk.ui

import android.arch.lifecycle.Observer
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.View
import com.example.pyrusservicedesk.R
import kotlinx.android.synthetic.main.psd_activity_tickets.*
import kotlinx.android.synthetic.main.psd_no_connection.*
import net.papirus.pyrusservicedesk.ui.viewmodel.ConnectionViewModelBase
import net.papirus.pyrusservicedesk.utils.getColor

internal abstract class ConnectionActivityBase<T: ConnectionViewModelBase>(viewModelClass: Class<T>)
    : ActivityBase() {

    protected val viewModel: T by getViewModel(viewModelClass)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        progress_bar?.progressDrawable?.setColorFilter(
                getColor(this, R.attr.colorAccentSecondary),
                PorterDuff.Mode.SRC)
        reconnect.setOnClickListener { reconnect() }
    }

    override fun observeData() {
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
    }

    private fun reconnect() {
        if (viewModel.getIsNetworkConnectedLiveDate().value == false)
            return
        no_connection.visibility = View.GONE
        viewModel.loadData()
    }
}