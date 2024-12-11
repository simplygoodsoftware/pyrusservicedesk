package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter

import androidx.recyclerview.widget.DiffUtil
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.new_entries.CommentEntryV2

/**
 * Diff callback implementation that compares two lists of ticket entries.
 * Used for updating the comment feed.
 */
internal class CommentsItemCallback : DiffUtil.ItemCallback<CommentEntryV2>() {

    override fun areItemsTheSame(oldItem: CommentEntryV2, newItem: CommentEntryV2) = when {
        oldItem is CommentEntryV2.Comment && newItem is CommentEntryV2.Comment -> newItem.id == oldItem.id
        oldItem is CommentEntryV2.Buttons && newItem is CommentEntryV2.Buttons -> newItem.id == oldItem.id
        oldItem is CommentEntryV2.Date && newItem is CommentEntryV2.Date -> newItem.date == oldItem.date
        oldItem is CommentEntryV2.Rating && newItem is CommentEntryV2.Rating -> true
        oldItem is CommentEntryV2.WelcomeMessage && newItem is CommentEntryV2.WelcomeMessage -> true
        oldItem is CommentEntryV2.SelectRating && newItem is CommentEntryV2.SelectRating -> true
        else -> false
    }

    override fun areContentsTheSame(oldItem: CommentEntryV2, newItem: CommentEntryV2) = when {
        oldItem is CommentEntryV2.Comment && newItem is CommentEntryV2.Comment -> newItem == oldItem
        oldItem is CommentEntryV2.Buttons && newItem is CommentEntryV2.Buttons -> newItem == oldItem
        oldItem is CommentEntryV2.Date && newItem is CommentEntryV2.Date -> newItem == oldItem
        oldItem is CommentEntryV2.Rating && newItem is CommentEntryV2.Rating -> newItem == oldItem
        oldItem is CommentEntryV2.WelcomeMessage && newItem is CommentEntryV2.WelcomeMessage -> newItem == oldItem
        oldItem is CommentEntryV2.SelectRating && newItem is CommentEntryV2.SelectRating -> true
        else -> false
    }

}