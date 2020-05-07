package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets

import androidx.lifecycle.Observer
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.presentation.ConnectionActivityBase
import com.pyrus.pyrusservicedesk.presentation.ui.navigation.UiNavigator
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.item_decorators.SpaceItemDecoration
import kotlinx.android.synthetic.main.psd_activity_tickets.*


/**
 * Activity for rendering list of current tickets
 */
internal class TicketsActivity: ConnectionActivityBase<TicketsViewModel>(TicketsViewModel::class.java) {

    companion object {
        /**
         * Provides intent for launching activity
         */
        fun getLaunchIntent(): Intent {
            return Intent(
                PyrusServiceDesk.get().application,
                TicketsActivity::class.java)
        }
    }

    override val layoutResId = R.layout.psd_activity_tickets
    override val toolbarViewId = R.id.tickets_toolbar
    override val refresherViewId = R.id.refresh
    override val progressBarViewId: Int = View.NO_ID

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
                                when{
                                    !it.isRead -> viewModel.getUnreadCount() - 1
                                    else -> viewModel.getUnreadCount()
                                })
                            viewModel.onTicketOpened(it)
                        }
                    }
                }
        tickets.adapter  = adapter
        tickets.addItemDecoration(SpaceItemDecoration(resources.getDimensionPixelSize(R.dimen.psd_tickets_item_space)))
        new_conversation.setOnClickListener {
            UiNavigator.toNewTicket(this, viewModel.getUnreadCount())
        }
    }

    override fun startObserveData() {
        super.startObserveData()
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

    override fun finish() {
        super.finish()
        PyrusServiceDesk.onServiceDeskStop()
    }

    private fun onMenuItemClicked(menuItem: MenuItem?): Boolean {
        return menuItem?.let {
            when (it.itemId) {
                R.id.psd_main_menu_close -> quitViewModel.quitServiceDesk()
            }
            true
        } ?: false
    }
}