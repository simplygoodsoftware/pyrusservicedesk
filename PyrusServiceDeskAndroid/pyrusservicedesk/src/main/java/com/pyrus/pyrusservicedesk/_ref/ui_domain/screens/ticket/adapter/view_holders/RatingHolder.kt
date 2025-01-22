package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.view_holders

import android.view.ViewGroup
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketView
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.new_entries.CommentEntryV2
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk.databinding.PsdViewHolderRatingBinding
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.ViewHolderBase

internal class RatingHolder(
    parent: ViewGroup,
    private val onEvent: (event: TicketView.Event) -> Unit,
) : ViewHolderBase<CommentEntryV2.RatingSelector>(parent, R.layout.psd_view_holder_rating) {

    private val binding = PsdViewHolderRatingBinding.bind(itemView)

    init {
        ConfigUtils.getSecondaryColorOnMainBackground(itemView.context).apply {
            binding.rating1.setBackgroundColor(this)
            binding.rating2.setBackgroundColor(this)
            binding.rating3.setBackgroundColor(this)
            binding.rating4.setBackgroundColor(this)
            binding.rating5.setBackgroundColor(this)
        }
    }

    override fun bindItem(entry: CommentEntryV2.RatingSelector) {
        super.bindItem(entry)

        binding.rating1.setOnClickListener { onRatingClick(1) }
        binding.rating2.setOnClickListener { onRatingClick(2) }
        binding.rating3.setOnClickListener { onRatingClick(3) }
        binding.rating4.setOnClickListener { onRatingClick(4) }
        binding.rating5.setOnClickListener { onRatingClick(5) }
    }

    private fun onRatingClick(rating: Int) {
        onEvent(TicketView.Event.OnRatingClick(rating))
    }
}