package net.papirus.pyrusservicedesk.presentation.ui.navigation_page.tickets

import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.example.pyrusservicedesk.R
import net.papirus.pyrusservicedesk.presentation.ui.view.recyclerview.AdapterBase
import net.papirus.pyrusservicedesk.presentation.ui.view.recyclerview.ViewHolderBase
import net.papirus.pyrusservicedesk.sdk.data.TicketShortDescription
import net.papirus.pyrusservicedesk.utils.ConfigUtils
import net.papirus.pyrusservicedesk.utils.getTimePassedFrom
import java.util.*

internal class TicketsAdapter: AdapterBase<TicketShortDescription>() {

    private var ticketClickListener: ((ticket: TicketShortDescription) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderBase<TicketShortDescription> {
        return TicketHolder(parent)
    }

    fun setOnTicketClickListener(listener: (ticket: TicketShortDescription) -> Unit){
        ticketClickListener = listener
    }

    inner class TicketHolder(parent: ViewGroup)
        : ViewHolderBase<TicketShortDescription>(parent, R.layout.psd_view_holder_ticket) {

        private val ticketName = itemView.findViewById<TextView>(R.id.ticket_name)
        private val unreadCounter = itemView.findViewById<ImageView>(R.id.unread_counter)
        private val date = itemView.findViewById<TextView>(R.id.date)
        private val lastComment = itemView.findViewById<TextView>(R.id.last_comment)

        init {
            itemView.setOnClickListener{ ticketClickListener?.invoke(getItem()) }
            unreadCounter.setColorFilter(ConfigUtils.getAccentColor(itemView.context))
        }

        override fun bindItem(item: TicketShortDescription) {
            super.bindItem(item)
            ticketName.text = getItem().subject
            lastComment.text = getItem().lastComment?.body
            date.text = getItem().lastComment?.creationDate?.getTimePassedFrom(itemView.context, Calendar.getInstance())
            unreadCounter.visibility = if (!getItem().isRead) VISIBLE else GONE
        }
    }
}