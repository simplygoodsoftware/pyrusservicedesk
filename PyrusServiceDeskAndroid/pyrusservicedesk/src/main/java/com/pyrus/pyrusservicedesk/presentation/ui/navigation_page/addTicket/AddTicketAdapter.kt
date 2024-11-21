package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.addTicket

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.TicketActivity

class AddTicketAdapter (users: List<String>, val onItemClick: (Int) -> Unit) : RecyclerView.Adapter<AddTicketAdapter.AddTicketViewHolder>() {

    private var users: List<String> = emptyList()
    init {
        this.users = users
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AddTicketViewHolder {
        return AddTicketViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.add_ticket_item, parent, false))
    }

    override fun onBindViewHolder(holder: AddTicketViewHolder, position: Int) {
        val item = users[position]
        holder.userName.text = item
    }

    override fun getItemCount(): Int {
        return users.size
    }

    inner class AddTicketViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val userName: TextView = itemView.findViewById(R.id.userNameTv)

        init {
            itemView.setOnClickListener{
                onItemClick(bindingAdapterPosition)
            }
        }
    }
}