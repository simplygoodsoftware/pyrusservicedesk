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
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.filterTicketsList.FilterTicketsFragment
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.filterTicketsList.FilterTicketsFragment.Companion
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.TicketActivity
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.recyclerview_tickets_list.TicketsListAdapter
import com.pyrus.pyrusservicedesk.sdk.data.Ticket
import com.pyrus.pyrusservicedesk.utils.CIRCLE_TRANSFORMATION
import com.pyrus.pyrusservicedesk.utils.RequestUtils.Companion.getOrganisationLogoUrl
import kotlinx.android.synthetic.main.psd_empty_tickets_list.createTicketTv
import kotlinx.android.synthetic.main.psd_tickets_list.delete_filter_iv
import kotlinx.android.synthetic.main.psd_tickets_list.fabAddTicket
import kotlinx.android.synthetic.main.psd_tickets_list.filter_fl
import kotlinx.android.synthetic.main.psd_tickets_list.psd_empty_tickets_list_ll
import kotlinx.android.synthetic.main.psd_tickets_list.tickets_rv
import kotlinx.android.synthetic.main.psd_tickets_list.view.filter_context_tv
import kotlinx.android.synthetic.main.psd_toolbar.psd_toolbar_filter_ib
import kotlinx.android.synthetic.main.psd_toolbar.psd_toolbar_qr_ib
import kotlinx.android.synthetic.main.psd_toolbar.psd_toolbar_settings_ib
import kotlinx.android.synthetic.main.psd_toolbar.psd_toolbar_vendor_name_tv

/**
 * Activity for rendering ticket/feed comments.
 */
internal class TicketListActivity : ConnectionActivityBase<TicketsListViewModel>(TicketsListViewModel::class.java), FilterTicketsFragment.CallbackForFilter {

    companion object {

        private const val KEY_DEFAULT_USER_ID = "0"
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

    private var selectedUserIdFilter: String = KEY_DEFAULT_USER_ID

    override val layoutResId = R.layout.psd_tickets_list
    override val toolbarViewId = R.id.toolbar_tickets_list
    override val refresherViewId = View.NO_ID
    override val progressBarViewId: Int = View.NO_ID

    private lateinit var adapter: TicketsListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val toolbarFilter = findViewById<ImageButton>(R.id.psd_toolbar_filter_ib)
        val toolbarQr = findViewById<ImageButton>(R.id.psd_toolbar_qr_ib)

        toolbarFilter.setOnClickListener {
            val bottomSheet = FilterTicketsFragment.newInstance(selectedUserIdFilter)
            bottomSheet.show(supportFragmentManager, bottomSheet.tag)
            Toast.makeText(applicationContext, "фильтры", Toast.LENGTH_SHORT).show()
        }
        delete_filter_iv.setOnClickListener { onDataSentBack(KEY_DEFAULT_USER_ID, "all") }
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

    private fun getSelectedUserIds(chosenUserId: String): List<Ticket> {
        val allUsersName = viewModel.getTicketsLiveData().value ?: emptyList()
        if (chosenUserId == KEY_DEFAULT_USER_ID)
            return allUsersName

        return allUsersName.filter { it.userId == chosenUserId }
    }

    override fun onDataSentBack(userId: String, userName: String) {
        adapter.setItems(getSelectedUserIds(userId))
        selectedUserIdFilter = userId
        val toolbarFilter = findViewById<ImageButton>(R.id.psd_toolbar_filter_ib)
        toolbarFilter.setBackgroundResource(if(userId == KEY_DEFAULT_USER_ID) R.drawable.ic_filter else R.drawable.ic_selected_filter)
        filter_fl.filter_context_tv.text = userName
        filter_fl.visibility = if(userId == KEY_DEFAULT_USER_ID) View.GONE else View.VISIBLE

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