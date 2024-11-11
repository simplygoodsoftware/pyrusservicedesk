package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.presentation.ConnectionActivityBase
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.TicketActivity
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.recyclerview_tickets_list.TicketsListAdapter
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.item_decorators.SpaceItemDecoration
import com.pyrus.pyrusservicedesk.sdk.data.Author
import com.pyrus.pyrusservicedesk.sdk.data.Comment
import com.pyrus.pyrusservicedesk.sdk.data.TicketShortDescription
import kotlinx.android.synthetic.main.psd_activity_ticket.comments
import kotlinx.android.synthetic.main.psd_activity_ticket.refresh
import kotlinx.android.synthetic.main.psd_tickets_list.tickets_rv
import kotlinx.android.synthetic.main.psd_toolbar.psd_toolbar_filter_ib
import kotlinx.android.synthetic.main.psd_toolbar.psd_toolbar_qr_ib
import java.util.Date

/**
 * Activity for rendering ticket/feed comments.
 */
internal class TicketListActivity : ConnectionActivityBase<TicketsListViewModel>(TicketsListViewModel::class.java) {

    companion object {
        private const val KEY_USER_ID = "KEY_USER_ID"
        private const val KEY_UNREAD_COUNT = "KEY_UNREAD_COUNT"

        private const val STATE_KEYBOARD_SHOWN = "STATE_KEYBOARD_SHOWN"

        private const val CHECK_IS_AT_BOTTOM_DELAY_MS = 50L

        /**
         * Provides intent for launching the screen.
         *
         * @param userid id of user e.g. restaurant id to be rendered.
         * When not, this should be omitted for the new ticket.
         * @param unreadCount current count of unread tickets.
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

//
//    private val attachFileSharedViewModel: AttachFileSharedViewModel by getViewModel(
//        AttachFileSharedViewModel::class.java)
//    private val commentActionsSharedViewModel: PendingCommentActionSharedViewModel by getViewModel(
//        PendingCommentActionSharedViewModel::class.java)
//

//    private val adapter = TicketsListAdapter().apply {
//        setOnTicketItemClickListener {
//            it.ticketId.let { ticketId ->
//                this@TicketListActivity.startActivity(TicketActivity.getLaunchIntent(
//                    ticketId,
//                    when{
//                        !it.isRead -> viewModel.getUnreadCount() - 1
//                        else -> viewModel.getUnreadCount()
//                    }
//                ))
//                viewModel.onTicketOpened(it)
//            }
//        }
//    }

    private lateinit var adapter: TicketsListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val toolbarFilter = findViewById<ImageButton>(R.id.psd_toolbar_filter_ib)
        val toolbarQr = findViewById<ImageButton>(R.id.psd_toolbar_qr_ib)
        val ticketsRv = findViewById<RecyclerView>(R.id.tickets_rv)

        //supportActionBar?.apply { title = getString(R.string.psd_tickets_activity_title) }
        toolbarFilter.setOnClickListener {
            //TODO
            Toast.makeText(applicationContext, "фильтры", Toast.LENGTH_SHORT).show()
        }
        toolbarQr.setOnClickListener {
            //TODO
            Toast.makeText(applicationContext, "QR", Toast.LENGTH_SHORT).show()
        }



        adapter = TicketsListAdapter(provideTickets())
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
        tickets_rv.adapter  = adapter
        tickets_rv.layoutManager = LinearLayoutManager(this)

        //TODO button New ticket, maybe addItemDecoration
        //tickets_rv.addItemDecoration(SpaceItemDecoration(resources.getDimensionPixelSize(R.dimen.psd_tickets_item_space)))
//        new_conversation.setOnClickListener {
//            UiNavigator.toNewTicket(this, viewModel.getUnreadCount())
//        }

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

    override fun finish() {
        super.finish()
        PyrusServiceDesk.onServiceDeskStop()
    }

}