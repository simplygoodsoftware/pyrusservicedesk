package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.databinding.PsdTicketsListBinding
import com.pyrus.pyrusservicedesk.presentation.ConnectionActivityBase
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.TicketActivity
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.recyclerview_tickets_list.TicketsListAdapter
import kotlinx.android.synthetic.main.psd_toolbar.view.psd_toolbar_filter_ib
import kotlinx.android.synthetic.main.psd_toolbar.view.psd_toolbar_qr_ib

/**
 * Activity for rendering ticket/feed comments.
 */
internal class TicketListActivity : ConnectionActivityBase<TicketsListViewModel>(TicketsListViewModel::class.java) {

    override val layoutResId = R.layout.psd_tickets_list
    override val toolbarViewId = R.id.toolbar_tickets_list
    override val refresherViewId = View.NO_ID
    override val progressBarViewId: Int = View.NO_ID


    private lateinit var adapter: TicketsListAdapter
    private lateinit var binding: PsdTicketsListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = PsdTicketsListBinding.inflate(layoutInflater)

        val toolbarFilter = findViewById<ImageButton>(R.id.psd_toolbar_filter_ib)
        val toolbarQr = findViewById<ImageButton>(R.id.psd_toolbar_qr_ib)
        val ticketsRv = findViewById<RecyclerView>(R.id.tickets_rv)

        binding.toolbarTicketsList.psd_toolbar_filter_ib.setOnClickListener {
            binding.toolbarTicketsList.psd_toolbar_filter_ib.setBackgroundResource(if(binding.filterFl.visibility == View.VISIBLE) R.drawable.ic_filter else R.drawable.ic_selected_filter)
            binding.filterFl.visibility = if(binding.filterFl.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            Toast.makeText(applicationContext, "фильтры", Toast.LENGTH_SHORT).show()
        }
        binding.toolbarTicketsList.psd_toolbar_qr_ib.setOnClickListener {
            //TODO
            Toast.makeText(applicationContext, "QR", Toast.LENGTH_SHORT).show()
        }

        adapter = TicketsListAdapter(emptyList())
            .apply {
                setOnTicketItemClickListener {
                    it.ticketId.let { ticketId ->
                        this@TicketListActivity.startActivity(TicketActivity.getLaunchIntent(
                            ticketId,
                            when {
                                !it.isRead -> viewModel.getUnreadCount() - 1
                                else -> viewModel.getUnreadCount()
                            }
                        )
                        )
                        viewModel.onTicketOpened(it)
                    }
                }
            }
        binding.ticketsRv.adapter  = adapter
        binding.ticketsRv.layoutManager = LinearLayoutManager(this)

        //TODO button New ticket, maybe addItemDecoration
        //tickets_rv.addItemDecoration(SpaceItemDecoration(resources.getDimensionPixelSize(R.dimen.psd_tickets_item_space)))

    }

    override fun startObserveData() {
        super.startObserveData()
        //TODO
//        viewModel.getTicketsLiveData().observe(
//            this,
//            Observer { list ->
//                refresh.isRefreshing = false
//                list?.let{ adapter.setItems(it) }
//            }
//        )
//        val list = provideTickets()
//        list.let{ adapter.setItems(it) }
    }

    companion object {

        /**
         * Provides intent for launching the screen.
         */
        fun getLaunchIntent(): Intent {
            return Intent(
                PyrusServiceDesk.get().application,
                TicketListActivity::class.java
            )
        }

    }

}