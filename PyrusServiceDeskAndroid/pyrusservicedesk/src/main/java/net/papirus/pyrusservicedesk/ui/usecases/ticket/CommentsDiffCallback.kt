package net.papirus.pyrusservicedesk.ui.usecases.ticket

import android.support.v7.util.DiffUtil
import net.papirus.pyrusservicedesk.sdk.data.Comment
import net.papirus.pyrusservicedesk.ui.usecases.ticket.entries.CommentEntry
import net.papirus.pyrusservicedesk.ui.usecases.ticket.entries.DateEntry
import net.papirus.pyrusservicedesk.ui.usecases.ticket.entries.TicketEntry

internal class CommentsDiffCallback(
    val oldList: List<TicketEntry>,
    val newList: List<TicketEntry>)
    : DiffUtil.Callback() {

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldEntry = oldList[oldItemPosition]
        val newEntry = newList[newItemPosition]
        return when{
            oldEntry is DateEntry && newEntry is DateEntry -> oldEntry.date == newEntry.date
            oldEntry is CommentEntry && newEntry is CommentEntry -> oldEntry.comment.isSameWith(newEntry.comment)
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
                oldEntry.comment == newEntry.comment && oldEntry.updateError == newEntry.updateError
            }
            else -> false
        }
    }

    private fun Comment.isSameWith(another: Comment) = commentId == another.commentId && localId == another.localId
}