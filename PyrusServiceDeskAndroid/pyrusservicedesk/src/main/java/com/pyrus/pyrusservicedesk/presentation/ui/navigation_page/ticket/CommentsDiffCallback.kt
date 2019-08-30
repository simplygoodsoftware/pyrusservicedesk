package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket

import androidx.recyclerview.widget.DiffUtil
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries.CommentEntry
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries.DateEntry
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries.TicketEntry
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries.WelcomeMessageEntry
import com.pyrus.pyrusservicedesk.sdk.data.Comment

/**
 * Diff callback implementation that compares two lists of ticket entries.
 * Used for updating the comment feed.
 */
internal class CommentsDiffCallback(
    private val oldList: List<TicketEntry>,
    private val newList: List<TicketEntry>)
    : DiffUtil.Callback() {

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldEntry = oldList[oldItemPosition]
        val newEntry = newList[newItemPosition]
        return when{
            oldEntry is DateEntry && newEntry is DateEntry -> true
            oldEntry is CommentEntry && newEntry is CommentEntry -> oldEntry.comment.isSameWith(newEntry.comment)
            oldEntry is WelcomeMessageEntry && newEntry is WelcomeMessageEntry -> true
            else -> false
        }
    }

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldEntry = oldList[oldItemPosition]
        val newEntry = newList[newItemPosition]
        return when {
            oldEntry is DateEntry && newEntry is DateEntry -> oldEntry.date == newEntry.date
            oldEntry is CommentEntry && newEntry is CommentEntry -> {
                oldEntry.comment == newEntry.comment && oldEntry.error == newEntry.error
            }
            oldEntry is WelcomeMessageEntry && newEntry is WelcomeMessageEntry -> oldEntry.message == newEntry.message
            else -> false
        }
    }

    private fun Comment.isSameWith(another: Comment) = commentId == another.commentId && localId == another.localId
}