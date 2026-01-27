package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.fingerprints

import android.graphics.drawable.AnimationDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketView
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.entries.CommentEntry
import com.pyrus.pyrusservicedesk._ref.utils.animateVisibility
import com.pyrus.pyrusservicedesk.databinding.PsdViewHolderCommentRatingBinding
import com.pyrus.pyrusservicedesk.payload_adapter.BaseViewHolder
import com.pyrus.pyrusservicedesk.payload_adapter.ItemFingerprint
import com.pyrus.pyrusservicedesk.payload_adapter.PayloadActionBuilder
import com.pyrus.pyrusservicedesk.payload_adapter.diff
import kotlin.reflect.KClass


internal class RatingCommentFingerprint(
    private val onEvent: (event: TicketView.Event) -> Unit,
) : ItemFingerprint<CommentEntry.Rating>() {

    override val layoutId: Int = R.layout.psd_view_holder_comment_rating

    override val entryKeyKClass: KClass<*> = CommentEntry.Rating::class

    override fun getViewHolder(
        layoutInflater: LayoutInflater,
        parent: ViewGroup
    ): BaseViewHolder<CommentEntry.Rating> = RatingCommentHolder(
        PsdViewHolderCommentRatingBinding.inflate(layoutInflater, parent, false),
        onEvent
    )

    override fun areItemsTheSame(
        oldItem: CommentEntry.Rating,
        newItem: CommentEntry.Rating
    ) = true

    override fun getChangePayload(
        oldItem: CommentEntry.Rating,
        newItem: CommentEntry.Rating,
    ): Any? {
        val payload = HashSet<String>()

        if (oldItem.creationTime != newItem.creationTime) payload.add(getPropertyName(CommentEntry.Rating::creationTime))
        if (oldItem.id != newItem.id) payload.add(getPropertyName(CommentEntry.Rating::id))
        if (oldItem.hasError != newItem.hasError) payload.add(getPropertyName(CommentEntry.Rating::hasError))
        if (oldItem.isLocal != newItem.isLocal) payload.add(getPropertyName(CommentEntry.Rating::isLocal))
        if (oldItem.rating != newItem.rating) payload.add(getPropertyName(CommentEntry.Rating::rating))
        if (oldItem.statusIconRes != newItem.statusIconRes) payload.add(getPropertyName(CommentEntry.Rating::statusIconRes))
        if (oldItem.statusIconIsVisible != newItem.statusIconIsVisible) payload.add(getPropertyName(CommentEntry.Rating::statusIconIsVisible))
        return payload
    }
}

internal class RatingCommentHolder(
    private val binding: PsdViewHolderCommentRatingBinding,
    private val onEvent: (event: TicketView.Event) -> Unit,
) : BaseViewHolder<CommentEntry.Rating>(binding.root) {

    var hasError: Boolean = false
    var currentId: Long? = null

    init {
        binding.root.setOnClickListener {
            val id = currentId
            if (hasError && id != null) {
                onEvent(TicketView.Event.OnErrorCommentClick(id))
            }
        }
    }

    override fun bind(builder: PayloadActionBuilder<CommentEntry.Rating>) = builder.diff {
        hasError = entry.hasError
        currentId = entry.id

        entry::rating.payloadCheck {
            binding.ratingImage.setImageResource(entry.rating.ratingToEmojiRes())
        }
        entry::statusIconRes.payloadCheck {
            binding.statusIcon.setImageResource(entry.statusIconRes)
        }
        entry::statusIconIsVisible.payloadCheck {
            if (firstItemBind) binding.statusIcon.isVisible = entry.statusIconIsVisible
            else binding.statusIcon.animateVisibility(entry.statusIconIsVisible)
        }
        entry::isLocal.payloadCheck {
            val animation = binding.statusIcon.drawable as? AnimationDrawable
            if (entry.isLocal) animation?.start()
            else animation?.stop()
        }
    }

    private fun Int?.ratingToEmojiRes(): Int = when (this) {
        1 -> R.drawable.ic_emoji_rating_1
        2 -> R.drawable.ic_emoji_rating_2
        3 -> R.drawable.ic_emoji_rating_3
        4 -> R.drawable.ic_emoji_rating_4
        5 -> R.drawable.ic_emoji_rating_5
        else -> R.drawable.ic_emoji_rating_3
    }

}