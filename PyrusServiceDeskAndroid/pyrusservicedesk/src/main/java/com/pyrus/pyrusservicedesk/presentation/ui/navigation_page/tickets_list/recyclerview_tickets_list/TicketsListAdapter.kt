package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.recyclerview_tickets_list

import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.AdapterBase
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.ViewHolderBase
import com.pyrus.pyrusservicedesk.sdk.data.Ticket

/**
 * Adapter that is used for rendering comment feed of the ticket screen.
 */
internal class TicketsListAdapter: AdapterBase<Ticket>() {

    private var onTicketItemClickListener: ((ticket: Ticket) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderBase<Ticket> {
        return TicketsListViewHolder(parent)
    }

    /**
     * Assigns [listener] ths is invoked when comment with the file that is
     * ready to be previewed was clicked.
     */
    fun setOnTicketItemClickListener(listener: (ticket: Ticket) -> Unit) {
        onTicketItemClickListener = listener
    }

    inner class TicketsListViewHolder(parent: ViewGroup)
        : ViewHolderBase<Ticket>(parent, R.layout.psd_tickets_list_item) {

        private val ticketName = itemView.findViewById<TextView>(R.id.ticket_name_tv)
        private val isUnread = itemView.findViewById<ImageView>(R.id.ticket_unread_iv)
        private val date = itemView.findViewById<TextView>(R.id.ticket_time_tv)
        private val lastComment = itemView.findViewById<TextView>(R.id.ticket_last_comment_tv)

        init {
            itemView.setOnClickListener{ onTicketItemClickListener?.invoke(getItem()) }
        }

        override fun bindItem(item: Ticket) {
            super.bindItem(item)
            ticketName.text = getItem().subject
            lastComment.text = getItem().lastComment?.body
            date.text = "20.09"
            isUnread.visibility = if (!getItem().isRead!!) VISIBLE else GONE //TODO !!
        }
    }
}