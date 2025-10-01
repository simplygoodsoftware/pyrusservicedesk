package com.pyrus.pyrusservicedesk.sdk.repositories.data_base.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.DatabaseMapper.mapToLastCommentInfo
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.SdDatabase.Companion.APPLICATIONS_TABLE
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.SdDatabase.Companion.COMMENTS_TABLE
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.SdDatabase.Companion.MEMBERS_TABLE
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.SdDatabase.Companion.TICKETS_TABLE
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.SdDatabase.Companion.USERS_TABLE
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.ApplicationEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.AttachmentEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.CommentEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.MemberEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.TicketEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.UserEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.support.ApplicationWithUsersEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.support.CommentWithAttachmentsEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.support.TicketWithComments
import kotlinx.coroutines.flow.Flow


@Dao
internal abstract class TicketsDao {

    @Transaction
    @Query("SELECT * FROM $TICKETS_TABLE WHERE ticket_id = :id")
    abstract fun getTicketWithComments(id: Long): TicketWithComments?

    @Transaction
    @Query("SELECT * FROM $COMMENTS_TABLE WHERE comment_id = :id")
    abstract fun getCommentWithAttachments(id: Long): CommentWithAttachmentsEntity?

    @Transaction
    @Query("SELECT * FROM $TICKETS_TABLE WHERE ticket_id = :id")
    abstract fun getTicketWithCommentsFlow(id: Long): Flow<TicketWithComments?>

    @Transaction
    @Query("SELECT * FROM $APPLICATIONS_TABLE")
    abstract fun getApplicationsFlow(): Flow<List<ApplicationWithUsersEntity>>

    @Transaction
    @Query("SELECT * FROM $APPLICATIONS_TABLE")
    abstract fun getApplicationsWithUsers(): List<ApplicationWithUsersEntity>

    @Transaction
    @Query("SELECT * FROM $TICKETS_TABLE")
    abstract fun getTickets(): List<TicketEntity>

    @Transaction
    @Query("SELECT * FROM $TICKETS_TABLE")
    abstract fun getTicketsWithComments(): List<TicketWithComments>

    @Query("SELECT * FROM $APPLICATIONS_TABLE")
    abstract fun getApplications(): List<ApplicationEntity>

    @Query("SELECT * FROM $USERS_TABLE")
    abstract fun getUsers(): List<UserEntity>

    @Query("SELECT * FROM $MEMBERS_TABLE WHERE user_id = :userId")
    abstract fun getMembers(userId: String): List<MemberEntity>


    @Transaction
    open fun insert(
        ticketWithComments: List<TicketWithComments>,
        applications: List<ApplicationEntity>,
        users: List<UserEntity>,
        members: List<MemberEntity>,
    ) {
        val applicationIds = applications.map { it.appId }

        val tickets = ticketWithComments.map { it.ticket }
        val ticketIds = tickets.map { it.ticketId }

        val commentWithAttachments = ticketWithComments.flatMap { it.comments }
        val comments = commentWithAttachments.map { it.comment }
        val commentsById = commentWithAttachments.associateBy { it.comment.commentId }
        val attachments = commentWithAttachments.flatMap { it.attachments }

        deleteApplicationsNotInIds(applicationIds)
        insertApplications(applications)

        insertUsers(users)
        insertMembers(members)

        deleteTicketsNotInIds(ticketIds)
        deleteCommentsNotInIds(ticketIds)
        for (ticket in tickets) {
            val lastComment = ticket.lastComment?.commentId?.let {
                commentsById[it] ?: getCommentWithAttachments(it)
            }
            if (lastComment == null) insertTicket(ticket)
            else insertTicket(ticket.copy(lastComment = mapToLastCommentInfo(lastComment)))
        }

        insertComments(comments)
        insertAttachments(attachments)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertApplications(applications: List<ApplicationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertUsers(users: List<UserEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertMembers(members: List<MemberEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertTicket(ticket: TicketEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertComments(tickets: List<CommentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertAttachments(tickets: List<AttachmentEntity>)

    @Query("DELETE FROM $TICKETS_TABLE WHERE ticket_id NOT IN (:ids)")
    abstract fun deleteTicketsNotInIds(ids: List<Long>)

    @Query("DELETE FROM $COMMENTS_TABLE WHERE ticket_id NOT IN (:ticketIds)")
    abstract fun deleteCommentsNotInIds(ticketIds: List<Long>)

    @Query("DELETE FROM $APPLICATIONS_TABLE WHERE app_id NOT IN (:ids)")
    abstract fun deleteApplicationsNotInIds(ids: List<String>)

}