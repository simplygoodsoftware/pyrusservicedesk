package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.add_ticket

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pyrus.pyrusservicedesk.R

class AddTicketAdapter (users: List<String>, val onItemClick: (Int) -> Unit) : RecyclerView.Adapter<AddTicketAdapter.AddTicketViewHolder>() {

    private var users: List<String> = emptyList()
    init {
        this.users = users
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AddTicketViewHolder {
        return AddTicketViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.psd_add_ticket_item, parent, false))
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