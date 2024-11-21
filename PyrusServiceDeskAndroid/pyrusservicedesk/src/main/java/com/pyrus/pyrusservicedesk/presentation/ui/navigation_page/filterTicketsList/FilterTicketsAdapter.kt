package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.filterTicketsList

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pyrus.pyrusservicedesk.R

class FilterTicketsAdapter (users: List<String>, userIds: List<String>, userId: String, val onItemClick: (Int) -> Unit) : RecyclerView.Adapter<FilterTicketsAdapter.AddTicketViewHolder>() {

    private var users: List<String> = emptyList()
    private var userIds: List<String> = emptyList()
    private var userId: String = ""
    init {
        this.users = users
        this.userIds = userIds
        this.userId = userId
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AddTicketViewHolder {
        return AddTicketViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.filter_tickets_item, parent, false))
    }

    override fun onBindViewHolder(holder: AddTicketViewHolder, position: Int) {
        val item = users[position]
        holder.userName.text = item
        holder.selectedUserIv.visibility = if(userIds[position] == userId) View.VISIBLE else View.GONE
    }

    override fun getItemCount(): Int {
        return users.size
    }

    inner class AddTicketViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val userName: TextView = itemView.findViewById(R.id.userNameTv)
        val selectedUserIv: ImageView = itemView.findViewById(R.id.selectedUserIv)

        init {
            itemView.setOnClickListener{
                onItemClick(bindingAdapterPosition)
            }
        }
    }
}