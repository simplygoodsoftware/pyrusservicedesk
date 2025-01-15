package com.pyrus.pyrusservicedesk.sdk.repositories

import android.net.Uri
import com.pyrus.pyrusservicedesk.User
import com.pyrus.pyrusservicedesk._ref.data.Attachment
import com.pyrus.pyrusservicedesk._ref.data.Author
import com.pyrus.pyrusservicedesk._ref.data.Comment
import com.pyrus.pyrusservicedesk._ref.data.FullTicket
import com.pyrus.pyrusservicedesk._ref.data.multy_chat.TicketSetInfo
import com.pyrus.pyrusservicedesk._ref.utils.RequestUtils
import com.pyrus.pyrusservicedesk._ref.utils.RequestUtils.Companion.getAvatarUrl
import com.pyrus.pyrusservicedesk._ref.utils.isImage
import com.pyrus.pyrusservicedesk.core.Account
import com.pyrus.pyrusservicedesk.core.getUserId
import com.pyrus.pyrusservicedesk.presentation.ui.view.Status
import com.pyrus.pyrusservicedesk.sdk.data.AttachmentDto
import com.pyrus.pyrusservicedesk.sdk.data.AuthorDto
import com.pyrus.pyrusservicedesk.sdk.data.CommentDto
import com.pyrus.pyrusservicedesk.sdk.data.TicketDto
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.TicketsDto
import com.pyrus.pyrusservicedesk.sdk.sync.SyncRequest
import com.pyrus.pyrusservicedesk.sdk.sync.TicketCommandType.*
import java.util.concurrent.ConcurrentHashMap

