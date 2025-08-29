package com.pyrus.pyrusservicedesk.sdk.repositories.data_base

import com.pyrus.pyrusservicedesk.User
import com.pyrus.pyrusservicedesk._ref.data.RatingSettings
import com.pyrus.pyrusservicedesk._ref.data.RatingTextValues
import com.pyrus.pyrusservicedesk._ref.data.multy_chat.Application
import com.pyrus.pyrusservicedesk._ref.data.multy_chat.Member
import com.pyrus.pyrusservicedesk.core.Account
import com.pyrus.pyrusservicedesk.core.getInstanceId
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.cleanTags
import com.pyrus.pyrusservicedesk.sdk.data.ApplicationDto
import com.pyrus.pyrusservicedesk.sdk.data.AttachmentDto
import com.pyrus.pyrusservicedesk.sdk.data.AuthorDto
import com.pyrus.pyrusservicedesk.sdk.data.AuthorInfoDto
import com.pyrus.pyrusservicedesk.sdk.data.CommentDto
import com.pyrus.pyrusservicedesk.sdk.data.RatingSettingsDto
import com.pyrus.pyrusservicedesk.sdk.data.RatingTextValuesDto
import com.pyrus.pyrusservicedesk.sdk.data.TicketDto
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.ApplicationEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.AttachmentEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.AuthorEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.CommentEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.CommentInfo
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.MemberEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.TicketEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.UserEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.support.CommentWithAttachmentsEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.support.TicketWithComments

internal object DatabaseMapper {

    fun mapToApplication(entity: ApplicationEntity) = Application(
        appId = entity.appId,
        orgName = entity.orgName,
        orgLogoUrl = entity.orgLogoUrl,
        orgDescription = entity.orgDescription,
    )

    fun mapToMember(entity: MemberEntity) = Member(
        authorId = entity.authorId,
        name = entity.name,
        hasAccess = entity.hasAccess,
        phone = entity.phone
    )

    fun mapToTicketWithComments(dto: TicketDto, account: Account): TicketWithComments? {

        val comments = dto.comments?.map { mapToCommentWithAttachments(it, dto.ticketId) }
        val ticket = mapToTicketEntity(dto, account) ?: return null

        return TicketWithComments(
            ticket = ticket,
            comments = comments ?: emptyList(),
        )
    }

    fun mapToApplicationEntity(dto: ApplicationDto): ApplicationEntity? {
        return ApplicationEntity(
            appId = dto.appId ?: return null,
            orgName = dto.orgName,
            orgLogoUrl = dto.orgLogoUrl,
            orgDescription = dto.orgDescription,
        )
    }

    fun mapToMembersEntity(dto: AuthorInfoDto, userId: String): MemberEntity {
        return MemberEntity(
            id = "$userId${dto.authorId}",
            userId = userId,
            authorId = dto.authorId,
            name = dto.name,
            hasAccess = dto.hasAccess ?: false,
            phone = dto.phone
        )
    }

    fun mapToUserEntity(user: User) = UserEntity(
        userId = user.userId,
        appId = user.appId,
        userName = user.userName
    )

    fun mapToLastCommentInfo(entity: CommentWithAttachmentsEntity) = CommentInfo(
        commentId = entity.comment.commentId,
        creationDate = entity.comment.creationDate,
        author = entity.comment.author,
        body = entity.comment.body,
        lastAttachmentName = entity.attachments.maxByOrNull { it.id }?.name
    )

    private fun mapToCommentWithAttachments(
        dto: CommentDto,
        ticketId: Long,
    ): CommentWithAttachmentsEntity {
        val comment = mapToCommentEntity(dto, ticketId)
        val attachments = dto.attachments?.map { mapToAttachmentEntity(it, comment.commentId) }
        return CommentWithAttachmentsEntity(
            comment = comment,
            attachments = attachments ?: emptyList()
        )
    }

    private fun mapToLastCommentInfo(dto: CommentDto) = CommentInfo(
        commentId = dto.commentId,
        creationDate = dto.creationDate,
        author = dto.author?.let { mapToAuthorEntity(it) },
        body = dto.body,
        lastAttachmentName = dto.attachments?.maxByOrNull { it.id }?.name
    )

    private fun mapToTicketEntity(dto: TicketDto, account: Account): TicketEntity? {
        return TicketEntity(
            ticketId = dto.ticketId,
            userId = dto.userId ?: account.getInstanceId(),
            subject = dto.subject,
            unescapedSubject = dto.subject.cleanTags(" "),
            author = dto.author,
            isRead = dto.isRead,
            isActive = dto.isActive,
            createdAt = dto.createdAt,
            showRating = dto.showRating,
            showRatingText = dto.showRatingText,
            lastComment = dto.lastComment?.let { mapToLastCommentInfo(it) },
            ratingSettings = dto.ratingSettings?.let { mapToRatingSettings(it) },
        )
    }

    private fun mapToRatingSettings(dto: RatingSettingsDto) = RatingSettings(
        size = dto.size,
        type = dto.type,
        ratingTextValues = dto.ratingTextValues?.map { mapToRatingTextValues(it) }
    )

    private fun mapToRatingTextValues(dto: RatingTextValuesDto) = RatingTextValues(
        rating = dto.rating,
        text = dto.text
    )

    private fun mapToCommentEntity(dto: CommentDto, ticketId: Long) = CommentEntity(
        commentId = dto.commentId,
        ticketId = ticketId,
        body = dto.body,
        unescapedBody = dto.body?.cleanTags(" "),
        isInbound = dto.isInbound,
        creationDate = dto.creationDate,
        rating = dto.rating,
        author = dto.author?.let(::mapToAuthorEntity),
    )

    private fun mapToAuthorEntity(dto: AuthorDto) = AuthorEntity(
        name = dto.name,
        id = dto.authorId,
        avatarId = dto.avatarId,
        avatarColorString = dto.avatarColorString
    )

    private fun mapToAttachmentEntity(dto: AttachmentDto, commentId: Long) = AttachmentEntity(
        id = dto.id,
        commentId = commentId,
        guid = dto.guid,
        type = dto.type,
        name = dto.name,
        bytesSize = dto.bytesSize,
        isText = dto.isText,
        isVideo = dto.isVideo,
        localUri = dto.localUri?.toString(),
    )

}