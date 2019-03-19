package net.papirus.pyrusservicedesk.presentation.ui.navigation_page.tickets

import android.arch.lifecycle.Observer
import android.content.Intent
import android.os.Bundle
import com.example.pyrusservicedesk.R
import kotlinx.android.synthetic.main.psd_activity_tickets.*
import net.papirus.pyrusservicedesk.PyrusServiceDesk
import net.papirus.pyrusservicedesk.presentation.ConnectionActivityBase
import net.papirus.pyrusservicedesk.presentation.ui.navigation.UiNavigator
import net.papirus.pyrusservicedesk.presentation.ui.view.recyclerview.item_decorators.SpaceItemDecoration


internal class TicketsActivity: ConnectionActivityBase<TicketsViewModel>(TicketsViewModel::class.java) {

    companion object {
        fun getLaunchIntent(): Intent {
            return Intent(
                PyrusServiceDesk.getInstance().application,
                TicketsActivity::class.java)
        }
    }

    override val layoutResId = R.layout.psd_activity_tickets
    override val toolbarViewId = R.id.tickets_toolbar

    private lateinit var adapter: TicketsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.apply { title = getString(R.string.psd_tickets_activity_title) }
        tickets_toolbar.setNavigationIcon(R.drawable.psd_arrow_back)
        tickets_toolbar.setNavigationOnClickListener { finish() }
        adapter = TicketsAdapter()
                .apply {
                    setOnTicketClickListener {
                        it.ticketId.let {
                            UiNavigator.toTicket(this@TicketsActivity, it)
                        }
                    }
                }
                .also { tickets.adapter  = it }
        tickets.addItemDecoration(
                SpaceItemDecoration(resources.getInteger(R.integer.psd_tickets_item_space)))
        new_conversation.setOnClickListener { UiNavigator.toNewTicket(this) }
    }

    override fun observeData() {
        super.observeData()
        viewModel.getIsLoadingLiveData().observe(
            this,
            Observer { isLoading ->
                isLoading?.let {  }
            }
        )
        viewModel.getTicketsLiveData().observe(
            this,
            Observer { list -> list?.let{ adapter.setItems(it) } }
        )
    }
}