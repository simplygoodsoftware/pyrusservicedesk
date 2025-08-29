package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.fingerprints

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketView
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.entries.CommentEntry
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk.databinding.PsdViewHolderRatingItemBinding
import com.pyrus.pyrusservicedesk.payload_adapter.BaseViewHolder
import com.pyrus.pyrusservicedesk.payload_adapter.ItemFingerprint
import com.pyrus.pyrusservicedesk.payload_adapter.PayloadActionBuilder
import com.pyrus.pyrusservicedesk.payload_adapter.diff
import kotlin.reflect.KClass

internal class RatingTextFingerprint(
    private val onEvent: (event: TicketView.Event) -> Unit,
) : ItemFingerprint<CommentEntry.RatingTextValues>() {

    override val layoutId: Int = R.layout.psd_view_holder_rating_item

    override val entryKeyKClass: KClass<*> = CommentEntry.RatingTextValues::class

    override fun getViewHolder(
        layoutInflater: LayoutInflater,
        parent: ViewGroup
    ): BaseViewHolder<CommentEntry.RatingTextValues> = RatingTextHolder(
        PsdViewHolderRatingItemBinding.inflate(layoutInflater, parent, false),
        onEvent
    )

    override fun areItemsTheSame(
        oldItem: CommentEntry.RatingTextValues,
        newItem: CommentEntry.RatingTextValues
    ) = true
}

internal class RatingTextHolder(
    private val binding: PsdViewHolderRatingItemBinding,
    private val onEvent: (event: TicketView.Event) -> Unit,
)  : BaseViewHolder<CommentEntry.RatingTextValues>(binding.root) {
    var rating: Int? = null

    init {
        binding.root.setOnClickListener { rating?.let { rating -> onRatingClick(rating) } }
        binding.ratingText.backgroundTintList = ColorStateList.valueOf(ConfigUtils.getSupportMessageTextBackgroundColor(binding.root.context))
    }

    override fun bind(builder: PayloadActionBuilder<CommentEntry.RatingTextValues>) = builder.diff {
        rating = entry.rating

        entry::text.payloadCheck {
            binding.ratingText.text = entry.text
        }
    }

    private fun onRatingClick(rating: Int) {
        onEvent(TicketView.Event.OnRatingClick(rating, null))
    }
}