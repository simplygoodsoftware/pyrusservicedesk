package com.pyrus.pyrusservicedesk.sdk.repositories

import android.net.Uri
import com.pyrus.pyrusservicedesk._ref.data.Attachment
import com.pyrus.pyrusservicedesk._ref.data.Author
import com.pyrus.pyrusservicedesk._ref.data.Comment
import com.pyrus.pyrusservicedesk._ref.data.FullTicket
import com.pyrus.pyrusservicedesk._ref.utils.RequestUtils
import com.pyrus.pyrusservicedesk._ref.utils.RequestUtils.Companion.getAvatarUrl
import com.pyrus.pyrusservicedesk._ref.utils.isImage
import com.pyrus.pyrusservicedesk.core.Account
import com.pyrus.pyrusservicedesk.presentation.ui.view.Status
import com.pyrus.pyrusservicedesk.sdk.data.AttachmentDto
import com.pyrus.pyrusservicedesk.sdk.data.AuthorDto
import com.pyrus.pyrusservicedesk.sdk.data.CommentDto
import com.pyrus.pyrusservicedesk.sdk.data.TicketCommandResultDto
import com.pyrus.pyrusservicedesk.sdk.data.TicketDto
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.AddCommentResponseData
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.CommentsDto

internal class RepositoryMapper(
    private val account: Account
) {

    fun map(commentsDto: CommentsDto): FullTicket = FullTicket(
        comments = commentsDto.comments.map(::map),
        showRating = commentsDto.showRating,
        showRatingText = commentsDto.showRatingText,
        userId = commentsDto.showRatingText,
        ticketId = TODO(),
        subject = TODO(),
        isRead = TODO(),
        lastComment = TODO(),
    )

    fun map(ticket: TicketDto): FullTicket {
        return FullTicket(
            comments = ticket.comments?.map(::map) ?: emptyList(),
            showRating = ticket.showRating ?: false,
            showRatingText = ticket.showRatingText,
            userId = ticket.userId,
            ticketId = ticket.ticketId,
            subject = ticket.subject,
            isRead = ticket.isRead ?: true,
            lastComment = ticket.lastComment?.let { map(it) },
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
        isInbound = commentDto.isInbound,
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

}