internal class RepositoryMapper(
    private val account: Account
) {

    private val ticketIdMap = ConcurrentHashMap<Long, Long>()

    fun mergeTickets(ticketsDto: TicketsDto?, commands: List<CommandEntity>): List<TicketSetInfo> {

        val tickets = ArrayList<FullTicket>()

        val commandsById = commands.groupBy { it.ticketId }
        tickets += ticketsDto?.tickets?.mapNotNull { ticketDto ->
            val ticketCommands = commandsById[ticketDto.ticketId] ?: emptyList()
            val commandsWithLocalId = commandsById[ticketIdMap[ticketDto.ticketId]] ?: emptyList()
            if (ticketDto.userId == null) return@mapNotNull null
            mergeTicketInternal(
                userId = ticketDto.userId,
                ticketDto = ticketDto,
                commands = ticketCommands + commandsWithLocalId
            )
        } ?: emptyList()

        val createTicketCommands = commands.filter {
            it.commandType == CreateComment.ordinal && it.requestNewTicket == true
        }
        tickets += createTicketCommands.map {
            val ticketCommands = commandsById[it.ticketId] ?: emptyList()
            mapToFullTicket(it, ticketCommands)
        }

        val smallUsers = mapToSmallAcc(account) // TODO use account store
        val usersByAppId = smallUsers.groupBy { it.appId }

        val ticketsByUserId: Map<String, List<FullTicket>> = tickets.groupBy { it.userId }

        val applications = ticketsDto?.applications?.associateBy { it.appId }

        val ticketSetInfoList = usersByAppId.keys.map { appId ->
            val users = usersByAppId[appId] ?: emptyList()
            val setTickets = ArrayList<FullTicket>()
            for (user in users) {
                val userTickets = ticketsByUserId[user.userId] ?: continue
                setTickets += userTickets
            }
            val filteredTicketsData = setTickets.toHashSet().sortedWith(TicketComparator())

            // TODO kate это в компаратор
            val resultTickets = filteredTicketsData.filter { it.isActive == true }.toMutableList()
            resultTickets.addAll(filteredTicketsData.filter { it.isActive == false })

            val application = applications?.get(appId)
            val orgName = application?.orgName
            val orgLogoUrl = application?.orgLogoUrl
            TicketSetInfo(
                appId = appId,
                orgName = orgName ?: "",
                orgLogoUrl = orgLogoUrl,
                tickets = resultTickets,
            )
        }

        return ticketSetInfoList
    }

    fun mergeTicket(userId: String, ticketDto: TicketDto, commands: List<CommandEntity>): FullTicket {
        val ticketCommands = commands.filter { it.ticketId == ticketDto.ticketId }
        return mergeTicketInternal(userId, ticketDto, ticketCommands)
    }

    private fun mergeTicketInternal(
        userId: String,
        ticketDto: TicketDto,
        commands: List<CommandEntity>,
    ): FullTicket {

        val comments = (ticketDto.comments?.map { map(userId, it)} ?: emptyList()).toMutableList()
        val createCommentCommands = commands.filter { it.commandType == CreateComment.ordinal }
        comments += createCommentCommands.map(::map)
        comments.sortBy { it.creationTime }

        val hasReadCommands = commands.any { it.commandType == MarkTicketAsRead.ordinal }
        val isRead = ticketDto.isRead == true || hasReadCommands

        return mapToFullTicket(ticketDto, comments, userId, isRead)
    }

    fun mapToFullTicket(
        ticket: TicketDto,
        comments: List<Comment>,
        userId: String,
        isRead: Boolean,
    ): FullTicket {
        return FullTicket(
            comments = comments,
            showRating = ticket.showRating ?: false,
            showRatingText = ticket.showRatingText,
            userId = userId,
            ticketId = ticket.ticketId,
            subject = ticket.subject,
            isRead = isRead,
            lastComment = comments.lastOrNull(),
            isActive = ticket.isActive,
        )
    }

    private fun map(userId: String, commentDto: CommentDto): Comment = Comment(
        id = commentDto.commentId,
        isLocal = false,
        body = commentDto.body,
        isInbound = commentDto.isInbound && commentDto.author?.authorId == (account as? Account.V3)?.authorId,
        attachments = commentDto.attachments?.map{ map(userId, it)},
        creationTime = commentDto.creationDate.time,
        rating = commentDto.rating,
        author = commentDto.author?.let { map(it) },
        isSending = false,
    )

    fun map(commandEntity: CommandEntity): Comment = Comment(
        id = commandEntity.localId,
        isLocal = true,
        body = commandEntity.comment,
        isInbound = true,
        attachments = commandEntity.attachments?.map(::map),
        creationTime = commandEntity.creationTime,
        rating = commandEntity.rating,
        author = null,
        isSending = !commandEntity.isError
    )

    fun map(attachmentEntity: AttachmentEntity) : Attachment = Attachment(
        id = attachmentEntity.id,
        name = attachmentEntity.name,
        isImage = attachmentEntity.name.isImage(),
        isText = false,
        bytesSize = attachmentEntity.bytesSize,
        isVideo = false,
        uri = attachmentEntity.uri,
        status = Status.Processing,
        progress = null,
        guid = attachmentEntity.guid
    )

    fun map(attachment: Attachment) : AttachmentEntity = AttachmentEntity(
        id = attachment.id,
        name = attachment.name,
        guid = attachment.guid,
        bytesSize = attachment.bytesSize,
        uri = attachment.uri,
        progress = attachment.progress,
        status = attachment.progress
    )

    fun map(userId: String, attachmentDto: AttachmentDto): Attachment = Attachment(
        id = attachmentDto.id,
        name = attachmentDto.name,
        isImage = attachmentDto.name.isImage(),
        isText = attachmentDto.isText,
        bytesSize = attachmentDto.bytesSize,
        isVideo = attachmentDto.isVideo,
        uri =  Uri.parse(RequestUtils.getPreviewUrl(attachmentDto.id, account, findUserV3(userId))),
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

    fun mapToCommandErrorEntity(command: SyncRequest.Command) : CommandEntity {
        return when(command) {
            is SyncRequest.Command.CreateComment -> CommandEntity(
                isError = true,
                localId = command.localId,
                commandType = CreateComment.ordinal,
                commandId = command.commandId,
                userId = command.userId,
                appId = command.appId,
                creationTime = command.creationTime,
                requestNewTicket = command.requestNewTicket,
                ticketId = command.ticketId,
                commentId = command.localId,
                comment = command.comment,
                attachments = command.attachments?.map(::map),
                rating = command.rating,
                token = null,
                tokenType = null,
            )
            is SyncRequest.Command.MarkTicketAsRead -> CommandEntity(
                isError = true,
                localId = command.localId,
                commandType = CreateComment.ordinal,
                commandId = command.commandId,
                userId = command.userId,
                appId = command.appId,
                creationTime = command.creationTime,
                requestNewTicket = null,
                ticketId = command.ticketId,
                commentId = null,
                comment = null,
                attachments = null,
                rating = null,
                token = null,
                tokenType = null,
            )
            is SyncRequest.Command.SetPushToken -> CommandEntity(
                isError = true,
                localId = command.localId,
                commandType = CreateComment.ordinal,
                commandId = command.commandId,
                userId = command.userId,
                appId = command.appId,
                creationTime = command.creationTime,
                requestNewTicket = null,
                ticketId = null,
                commentId = null,
                comment = null,
                attachments = null,
                rating = null,
                token = command.token,
                tokenType = command.tokenType,
            )
        }
    }

    fun mapToFullTicket(
        command: CommandEntity,
        addCommentCommands: List<CommandEntity>,
    ): FullTicket {
        val initialComment = map(command)
        return mapToFullTicket(
            initialComment = initialComment,
            ticketId = command.ticketId!!,
            userId = command.userId,
            addCommentCommands = addCommentCommands,
        )
    }

    private fun mapToFullTicket(
        initialComment: Comment,
        ticketId: Long,
        userId: String,
        addCommentCommands: List<CommandEntity>,
    ): FullTicket {
        val comments = ArrayList<Comment>()
        comments += initialComment
        comments += addCommentCommands.map(::map)
        comments.sortBy { it.creationTime }
        val lastComment = comments.lastOrNull()

        return FullTicket(
            subject = initialComment.body,
            isRead = true,
            lastComment = lastComment,
            comments = comments,
            showRating = false,
            showRatingText = null,
            isActive = true,
            userId = userId,
            ticketId = ticketId
        )
    }

    private fun findUserV3(userId: String): User? {
        return (account as? Account.V3)?.users?.find { it.userId == userId }
    }

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
                o2.lastComment == null -> o1.ticketId.compareTo(o2.ticketId)
                else -> 1
            }
            o2.lastComment == null -> -1
            o1.lastComment.creationTime < o2.lastComment.creationTime -> 1
            o1.lastComment.creationTime > o2.lastComment.creationTime -> -1
            else -> (o1.lastComment.id - o2.lastComment.id).toInt()
        }
    }
}