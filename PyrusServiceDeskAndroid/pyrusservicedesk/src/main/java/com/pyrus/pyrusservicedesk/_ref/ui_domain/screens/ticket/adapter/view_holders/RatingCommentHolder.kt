package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.view_holders

import android.graphics.drawable.AnimationDrawable
import android.view.View
import android.view.ViewGroup
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.databinding.PsdViewHolderCommentRatingBinding
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries.CommentEntry
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries.hasError
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.ViewHolderBase

internal class RatingCommentHolder(
    parent: ViewGroup,
    private val onErrorCommentEntryClickListener: (entry: CommentEntry) -> Unit,
) : ViewHolderBase<CommentEntry>(parent, R.layout.psd_view_holder_comment_rating) {

    private val binding = PsdViewHolderCommentRatingBinding.bind(itemView)

    override fun bindItem(item: CommentEntry) {
        super.bindItem(item)
        with(binding) {
            ratingImage.setImageResource(item.comment.rating?.ratingToEmojiRes() ?: R.drawable.ic_emoji_rating_3)
            when {
                getItem().hasError() -> {
                    statusIcon.setImageResource(R.drawable.psd_error)
                    statusIcon.visibility = View.VISIBLE
                }
                getItem().comment.isLocal() -> {
                    statusIcon.setImageResource(R.drawable.psd_sync_clock)
                    statusIcon.visibility = View.VISIBLE
                    (statusIcon.drawable as AnimationDrawable).start()
                }
                else -> {
                    statusIcon.visibility = View.GONE
                }
            }
            root.setOnClickListener {
                if (getItem().hasError())
                    onErrorCommentEntryClickListener.invoke(getItem())
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