package com.pyrus.pyrusservicedesk.sdk.repositories

import android.net.Uri
import com.pyrus.pyrusservicedesk.User
import com.pyrus.pyrusservicedesk._ref.data.Attachment
import com.pyrus.pyrusservicedesk._ref.data.Author
import com.pyrus.pyrusservicedesk._ref.data.Comment
import com.pyrus.pyrusservicedesk._ref.data.FullTicket
import com.pyrus.pyrusservicedesk._ref.data.TicketHeader
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
import com.pyrus.pyrusservicedesk.sdk.sync.CommandParamsDto.CommandsParamsType
import com.pyrus.pyrusservicedesk.sdk.sync.SyncRequest
import com.pyrus.pyrusservicedesk.sdk.sync.TicketCommandType.CreateComment
import com.pyrus.pyrusservicedesk.sdk.sync.TicketCommandType.MarkTicketAsRead

internal class RepositoryMapper(
    private val account: Account,
    private val idStore: IdStore
) {

    fun mergeTickets(ticketsDto: TicketsDto?, commands: List<CommandEntity>): List<TicketSetInfo> {

        val tickets = ArrayList<TicketHeader>()

        val commandsById = commands.groupBy { it.ticketId }
        tickets += ticketsDto?.tickets?.mapNotNull { ticketDto ->
            val ticketCommands = commandsById[ticketDto.ticketId] ?: emptyList()

            val localId = idStore.getTicketLocalId(ticketDto.ticketId)
            val commandsWithLocalId = commandsById[localId] ?: emptyList()

            if (ticketDto.userId == null) return@mapNotNull null

            mergeTicketHeader(
                userId = ticketDto.userId,
                ticketDto = ticketDto,
                commands = ticketCommands + commandsWithLocalId
            )
        } ?: emptyList()

        val createTicketCommands = commands.filter {
            it.ticketId != null
                && idStore.getTicketServerId(it.ticketId) == null
                && it.commandType == CreateComment.ordinal
                && it.requestNewTicket == true
        }.distinctBy { it.ticketId }

        tickets += createTicketCommands.map {
            val ticketCommands = commandsById[it.ticketId!!] ?: emptyList()
            mapToTicketHeader(it.ticketId, it.userId, ticketCommands)
        }

        val smallUsers = mapToSmallAcc(account) // TODO use account store
        val usersByAppId = smallUsers.groupBy { it.appId }

        val ticketsByUserId: Map<String, List<TicketHeader>> = tickets.groupBy { it.userId }

        val applications = ticketsDto?.applications?.associateBy { it.appId }

        val ticketSetInfoList = usersByAppId.keys.map { appId ->
            val users = usersByAppId[appId] ?: emptyList()
            val setTickets = ArrayList<TicketHeader>()
            for (user in users) {
                val userTickets = ticketsByUserId[user.userId] ?: continue
                setTickets += userTickets
            }
            val filteredTicketsData = setTickets.toHashSet().sortedWith(TicketHeaderComparator())

            val application = applications?.get(appId)
            val orgName = application?.orgName
            val orgLogoUrl = application?.orgLogoUrl
            TicketSetInfo(
                appId = appId,
                orgName = orgName ?: "",
                orgLogoUrl = orgLogoUrl,
                tickets = filteredTicketsData,
            )
        }

        return ticketSetInfoList
    }

    fun mergeTicket(
        userId: String,
        ticketDto: TicketDto,
        commands: List<CommandEntity>,
    ): FullTicket {

        val comments = (ticketDto.comments?.map { map(userId, it)} ?: emptyList()).toMutableList()
        val serverCommentIds = comments.map { it.id }.toSet()
        val createCommentCommands = commands.filter { it.commandType == CreateComment.ordinal }
        comments += createCommentCommands
            .filter { idStore.getCommentServerId(it.localId) !in serverCommentIds }
            .map(::map)

        comments.sortBy { it.creationTime }

        val hasReadCommands = commands.any { it.commandType == MarkTicketAsRead.ordinal }
        val isRead = ticketDto.isRead == true || hasReadCommands

        return mapToFullTicket(ticketDto, comments, userId, isRead)
    }

    private fun mergeTicketHeader(
        userId: String,
        ticketDto: TicketDto,
        commands: List<CommandEntity>,
    ): TicketHeader {

        val comments = ticketDto.comments?.sortedBy { it.creationDate } ?: emptyList()
        val serverCommentIds = comments.map { it.commentId }.toSet()
        val createCommentCommands = commands.filter { it.commandType == CreateComment.ordinal }

        val filteredCommands = createCommentCommands
            .filter { idStore.getCommentServerId(it.localId) !in serverCommentIds }
            .sortedBy { it.creationTime }

        val hasReadCommands = commands.any { it.commandType == MarkTicketAsRead.ordinal }
        val isRead = ticketDto.isRead == true || hasReadCommands

        val firstCommand: CommandEntity? = filteredCommands.firstOrNull()
        val lastCommand = filteredCommands.lastOrNull()

        val firstServerComment: CommentDto? = comments.firstOrNull()
        val lastServerComment = comments.lastOrNull()

        fun CommentDto?.olderThan(entity: CommandEntity?): Boolean {
            return (this?.creationDate ?: Long.MIN_VALUE) >= (entity?.creationTime ?: Long.MIN_VALUE)
        }

        val firstCommentText: String? = when {
            firstServerComment.olderThan(firstCommand) -> firstServerComment?.body
            else -> firstCommand?.comment
        }
        val lastCommentText: String? = when {
            lastServerComment.olderThan(lastCommand) -> firstServerComment?.body
            else -> firstCommand?.comment
        }
        val lastCommentCreationDate: Long? = when {
            lastServerComment.olderThan(lastCommand) -> firstServerComment?.creationDate
            else -> firstCommand?.creationTime
        }

        return TicketHeader(
            userId = userId,
            ticketId = ticketDto.ticketId,
            subject = firstCommentText,
            isRead = isRead,
            lastCommentText = lastCommentText,
            lastCommentCreationDate = lastCommentCreationDate,
            isActive = ticketDto.isActive,
        )
    }

    private fun mapToFullTicket(
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
        creationTime = commentDto.creationDate,
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

    fun mapToSyncRequest(entity: CommandEntity): SyncRequest.Command? {
        return when(entity.commandType) {
            CommandsParamsType.CreateComment.ordinal -> SyncRequest.Command.CreateComment(
                localId = entity.localId,
                commandId = entity.commandId,
                userId = entity.userId,
                appId = entity.appId,
                creationTime = entity.creationTime,
                requestNewTicket = entity.requestNewTicket ?: return null,
                ticketId = entity.ticketId ?: return null,
                comment = entity.comment,
                attachments = entity.attachments?.map(::map),
                rating = entity.rating
            )
            CommandsParamsType.MarkTicketAsRead.ordinal -> SyncRequest.Command.MarkTicketAsRead(
                localId = entity.localId,
                commandId = entity.commandId,
                userId = entity.userId,
                appId = entity.appId,
                creationTime = entity.creationTime,
                ticketId = entity.ticketId ?: return null,
            )
            CommandsParamsType.SetPushToken.ordinal -> SyncRequest.Command.SetPushToken(
                localId = entity.localId,
                commandId = entity.commandId,
                userId = entity.userId,
                appId = entity.appId,
                creationTime = entity.creationTime,
                token = entity.token ?: return null,
                tokenType = entity.tokenType ?: return null,
            )
            else -> null
        }
    }

    fun mapToCommandErrorEntity(command: SyncRequest.Command): CommandEntity {
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

    private fun mapToTicketHeader(
        ticketId: Long,
        userId: String,
        addCommentCommands: List<CommandEntity>,
    ): TicketHeader {
        val commands = addCommentCommands.sortedBy { it.creationTime }

        val lastComment = commands.lastOrNull()
        val firstComment = commands.firstOrNull()

        return TicketHeader(
            subject = firstComment?.comment,
            isRead = true,
            lastCommentText = lastComment?.comment,
            lastCommentCreationDate = lastComment?.creationTime,
            isActive = true,
            userId = userId,
            ticketId = ticketId
        )
    }

    fun mapToFullTicket(
        ticketId: Long,
        userId: String,
        addCommentCommands: List<CommandEntity>,
    ): FullTicket {
        val comments = ArrayList<Comment>()
        comments += addCommentCommands.map(::map)
        comments.sortBy { it.creationTime }
        val lastComment = comments.lastOrNull()
        val firstComment = comments.firstOrNull()

        return FullTicket(
            subject = firstComment?.body,
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

private class TicketHeaderComparator : Comparator<TicketHeader> {

    override fun compare(o1: TicketHeader, o2: TicketHeader): Int {
        val o1IsActive = if (o1.isActive == true) 1 else 0
        val o2IsActive = if (o2.isActive == true) 1 else 0
        return when {
            o1IsActive < o2IsActive -> 1
            o1IsActive > o2IsActive -> -1
            o1.lastCommentCreationDate == null -> return when {
                o2.lastCommentCreationDate == null -> o1.ticketId.compareTo(o2.ticketId)
                else -> 1
            }
            o2.lastCommentCreationDate == null -> -1
            o1.lastCommentCreationDate < o2.lastCommentCreationDate -> 1
            o1.lastCommentCreationDate > o2.lastCommentCreationDate -> -1
            else -> 0
        }
    }
}