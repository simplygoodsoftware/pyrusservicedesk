package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsView.Model.TicketSetInfoEntry
import com.pyrus.pyrusservicedesk.databinding.TicketsListFragmentBinding

internal class TicketSetAdapter(
    private val onEvent: (TicketsContract.Message.Outer) -> Unit
): ListAdapter<TicketSetInfoEntry, TicketSetAdapter.TicketSetViewHolder>(ItemCallback()) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TicketSetViewHolder {
        val binding = TicketsListFragmentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TicketSetViewHolder(binding, onEvent)
    }

    override fun onBindViewHolder(holder: TicketSetViewHolder, position: Int) {
        holder.bind(getItem(position))
    }


    internal class TicketSetViewHolder(
        private val binding: TicketsListFragmentBinding,
        private val onEvent: (TicketsContract.Message.Outer) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val adapter: TicketsListAdapter = TicketsListAdapter {
            onEvent(TicketsContract.Message.Outer.OnTicketClick(it.ticketId))
        }

        init {
            binding.ticketsRv.adapter = adapter
            binding.ticketsRv.layoutManager = LinearLayoutManager(itemView.context)
        }

        fun bind(entry: TicketSetInfoEntry) {
            adapter.setItems(entry.tickets)
        }

    }

    private class ItemCallback: DiffUtil.ItemCallback<TicketSetInfoEntry>() {

        override fun areItemsTheSame(oldItem: TicketSetInfoEntry, newItem: TicketSetInfoEntry): Boolean {
            return oldItem.appId == newItem.appId
        }

        override fun areContentsTheSame(oldItem: TicketSetInfoEntry, newItem: TicketSetInfoEntry): Boolean {
            return oldItem == newItem
        }

    }



}