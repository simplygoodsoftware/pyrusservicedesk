package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.adapters

import android.graphics.drawable.AnimationDrawable
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.Message
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsView.Model.TicketHeaderEntry
import com.pyrus.pyrusservicedesk._ref.utils.getTimeWhen
import com.pyrus.pyrusservicedesk._ref.utils.text
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.HtmlTagUtils
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.AdapterBase
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.ViewHolderBase
import java.util.Calendar

/**
 * Adapter that is used for rendering comment feed of the ticket screen.
 */
internal class TicketsListAdapter(
    private val onEvent: (Message.Outer) -> Unit
): AdapterBase<TicketHeaderEntry>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderBase<TicketHeaderEntry> {
        return TicketsListViewHolder(parent)
    }

    inner class TicketsListViewHolder(parent: ViewGroup)
        : ViewHolderBase<TicketHeaderEntry>(parent, R.layout.psd_tickets_list_item) {

        private val ticketName = itemView.findViewById<TextView>(R.id.ticket_name_tv)
        private val isUnread = itemView.findViewById<ImageView>(R.id.ticket_unread_iv)
        private val date = itemView.findViewById<TextView>(R.id.ticket_time_tv)
        private val lastComment = itemView.findViewById<TextView>(R.id.ticket_last_comment_tv)
        private val status = itemView.findViewById<ImageView>(R.id.ticket_status_iv)

        init {
            itemView.setOnClickListener {
                onEvent.invoke(Message.Outer.OnTicketClick(getItem().ticketId, getItem().userId))
            }
        }

        override fun bindItem(entry: TicketHeaderEntry) {
            super.bindItem(entry)
            // TODO форматированае и спаны
            ticketName.text = entry.title
            // TODO форматированае и спаны
            lastComment.text = entry.lastCommentText?.text(itemView.context)?.let(HtmlTagUtils::cleanTags)

            val lastCommentCreationTime = entry.lastCommentCreationTime
            val dateText = lastCommentCreationTime?.getTimeWhen(itemView.context, Calendar.getInstance())
            date.text = dateText
            isUnread.visibility = if (!entry.isRead) VISIBLE else GONE
            status.isVisible = entry.isLoading
            (status.drawable as AnimationDrawable).start()
        }
    }
}