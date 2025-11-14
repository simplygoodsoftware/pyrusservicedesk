package com.pyrus.pyrusservicedesk.sdk.repositories.data_base.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.SdDatabase.Companion.COMMANDS_TABLE
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.SdDatabase.Companion.COMMENTS_TABLE
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.SdDatabase.Companion.TICKETS_TABLE
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.CommandEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.CommentInfo
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.TicketEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.support.CommandWithAttachmentsEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.support.CommandWithHeader
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.support.CommentWithAttachmentsEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.support.TicketWithComment

@Dao
internal abstract class SearchDao {

    @Transaction
    open fun searchCommentsCommandsWithHeader(query: String, limit: Int): List<CommandWithHeader> {
        val commands = searchCommentsCommands(query, limit)

        val result = ArrayList<CommandWithHeader>()
        for (command in commands) {
            val ticketId = command.command.ticketId ?: continue
            if (ticketId > 0) {
                val ticket = getTicket(ticketId) ?: continue
                CommandWithHeader(ticket, command)
            }
            else {
                val commandHeader = getCommand(ticketId) ?: continue
                CommandWithHeader(commandHeader, command)
            }
        }
        return result
    }

    @Transaction
    open fun searchTicketsWithComment(query: String, limit: Int): List<TicketWithComment> {

        val result = ArrayList<TicketWithComment>()

        val tickets = searchTickets(query, limit)
        result += tickets.map { TicketWithComment(it, it.lastComment) }

        val comments = searchComments(query, limit)

        for (comment in comments) {
            val ticket = getTicket(comment.comment.ticketId)
            if (ticket != null) {
                val commentInfo = CommentInfo(
                    comment.comment.commentId,
                    comment.comment.creationDate,
                    comment.comment.author,
                    comment.comment.body,
                    comment.attachments.lastOrNull()?.name
                )
                result += TicketWithComment(ticket, commentInfo)
            }
        }

        return result
    }

    @Query("SELECT * FROM $COMMANDS_TABLE WHERE ticket_id = :ticketId")
    abstract fun getCommand(ticketId: Long): CommandEntity?

    @Transaction
    @Query("SELECT * FROM $COMMANDS_TABLE WHERE command_type=0 AND comment LIKE '%' || :query || '%' ORDER BY creation_time ASC LIMIT :limit" )
    abstract fun searchCommentsCommands(query: String, limit: Int): List<CommandWithAttachmentsEntity>

    @Query("SELECT * FROM $TICKETS_TABLE WHERE ticket_id LIKE :ticketId LIMIT 1")
    abstract fun getTicket(ticketId: Long): TicketEntity?

    @Transaction
    @Query("SELECT * FROM $COMMENTS_TABLE WHERE unescaped_body LIKE '%' || :query || '%' ORDER BY created_at ASC LIMIT :limit")
    abstract fun searchComments(query: String, limit: Int): List<CommentWithAttachmentsEntity>

    @Query("SELECT * FROM $TICKETS_TABLE WHERE unescaped_subject LIKE '%' || :query || '%' ORDER BY created_at ASC LIMIT :limit")
    abstract fun searchTickets(query: String, limit: Int): List<TicketEntity>

}