package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.presentation.ConnectionActivityBase
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.addTicket.AddTicketFragment
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.TicketActivity
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.recyclerview_tickets_list.TicketsListAdapter
import com.pyrus.pyrusservicedesk.utils.CIRCLE_TRANSFORMATION
import com.pyrus.pyrusservicedesk.utils.RequestUtils.Companion.getOrganisationLogoUrl
import kotlinx.android.synthetic.main.psd_empty_tickets_list.createTicketTv
import kotlinx.android.synthetic.main.psd_tickets_list.fabAddTicket
import kotlinx.android.synthetic.main.psd_tickets_list.filter_fl
import kotlinx.android.synthetic.main.psd_tickets_list.psd_empty_tickets_list_ll
import kotlinx.android.synthetic.main.psd_tickets_list.tickets_rv
import kotlinx.android.synthetic.main.psd_toolbar.psd_toolbar_filter_ib
import kotlinx.android.synthetic.main.psd_toolbar.psd_toolbar_qr_ib
import kotlinx.android.synthetic.main.psd_toolbar.psd_toolbar_settings_ib
import kotlinx.android.synthetic.main.psd_toolbar.psd_toolbar_vendor_name_tv

/**
 * Activity for rendering ticket/feed comments.
 */
internal class TicketListActivity : ConnectionActivityBase<TicketsListViewModel>(TicketsListViewModel::class.java) {

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

    override val layoutResId = R.layout.psd_tickets_list
    override val toolbarViewId = R.id.toolbar_tickets_list
    override val refresherViewId = View.NO_ID
    override val progressBarViewId: Int = View.NO_ID

    private lateinit var adapter: TicketsListAdapter

    @SuppressLint("CommitTransaction")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val toolbarFilter = findViewById<ImageButton>(R.id.psd_toolbar_filter_ib)
        val toolbarQr = findViewById<ImageButton>(R.id.psd_toolbar_qr_ib)

        toolbarFilter.setOnClickListener {
            toolbarFilter.setBackgroundResource(if(filter_fl.visibility == View.VISIBLE) R.drawable.ic_filter else R.drawable.ic_selected_filter)
            filter_fl.visibility = if(filter_fl.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            Toast.makeText(applicationContext, "фильтры", Toast.LENGTH_SHORT).show()
        }
        toolbarQr.setOnClickListener {
            //TODO
            Toast.makeText(applicationContext, "QR", Toast.LENGTH_SHORT).show()
        }
        fabAddTicket.setOnClickListener {
            //TODO
            AddTicketFragment().show(supportFragmentManager, "")
            //this@TicketListActivity.startActivity(TicketActivity.getLaunchIntent())
        }
        createTicketTv.setOnClickListener {
            //TODO
            AddTicketFragment().show(supportFragmentManager, "")
            //this@TicketListActivity.startActivity(TicketActivity.getLaunchIntent())
        }

        adapter = TicketsListAdapter()
            .apply {
                setOnTicketItemClickListener {
                    it.ticketId.let { ticketId ->
                        this@TicketListActivity.startActivity(TicketActivity.getLaunchIntent(
                            ticketId = ticketId,
                            userId = viewModel.getCurrentUserId(ticketId)
                        )
                        )
                        viewModel.onTicketOpened(it)
                    }
                }
            }
        tickets_rv.adapter  = adapter
        tickets_rv.layoutManager = LinearLayoutManager(this)

        //TODO addItemDecoration maybe
        //tickets_rv.addItemDecoration(SpaceItemDecoration(resources.getDimensionPixelSize(R.dimen.psd_tickets_item_space)))
//        new_conversation.setOnClickListener {
//            UiNavigator.toNewTicket(this, viewModel.getUnreadCount())
//        }

    }

    override fun startObserveData() {
        super.startObserveData()
        viewModel.getTicketsLiveData().observe(
            this
        ) { list ->
            //TODO
            //refresh.isRefreshing = false
            val visibility = list.isNullOrEmpty()
            psd_toolbar_filter_ib.visibility = if (!visibility) View.VISIBLE else View.GONE
            psd_toolbar_qr_ib.visibility = if (!visibility) View.VISIBLE else View.GONE
            psd_toolbar_settings_ib.visibility = if (visibility) View.VISIBLE else View.GONE
            psd_empty_tickets_list_ll.visibility = if (visibility) View.VISIBLE else View.GONE
            fabAddTicket.visibility = if (!visibility) View.VISIBLE else View.GONE
            list?.let { adapter.setItems(it) }
        }

        viewModel.getApplicationsLiveData().observe(
            this
        ) { applications ->
            //TODO several vendors
            val imageView = findViewById<ImageView>(R.id.psd_toolbar_vendor_iv)

            applications[0].orgLogoUrl?.let {
                PyrusServiceDesk.get().picasso
                    .load(getOrganisationLogoUrl(it, PyrusServiceDesk.get().domain))
                    .transform(CIRCLE_TRANSFORMATION)
                    .into(imageView)
            }

            applications[0].orgName.let { psd_toolbar_vendor_name_tv.text = it }
        }
    }


    //TODO надо ли?
//    override fun finish() {
//        super.finish()
//        PyrusServiceDesk.onServiceDeskStop()
//    }

}