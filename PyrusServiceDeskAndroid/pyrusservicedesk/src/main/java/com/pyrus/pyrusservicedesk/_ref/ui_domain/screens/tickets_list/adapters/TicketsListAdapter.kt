package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.adapters

import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.data.FullTicket
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.Message
import com.pyrus.pyrusservicedesk._ref.utils.getTimeWhen
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.AdapterBase
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.ViewHolderBase
import java.util.Calendar

/**
 * Adapter that is used for rendering comment feed of the ticket screen.
 */
internal class TicketsListAdapter(
    private val onEvent: (Message.Outer) -> Unit
): AdapterBase<FullTicket>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderBase<FullTicket> {
        return TicketsListViewHolder(parent)
    }

    inner class TicketsListViewHolder(parent: ViewGroup)
        : ViewHolderBase<FullTicket>(parent, R.layout.psd_tickets_list_item) {

        private val ticketName = itemView.findViewById<TextView>(R.id.ticket_name_tv)
        private val isUnread = itemView.findViewById<ImageView>(R.id.ticket_unread_iv)
        private val date = itemView.findViewById<TextView>(R.id.ticket_time_tv)
        private val lastComment = itemView.findViewById<TextView>(R.id.ticket_last_comment_tv)

        init {
            itemView.setOnClickListener {
                onEvent.invoke(Message.Outer.OnTicketClick(getItem().ticketId, getItem().userId))
            }
        }

        override fun bindItem(entry: FullTicket) {
            super.bindItem(entry)
            // TODO форматированае и спаны
            ticketName.text = entry.subject
            // TODO форматированае и спаны
            lastComment.text = entry.lastComment?.body

            val lastCommentCreationTime = entry.lastComment?.creationTime
            val dateText = lastCommentCreationTime?.getTimeWhen(itemView.context, Calendar.getInstance()) //lastCommentCreationTime?.let { TextProvider.Date(it, R.string.psd_time_format) }
            date.text = dateText//.text(date.context) //TODO
            isUnread.visibility = if (!entry.isRead) VISIBLE else GONE
        }
    }
}