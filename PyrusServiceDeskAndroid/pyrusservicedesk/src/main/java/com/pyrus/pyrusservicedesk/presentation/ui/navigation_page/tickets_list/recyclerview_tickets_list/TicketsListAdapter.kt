package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.recyclerview_tickets_list

import android.annotation.SuppressLint
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries.TicketEntry
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.AdapterBase
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.ViewHolderBase
import com.pyrus.pyrusservicedesk.sdk.data.TicketShortDescription
import com.pyrus.pyrusservicedesk.utils.getTimePassedFrom
import java.util.Calendar

/**
 * Adapter that is used for rendering comment feed of the ticket screen.
 */
internal class TicketsListAdapter(itemsList: List<TicketShortDescription>): AdapterBase<TicketShortDescription>() {

    init {
        this.itemsList = itemsList.toMutableList()
    }
    private var onTicketItemClickListener: ((ticket: TicketShortDescription) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderBase<TicketShortDescription> {
        return TicketsListViewHolder(parent)
    }

    /**
     * Assigns [listener] ths is invoked when comment with the file that is
     * ready to be previewed was clicked.
     */
    fun setOnTicketItemClickListener(listener: (ticket: TicketShortDescription) -> Unit) {
        onTicketItemClickListener = listener
    }

    inner class TicketsListViewHolder(parent: ViewGroup)
        : ViewHolderBase<TicketShortDescription>(parent, R.layout.psd_tickets_list_item) {

        private val ticketName = itemView.findViewById<TextView>(R.id.ticket_name_tv)
        private val isUnread = itemView.findViewById<ImageView>(R.id.ticket_unread_iv)
        private val date = itemView.findViewById<TextView>(R.id.ticket_time_tv)
        private val lastComment = itemView.findViewById<TextView>(R.id.ticket_last_comment_tv)

        init {
            itemView.setOnClickListener{ onTicketItemClickListener?.invoke(getItem()) }
        }

        override fun bindItem(item: TicketShortDescription) {
            super.bindItem(item)
            ticketName.text = getItem().subject
            lastComment.text = getItem().lastComment?.body
            date.text = "20.09"
            isUnread.visibility = if (!getItem().isRead) VISIBLE else GONE
        }
    }
}