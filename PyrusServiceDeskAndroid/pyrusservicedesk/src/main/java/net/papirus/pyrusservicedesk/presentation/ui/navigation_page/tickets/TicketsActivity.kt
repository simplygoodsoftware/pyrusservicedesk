package net.papirus.pyrusservicedesk.presentation.ui.navigation_page.tickets

import android.arch.lifecycle.Observer
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
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
    override val refresherViewId = R.id.refresh

    private lateinit var adapter: TicketsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.apply { title = getString(R.string.psd_tickets_activity_title) }
        tickets_toolbar.setNavigationOnClickListener { finish() }
        tickets_toolbar.setOnMenuItemClickListener{ onMenuItemClicked(it) }
        adapter = TicketsAdapter()
                .apply {
                    setOnTicketClickListener {
                        it.ticketId.let { ticketId ->
                            UiNavigator.toTicket(
                                this@TicketsActivity,
                                ticketId,
                                if (!it.isRead) viewModel.getUnreadCount() - 1 else viewModel.getUnreadCount())
                            viewModel.onTicketOpened(it)
                        }
                    }
                }
                .also { tickets.adapter  = it }
        tickets.addItemDecoration(
                SpaceItemDecoration(resources.getInteger(R.integer.psd_tickets_item_space)))
        new_conversation.setOnClickListener { UiNavigator.toNewTicket(this, viewModel.getUnreadCount()) }
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
            Observer { list ->
                refresh.isRefreshing = false
                list?.let{ adapter.setItems(it) }
            }
        )
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return menu?.let{
            MenuInflater(this).inflate(R.menu.psd_main_menu, menu)
            menu.findItem(R.id.psd_main_menu_close).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            true
        } ?: false
    }

    private fun onMenuItemClicked(menuItem: MenuItem?): Boolean {
        return menuItem?.let {
            when (it.itemId) {
                R.id.psd_main_menu_close -> sharedViewModel.quitServiceDesk()
            }
            true
        } ?: false
    }
}