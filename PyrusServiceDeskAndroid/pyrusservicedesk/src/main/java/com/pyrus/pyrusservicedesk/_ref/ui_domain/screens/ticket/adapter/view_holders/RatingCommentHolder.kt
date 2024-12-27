package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.view_holders

import android.graphics.drawable.AnimationDrawable
import android.view.View
import android.view.ViewGroup
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.new_entries.CommentEntryV2
import com.pyrus.pyrusservicedesk.databinding.PsdViewHolderCommentRatingBinding
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.ViewHolderBase

internal class RatingCommentHolder(
    parent: ViewGroup,
    private val onErrorCommentEntryClickListener: (id: Long) -> Unit,
) : ViewHolderBase<CommentEntryV2.Rating>(parent, R.layout.psd_view_holder_comment_rating) {

    private val binding = PsdViewHolderCommentRatingBinding.bind(itemView)

    override fun bindItem(entry: CommentEntryV2.Rating) {
        super.bindItem(entry)
        with(binding) {
            ratingImage.setImageResource(entry.rating.ratingToEmojiRes())
            when {
                entry.hasError -> {
                    statusIcon.setImageResource(R.drawable.psd_error)
                    statusIcon.visibility = View.VISIBLE
                }
                entry.isLocal -> {
                    statusIcon.setImageResource(R.drawable.psd_sync_clock)
                    statusIcon.visibility = View.VISIBLE
                    (statusIcon.drawable as AnimationDrawable).start()
                }
                else -> {
                    statusIcon.visibility = View.GONE
                }
            }
            root.setOnClickListener {
                if (entry.hasError)
                    onErrorCommentEntryClickListener.invoke(entry.id)
            }
        }
    }

    private fun Int?.ratingToEmojiRes(): Int {
        return when (this) {
            1 -> R.drawable.ic_emoji_rating_1
            2 -> R.drawable.ic_emoji_rating_2
            3 -> R.drawable.ic_emoji_rating_3
            4 -> R.drawable.ic_emoji_rating_4
            5 -> R.drawable.ic_emoji_rating_5
            else -> R.drawable.ic_emoji_rating_3
        }
    }

}