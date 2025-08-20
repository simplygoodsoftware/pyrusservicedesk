package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.fingerprints

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat.getColor
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketView
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.entries.CommentEntry
import com.pyrus.pyrusservicedesk.databinding.PsdViewHolderRatingBinding
import com.pyrus.pyrusservicedesk.payload_adapter.BaseViewHolder
import com.pyrus.pyrusservicedesk.payload_adapter.ItemFingerprint
import com.pyrus.pyrusservicedesk.payload_adapter.PayloadActionBuilder
import com.pyrus.pyrusservicedesk.payload_adapter.diff
import kotlin.reflect.KClass

internal class RatingFingerprint(
    private val onEvent: (event: TicketView.Event) -> Unit,
) : ItemFingerprint<CommentEntry.RatingSelector>() {

    override val layoutId: Int = R.layout.psd_view_holder_rating

    override val entryKeyKClass: KClass<*> = CommentEntry.RatingSelector::class

    override fun getViewHolder(
        layoutInflater: LayoutInflater,
        parent: ViewGroup
    ): BaseViewHolder<CommentEntry.RatingSelector> = RatingHolder(
        PsdViewHolderRatingBinding.inflate(layoutInflater, parent, false),
        onEvent
    )

    override fun areItemsTheSame(
        oldItem: CommentEntry.RatingSelector,
        newItem: CommentEntry.RatingSelector
    ) = true
}

internal class RatingHolder(
    private val binding: PsdViewHolderRatingBinding,
    private val onEvent: (event: TicketView.Event) -> Unit,
)  : BaseViewHolder<CommentEntry.RatingSelector>(binding.root) {

    init {
        getColor(itemView.context, R.color.psd_comment_inbound_bg).apply {
            binding.rating1.setBackgroundColor(this)
            binding.rating2.setBackgroundColor(this)
            binding.rating3.setBackgroundColor(this)
            binding.rating4.setBackgroundColor(this)
            binding.rating5.setBackgroundColor(this)
        }
    }

    override fun bind(builder: PayloadActionBuilder<CommentEntry.RatingSelector>) = builder.diff {

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