package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.view_holders

import android.view.ViewGroup
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.databinding.PsdViewHolderRatingBinding
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries.RatingEntry
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.ViewHolderBase
import com.pyrus.pyrusservicedesk.utils.ConfigUtils

internal class RatingHolder(
    parent: ViewGroup,
    private val onRatingClickListener: (Int) -> Unit,
) : ViewHolderBase<RatingEntry>(parent, R.layout.psd_view_holder_rating) {

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

    override fun bindItem(item: RatingEntry) {
        super.bindItem(item)

        binding.rating1.setOnClickListener { onRatingClickListener.invoke(1) }
        binding.rating2.setOnClickListener { onRatingClickListener.invoke(2) }
        binding.rating3.setOnClickListener { onRatingClickListener.invoke(3) }
        binding.rating4.setOnClickListener { onRatingClickListener.invoke(4) }
        binding.rating5.setOnClickListener { onRatingClickListener.invoke(5) }
    }
}