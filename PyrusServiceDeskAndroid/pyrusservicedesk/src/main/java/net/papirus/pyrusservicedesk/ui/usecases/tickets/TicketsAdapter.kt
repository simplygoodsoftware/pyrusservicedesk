package net.papirus.pyrusservicedesk.ui.usecases.tickets

import android.view.View.GONE
import android.view.ViewGroup
import android.widget.TextView
import com.example.pyrusservicedesk.R
import net.papirus.pyrusservicedesk.repository.data.Ticket
import net.papirus.pyrusservicedesk.ui.view.recyclerview.AdapterBase
import net.papirus.pyrusservicedesk.ui.view.recyclerview.ViewHolderBase

internal class TicketsAdapter: AdapterBase<Ticket>() {

    private var ticketClickListener: ((ticket: Ticket) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderBase<Ticket> {
        return TicketHolder(parent)
    }

    fun setOnTicketClickListener(listener: (ticket: Ticket) -> Unit){
        ticketClickListener = listener
    }

    inner class TicketHolder(parent: ViewGroup)
        : ViewHolderBase<Ticket>(parent, R.layout.psd_view_holder_ticket) {

        private val ticketName = itemView.findViewById<TextView>(R.id.ticket_name)
        private val unreadCounter = itemView.findViewById<TextView>(R.id.unread_counter)
        private val date = itemView.findViewById<TextView>(R.id.date)
        private val lastComment = itemView.findViewById<TextView>(R.id.last_comment)

        init {
            itemView.setOnClickListener{ ticketClickListener?.invoke(getItem()) }
        }

        override fun bindItem(item: Ticket) {
            super.bindItem(item)
            ticketName.text = getItem().subject
            lastComment.text = getItem().description
            date.text = "1 w ago"
            unreadCounter.visibility = GONE
        }
    }
}