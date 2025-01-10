package com.pyrus.pyrusservicedesk.sdk.repositories

import android.net.Uri
import com.pyrus.pyrusservicedesk._ref.data.Attachment
import com.pyrus.pyrusservicedesk._ref.data.Author
import com.pyrus.pyrusservicedesk._ref.data.Comment
import com.pyrus.pyrusservicedesk._ref.data.FullTicket
import com.pyrus.pyrusservicedesk._ref.data.multy_chat.TicketSetInfo
import com.pyrus.pyrusservicedesk._ref.data.multy_chat.TicketsInfo
import com.pyrus.pyrusservicedesk._ref.utils.RequestUtils
import com.pyrus.pyrusservicedesk._ref.utils.RequestUtils.Companion.getAvatarUrl
import com.pyrus.pyrusservicedesk._ref.utils.isImage
import com.pyrus.pyrusservicedesk.core.Account
import com.pyrus.pyrusservicedesk.core.getUserId
import com.pyrus.pyrusservicedesk.presentation.ui.view.Status
import com.pyrus.pyrusservicedesk.sdk.data.AttachmentDto
import com.pyrus.pyrusservicedesk.sdk.data.AuthorDto
import com.pyrus.pyrusservicedesk.sdk.data.CommentDto
import com.pyrus.pyrusservicedesk.sdk.sync.TicketCommandResultDto
import com.pyrus.pyrusservicedesk.sdk.data.TicketDto
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.AddCommentResponseData
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.CommentsDto
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.TicketsDto

internal class RepositoryMapper(
    private val account: Account
) {

    fun map(ticketsDto: TicketsDto): TicketsInfo {
        val smallUsers = mapToSmallAcc(account) // TODO use account store

        val usersByAppId = smallUsers.groupBy { it.appId }

        val ticketsByUserId = ticketsDto.tickets
            ?.map(::map)
            ?.groupBy { it.userId } ?: error("tickets is null")

        val applications = ticketsDto.applications?.associateBy { it.appId } ?: error("applications is null")

        val ticketSetInfoList = usersByAppId.keys.map { appId ->
            val users = usersByAppId[appId] ?: emptyList()
            val tickets = ArrayList<FullTicket>()
            for (user in users) {
                val userTickets = ticketsByUserId[user.userId] ?: continue
                tickets += userTickets
            }
            val filteredTicketsData = tickets.toHashSet().sortedWith(TicketComparator())
            val resultTickets = filteredTicketsData.filter { it.isActive == true }.toMutableList()
            resultTickets.addAll(filteredTicketsData.filter { it.isActive == false })
            val application = applications[appId]
            val orgName = application?.orgName
            val orgLogoUrl = application?.orgLogoUrl
            TicketSetInfo(
                appId = appId,
                orgName = orgName ?: "",
                orgLogoUrl = orgLogoUrl,
                tickets = resultTickets,
            )
        }

        return TicketsInfo(ticketSetInfoList)
    }

    fun map(commentsDto: CommentsDto): FullTicket = FullTicket(
        comments = commentsDto.comments.map(::map),
        showRating = commentsDto.showRating,
        showRatingText = commentsDto.showRatingText,
        userId = commentsDto.showRatingText,
        ticketId = TODO(),
        subject = TODO(),
        isRead = TODO(),
        lastComment = TODO(),
        isActive = TODO(),
    )

    fun map(ticket: TicketDto): FullTicket {
        return FullTicket(
            comments = ticket.comments?.map(::map) ?: emptyList(),
            showRating = ticket.showRating ?: false,
            showRatingText = ticket.showRatingText,
            userId = ticket.userId!!, // TODO check it
            ticketId = ticket.ticketId,
            subject = ticket.subject,
            isRead = ticket.isRead ?: true,
            lastComment = ticket.lastComment?.let { map(it) },
            isActive = ticket.isActive,
        )
    }

    fun mapEmptyFullTicket(ticketId: Int, userId: String): FullTicket {
        return FullTicket(
            comments = emptyList(),
            showRating = false,
            showRatingText = null,
            userId = userId,
            ticketId = ticketId,
            subject = null,
            isRead = true,
            lastComment = null,
            isActive = true,
        )
    }

    fun map(commandResult: TicketCommandResultDto): AddCommentResponseData {
        return AddCommentResponseData(
            commentId = commandResult.commentId,
            attachmentIds = TODO(),
            sentAttachments = TODO()
        )
    }

    private fun map(commentDto: CommentDto): Comment = Comment(
        id = commentDto.commentId,
        isLocal = false,
        body = commentDto.body,
        isInbound = commentDto.isInbound && commentDto.author?.authorId == (account as? Account.V3)?.authorId,
        attachments = commentDto.attachments?.map(::map),
        creationTime = commentDto.creationDate.time,
        rating = commentDto.rating,
        author = commentDto.author?.let { map(it) },
        isSending = false,
    )


    fun map(attachmentDto: AttachmentDto): Attachment = Attachment(
        id = attachmentDto.id,
        name = attachmentDto.name,
        isImage = attachmentDto.name.isImage(),
        isText = attachmentDto.isText,
        bytesSize = attachmentDto.bytesSize,
        isVideo = attachmentDto.isVideo,
        uri =  Uri.parse(RequestUtils.getPreviewUrl(attachmentDto.id, account)),
        status = Status.Completed,
        progress = null,
        guid = attachmentDto.guid,
    )

    private fun map(authorDto: AuthorDto) = Author(
        name = authorDto.name,
        authorId = authorDto.authorId,
        avatarUrl = when (authorDto.avatarId) {
            null,
            0 -> null
            else -> getAvatarUrl(authorDto.avatarId, account.domain)
        },
        avatarColor = authorDto.avatarColorString,
    )

    private fun mapToSmallAcc(account: Account): List<SmallUser> = when(account) {
        is Account.V1 -> listOf(SmallUser(account.getUserId(), account.appId))
        is Account.V2 -> listOf(SmallUser(account.getUserId(), account.appId))
        is Account.V3 -> account.users.map { SmallUser(it.userId, it.appId) }
    }

    private data class SmallUser(
        val userId: String,
        val appId: String,
    )

}

private class TicketComparator : Comparator<FullTicket> {

    override fun compare(o1: FullTicket, o2: FullTicket): Int {
        return when {
            o1.lastComment == null -> return when {
                o2.lastComment == null -> o1.ticketId - o2.ticketId
                else -> 1
            }
            o2.lastComment == null -> -1
            o1.lastComment.creationTime < o2.lastComment.creationTime -> 1
            o1.lastComment.creationTime > o2.lastComment.creationTime -> -1
            else -> (o1.lastComment.id - o2.lastComment.id).toInt()
        }
    }
}