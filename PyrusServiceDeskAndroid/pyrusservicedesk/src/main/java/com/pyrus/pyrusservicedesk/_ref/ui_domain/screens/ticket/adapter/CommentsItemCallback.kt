package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter

import androidx.recyclerview.widget.DiffUtil
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.new_entries.CommentEntryV2

/**
 * Item callback implementation that compares two entry of ticket entries.
 * Used for updating the comment feed.
 */
internal class CommentsItemCallback : DiffUtil.ItemCallback<CommentEntryV2>() {

    override fun areItemsTheSame(oldItem: CommentEntryV2, newItem: CommentEntryV2) = when {
        oldItem is CommentEntryV2.Comment && newItem is CommentEntryV2.Comment -> newItem.entryId == oldItem.entryId
        oldItem is CommentEntryV2.Buttons && newItem is CommentEntryV2.Buttons -> newItem.id == oldItem.id
        oldItem is CommentEntryV2.Date && newItem is CommentEntryV2.Date -> newItem.date == oldItem.date
        oldItem is CommentEntryV2.Rating && newItem is CommentEntryV2.Rating -> true
        oldItem is CommentEntryV2.SimpleText && newItem is CommentEntryV2.SimpleText -> newItem.entryId == oldItem.entryId
        oldItem is CommentEntryV2.RatingSelector && newItem is CommentEntryV2.RatingSelector -> true
        else -> false
    }

    override fun areContentsTheSame(oldItem: CommentEntryV2, newItem: CommentEntryV2): Boolean {
        return newItem == oldItem
    }

}