package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.Message
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsView.Model.TicketSetInfoEntry
import com.pyrus.pyrusservicedesk.databinding.PsdTicketsListPageBinding

internal class TicketsPageAdapter(
    private val onEvent: (Message.Outer) -> Unit
): ListAdapter<TicketSetInfoEntry, TicketsPageAdapter.TicketsPageViewHolder>(ItemCallback()) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TicketsPageViewHolder {
        val binding = PsdTicketsListPageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TicketsPageViewHolder(binding, onEvent)
    }

    override fun onBindViewHolder(holder: TicketsPageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun getTitle(position: Int): String {
        return getItem(position).titleText
    }

    fun getAppId(position: Int): String {
        return getItem(position).appId
    }

    internal class TicketsPageViewHolder(
        private val binding: PsdTicketsListPageBinding,
        onEvent: (Message.Outer) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        private val adapter: TicketsListAdapter = TicketsListAdapter(onEvent)

        init {
            binding.ticketsRv.adapter = adapter
            binding.ticketsRv.layoutManager = LinearLayoutManager(itemView.context)
            binding.createTicketTv.setOnClickListener {
                onEvent(Message.Outer.OnCreateTicketClick)
            }
            binding.refresh.setOnRefreshListener {
                onEvent(Message.Outer.OnRefresh)
            }
        }

        fun bind(entry: TicketSetInfoEntry) {
            adapter.setItems(entry.tickets)
            binding.emptyTicketsListLl.isVisible = entry.tickets.isEmpty()
            binding.ticketsRv.isVisible = entry.tickets.isNotEmpty()
            binding.refresh.isRefreshing = entry.isLoading
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