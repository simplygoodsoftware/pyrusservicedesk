package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.presentation.ConnectionActivityBase
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.TicketActivity
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.recyclerview_tickets_list.TicketsListAdapter
import com.pyrus.pyrusservicedesk.sdk.data.Author
import com.pyrus.pyrusservicedesk.sdk.data.Comment
import com.pyrus.pyrusservicedesk.sdk.data.TicketShortDescription
import kotlinx.android.synthetic.main.psd_empty_tickets_list.createTicketTv
import kotlinx.android.synthetic.main.psd_tickets_list.fabAddTicket
import kotlinx.android.synthetic.main.psd_tickets_list.filter_fl
import kotlinx.android.synthetic.main.psd_tickets_list.tickets_rv
import java.util.Date

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val toolbarFilter = findViewById<ImageButton>(R.id.psd_toolbar_filter_ib)
        val toolbarQr = findViewById<ImageButton>(R.id.psd_toolbar_qr_ib)

        //supportActionBar?.apply { title = getString(R.string.psd_tickets_activity_title) }
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
            this@TicketListActivity.startActivity(TicketActivity.getLaunchIntent())
        }
        createTicketTv.setOnClickListener {
            //TODO
            this@TicketListActivity.startActivity(TicketActivity.getLaunchIntent())
        }

        adapter = TicketsListAdapter()
            .apply {
                setOnTicketItemClickListener {
                    it.ticketId.let { ticketId ->
                        this@TicketListActivity.startActivity(TicketActivity.getLaunchIntent(
                            ticketId
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
            list?.let { adapter.setItems(it) }
        }
    }

    //TODO delete
    private fun provideTickets(): List<TicketShortDescription> {
        val tasks = listOf(
            TicketShortDescription(0, "Ошибка в счете", false,  Comment(
                0, "iiko: Мы рады, что смогли Вам помочь решить проблему ☺", creationDate = Date(1731074815), author = Author("Autor"))),
            TicketShortDescription(0, "Проблемы с авторизацией в учетной зписи long", true,  Comment(
                0, "Вы: После обновления страницы ничего не происходит. Как перевести в режим прос", creationDate = Date(1730815615000), author = Author("Autor"))),
            TicketShortDescription(0, "Ошибка в счете", true,  Comment(
                0, "печатает", creationDate = Date(1731074815), author = Author("Autor"))),
            TicketShortDescription(0, "Ошибка в счете", true,  Comment(
                0, "iiko: Мы рады, что смогли Вам помочь решить проблему ☺", creationDate = Date(1728137215000), author = Author("Autor"))),
        )
        return tasks
    }


    //TODO надо ли?
//    override fun finish() {
//        super.finish()
//        PyrusServiceDesk.onServiceDeskStop()
//    }

}