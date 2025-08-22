package com.pyrus.pyrusservicedesk.sdk.repositories

import androidx.core.net.toUri
import com.pyrus.pyrusservicedesk.User
import com.pyrus.pyrusservicedesk._ref.data.Attachment
import com.pyrus.pyrusservicedesk._ref.data.Author
import com.pyrus.pyrusservicedesk._ref.data.Comment
import com.pyrus.pyrusservicedesk._ref.data.FullTicket
import com.pyrus.pyrusservicedesk._ref.data.TicketHeader
import com.pyrus.pyrusservicedesk._ref.data.multy_chat.TicketSetInfo
import com.pyrus.pyrusservicedesk._ref.data.multy_chat.TicketsInfo
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.search.SearchResult
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.fingerprints.Status
import com.pyrus.pyrusservicedesk._ref.utils.RequestUtils
import com.pyrus.pyrusservicedesk._ref.utils.RequestUtils.getAvatarUrl
import com.pyrus.pyrusservicedesk._ref.utils.RequestUtils.getOrganisationLogoUrl
import com.pyrus.pyrusservicedesk._ref.utils.isAudio
import com.pyrus.pyrusservicedesk._ref.utils.isImage
import com.pyrus.pyrusservicedesk.core.Account
import com.pyrus.pyrusservicedesk.core.getAuthorId
import com.pyrus.pyrusservicedesk.core.getInstanceId
import com.pyrus.pyrusservicedesk.core.getUsers
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.AttachmentEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.AuthorEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.CommandEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.CommentInfo
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.LocalAttachmentEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.TicketEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.UserEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.support.ApplicationWithUsersEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.support.CommandWithAttachmentsEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.support.CommandWithHeader
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.support.CommentWithAttachmentsEntity
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.support.TicketWithComment
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.support.TicketWithComments
import com.pyrus.pyrusservicedesk.sdk.sync.CommandParamsDto.CommandsParamsType
import com.pyrus.pyrusservicedesk.sdk.sync.SyncRequest
import com.pyrus.pyrusservicedesk.sdk.sync.TicketCommandType.CreateComment
import com.pyrus.pyrusservicedesk.sdk.sync.TicketCommandType.MarkTicketAsRead

internal class RepositoryMapper(
    private val idStore: IdStore,
) {

    fun mergeData(
        account: Account,
        applications: List<ApplicationWithUsersEntity>,
        commands: List<CommandWithAttachmentsEntity>,
    ): TicketsInfo {
        val usersWithData = applications.flatMap { it.users.map { entity -> mapToUser(entity.user)} }

        return TicketsInfo(
            usersWithData = usersWithData,
            users = account.getUsers(),
            ticketSetInfoList = mergeTickets(account, applications, commands),
        )
    }

    fun mapToSearchResult(account: Account, entity: CommandWithHeader): SearchResult? {

        val title: String = when(val header = entity.header) {
            is CommandEntity -> header.comment ?: ""
            is TicketEntity -> header.subject
        }

        return SearchResult(
            ticketId = entity.command.command.ticketId ?: return null,
            userId = entity.command.command.userId ?: return null, //use in multiChat and in multiChat we mast have user id
            commentId = entity.command.command.commentId ?: return null,
            title = title,
            commentInfo = mapToCommentInfo(account, entity.command),
            creationTime = entity.command.command.creationTime
        )
    }

    private fun mapToCommentInfo(account: Account, entity: CommentInfo): TicketHeader.LastComment {
        val author = entity.author?.let { map(account, entity.author) }
        return TicketHeader.LastComment(
            entity.commentId,
            author,
            entity.body,
            entity.lastAttachmentName,
            entity.creationDate
        )
    }

    fun mapToSearchResult(account: Account, entity: TicketWithComment): SearchResult? {
        return SearchResult(
            ticketId = entity.ticket.ticketId,
            userId = entity.ticket.userId,
            commentId = entity.comment?.commentId,
            title = entity.ticket.subject,
            commentInfo = entity.comment?.let { mapToCommentInfo(account, it) },
            creationTime = entity.comment?.creationDate ?: entity.ticket.createdAt
        )
    }

    private fun mapToUser(entity: UserEntity): User = User(
        userId = entity.userId,
        appId = entity.appId,
        userName = entity.userName,
    )

    private fun mergeTickets(
        account: Account,
        applicationsWithComments: List<ApplicationWithUsersEntity>,
        commands: List<CommandWithAttachmentsEntity>,
    ): List<TicketSetInfo> {

        val ticketEntities = applicationsWithComments.flatMap { it.users }.flatMap { it.tickets }

        val tickets = ArrayList<TicketHeader>()

        val commandsById = commands.groupBy { it.command.ticketId }
        tickets += ticketEntities.map { ticketEntity ->
            val ticketCommands = commandsById[ticketEntity.ticketId] ?: emptyList()

            val localId = idStore.getTicketLocalId(ticketEntity.ticketId)
            val commandsWithLocalId = commandsById[localId] ?: emptyList()

            val userId = ticketEntity.userId

            mergeTicketHeader(
                userId = userId,
                ticketEntity = ticketEntity,
                commands = ticketCommands + commandsWithLocalId,
                account = account
            )
        }

        val createTicketCommands = commands.filter {
            it.command.ticketId != null
                && idStore.getTicketServerId(it.command.ticketId) == null
                && it.command.commandType == CreateComment.ordinal
                && it.command.ticketId < 0
        }.distinctBy { it.command.ticketId }

        tickets += createTicketCommands.map {
            val ticketCommands = commandsById[it.command.ticketId!!] ?: emptyList()
            mapToTicketHeader(account, it.command.ticketId, it.command.userId ?: account.getInstanceId(), ticketCommands)
        }

        val smallUsers = mapToSmallAcc(account)
        val usersByAppId = smallUsers.groupBy { it.appId }

        val ticketsByUserId: Map<String, List<TicketHeader>> = tickets.groupBy { it.userId }

        val applications = applicationsWithComments.map{it.application}.associateBy { it.appId }

        val ticketSetInfoList = usersByAppId.keys.map { appId ->
            val users = usersByAppId[appId] ?: emptyList()
            val setTickets = ArrayList<TicketHeader>()
            for (user in users) {
                val userTickets = ticketsByUserId[user.userId] ?: continue
                setTickets += userTickets
            }
            val sortedTicketsData = setTickets.toHashSet().sortedWith(TicketHeaderComparator())

            val application = applications[appId]
            val orgName = application?.orgName
            val orgLogoUrl = application?.orgLogoUrl?.let { getOrganisationLogoUrl(it, account.domain) }
            TicketSetInfo(
                appId = appId,
                userIds = users.map { it.userId }.toSet(),
                orgName = orgName ?: "",
                orgLogoUrl = orgLogoUrl,
                tickets = sortedTicketsData,
            )
        }

        return ticketSetInfoList
    }

    fun mergeTicket(
        account: Account,
        userId: String,
        ticket: TicketWithComments,
        commands: List<CommandWithAttachmentsEntity>,
        orgLogoUrl: String?,
    ): FullTicket {

        val comments = ticket.comments.map { map(account, userId, it) }.toMutableList()

        val serverCommentIds = comments.map { it.id }.toSet()
        val createCommentCommands = commands.filter { it.command.commandType == CreateComment.ordinal }
        comments += createCommentCommands
            .filter { idStore.getCommentServerId(it.command.localId) !in serverCommentIds }
            .map(::map)

        comments.sortWith(
            compareBy({ it.isSending }, { it.creationTime }, { it.id })
        )

        val hasReadCommands = commands.any { it.command.commandType == MarkTicketAsRead.ordinal }
        val isRead = ticket.ticket.isRead == true || hasReadCommands

        return mapToFullTicket(ticket, comments, userId, orgLogoUrl, isRead)
    }

    private fun mergeTicketHeader(
        userId: String,
        ticketEntity: TicketEntity,
        commands: List<CommandWithAttachmentsEntity>,
        account: Account,
    ): TicketHeader {

        val createCommentCommands = commands.filter { it.command.commandType == CreateComment.ordinal }

        val lastServerComment = ticketEntity.lastComment
        val lastServerCommentId = lastServerComment?.commentId ?: 0

        val filteredCommands = createCommentCommands
            .filter {
                val serverId = idStore.getCommentServerId(it.command.localId)
                serverId == null || serverId > lastServerCommentId
            }
            .sortedWith(compareBy({ it.command.creationTime }, { -it.command.localId }))

        val hasReadCommands = commands.any { it.command.commandType == MarkTicketAsRead.ordinal }
        val isRead = ticketEntity.isRead == true || hasReadCommands
        val isLoading = filteredCommands.any { !it.command.isError }

        val lastCommand = filteredCommands.lastOrNull()

        fun CommentInfo?.olderThan(entity: CommandEntity?): Boolean {
            return (this?.creationDate ?: Long.MIN_VALUE) >= (entity?.creationTime ?: Long.MIN_VALUE)
        }

        val lastComment = when {
            lastServerComment.olderThan(lastCommand?.command) -> lastServerComment?.let {
                TicketHeader.LastComment(
                    id = it.commentId,
                    author = it.author?.let { dto -> map(account, dto) },
                    body = it.body,
                    lastAttachmentName = it.lastAttachmentName,
                    creationDate = it.creationDate
                )
            }
            else -> lastCommand?.let { mapToCommentInfo(account, it) }
        }

        return TicketHeader(
            userId = userId,
            ticketId = ticketEntity.ticketId,
            subject = ticketEntity.subject,
            isRead = isRead,
            lastComment = lastComment,
            isActive = ticketEntity.isActive == true,
            isLoading = isLoading,
            createdAt = ticketEntity.createdAt,
        )
    }

    private fun mapToAuthor(account: Account, userId: String?): Author = Author(
        isUser = true,
        name = account.getUsers().find { it.userId == userId }?.userName,
        authorId = (account as? Account.V3)?.authorId,
        avatarUrl = null,
        avatarColor = null
    )

    private fun mapToFullTicket(
        ticket: TicketWithComments,
        comments: List<Comment>,
        userId: String,
        orgLogoUrl: String?,
        isRead: Boolean,
    ): FullTicket {
        return FullTicket(
            comments = comments,
            showRating = ticket.ticket.showRating == true,
            showRatingText = ticket.ticket.showRatingText,
            userId = userId,
            ticketId = ticket.ticket.ticketId,
            subject = ticket.ticket.subject,
            orgLogoUrl = orgLogoUrl,
            isActive = ticket.ticket.isActive == true,
            isRead = isRead,
        )
    }

    private fun map(account: Account, userId: String, commentEntity: CommentWithAttachmentsEntity): Comment = Comment(
        id = commentEntity.comment.commentId,
        persistentId = idStore.getCommentLocalId(commentEntity.comment.commentId) ?: commentEntity.comment.commentId,
        isLocal = false,
        body = commentEntity.comment.body,
        isInbound = commentEntity.comment.isInbound && commentEntity.comment.author?.id == (account as? Account.V3)?.authorId,
        isSupport = !commentEntity.comment.isInbound,
        attachments = commentEntity.attachments.map{ map(account, userId, it)},
        creationTime = commentEntity.comment.creationDate,
        rating = commentEntity.comment.rating,
        author = commentEntity.comment.author?.let { map(account, it) },
        isSending = false,
    )

    fun map(commandEntity: CommandWithAttachmentsEntity): Comment = Comment(
        id = commandEntity.command.localId,
        persistentId = commandEntity.command.localId,
        isLocal = true,
        body = commandEntity.command.comment,
        isInbound = true,
        isSupport = false,
        attachments = commandEntity.attachments?.map(::map),
        creationTime = commandEntity.command.creationTime,
        rating = commandEntity.command.rating,
        author = null,
        isSending = !commandEntity.command.isError
    )

    fun map(attachmentEntity: LocalAttachmentEntity) : Attachment = Attachment(
        id = attachmentEntity.id,
        name = attachmentEntity.name,
        isImage = attachmentEntity.name.isImage(),
        isText = false,
        bytesSize = attachmentEntity.bytesSize,
        isVideo = false,
        uri = attachmentEntity.uri.toUri(),
        status = Status.Processing,
        progress = null,
        guid = attachmentEntity.guid
    )

    fun map(commandId: String, attachment: Attachment) : LocalAttachmentEntity = LocalAttachmentEntity(
        id = attachment.id,
        commandId = commandId,
        name = attachment.name,
        guid = attachment.guid,
        bytesSize = attachment.bytesSize,
        uri = attachment.uri.toString(),
        progress = attachment.progress,
        status = attachment.progress
    )

    fun map(account: Account, userId: String, entity: AttachmentEntity): Attachment {
        val url = if (entity.name.isAudio()) {
            RequestUtils.getFileUrl(entity.id, account, findUser(account, userId)).toUri()
        }
        else {
            RequestUtils.getPreviewUrl(entity.id, account, findUser(account, userId)).toUri()
        }
        return Attachment(
            id = entity.id,
            name = entity.name,
            isImage = entity.name.isImage(),
            isText = entity.isText,
            bytesSize = entity.bytesSize,
            isVideo = entity.isVideo,
            uri = url,
            status = Status.Completed,
            progress = null,
            guid = entity.guid,
        )
    }

    private fun map(account: Account, authorEntity: AuthorEntity) = Author(
        isUser = account.getAuthorId() == authorEntity.id,
        name = authorEntity.name,
        authorId = authorEntity.id,
        avatarUrl = when (authorEntity.avatarId) {
            null,
            0,
                -> null
            else -> getAvatarUrl(authorEntity.avatarId, account.domain)
        },
        avatarColor = authorEntity.avatarColorString,
    )

    fun mapToSyncRequest(entity: CommandWithAttachmentsEntity): SyncRequest.Command? {
        val command = entity.command
        return when(command.commandType) {
            CommandsParamsType.CreateComment.ordinal -> SyncRequest.Command.CreateComment(
                localId = command.localId,
                commandId = command.commandId,
                userId = command.userId,
                appId = command.appId,
                creationTime = command.creationTime,
                requestNewTicket = command.requestNewTicket ?: return null,
                ticketId = command.ticketId ?: return null,
                comment = command.comment,
                attachments = entity.attachments?.map(::map),
                rating = command.rating
            )
            CommandsParamsType.MarkTicketAsRead.ordinal -> SyncRequest.Command.MarkTicketAsRead(
                localId = command.localId,
                commandId = command.commandId,
                userId = command.userId,
                appId = command.appId,
                creationTime = command.creationTime,
                ticketId = command.ticketId ?: return null,
            )
            CommandsParamsType.SetPushToken.ordinal -> SyncRequest.Command.SetPushToken(
                localId = command.localId,
                commandId = command.commandId,
                userId = command.userId,
                appId = command.appId,
                creationTime = command.creationTime,
                token = command.token ?: return null,
                tokenType = command.tokenType ?: return null,
            )
            else -> null
        }
    }

    fun mapToCommandEntity(isError: Boolean, command: SyncRequest.Command, instanceId: String): CommandWithAttachmentsEntity {
        val entity = when(command) {
            is SyncRequest.Command.CreateComment -> CommandEntity(
                isError = isError,
                localId = command.localId,
                commandType = CreateComment.ordinal,
                commandId = command.commandId,
                userId = command.userId ?: instanceId,
                appId = command.appId,
                creationTime = command.creationTime,
                requestNewTicket = false,
                ticketId = command.ticketId,
                commentId = command.localId,
                comment = command.comment,
                rating = command.rating,
                token = null,
                tokenType = null,
            )
            is SyncRequest.Command.MarkTicketAsRead -> CommandEntity(
                isError = isError,
                localId = command.localId,
                commandType = CreateComment.ordinal,
                commandId = command.commandId,
                userId = command.userId ?: instanceId,
                appId = command.appId,
                creationTime = command.creationTime,
                requestNewTicket = null,
                ticketId = command.ticketId,
                commentId = null,
                comment = null,
                rating = null,
                token = null,
                tokenType = null,
            )
            is SyncRequest.Command.SetPushToken -> CommandEntity(
                isError = isError,
                localId = command.localId,
                commandType = CreateComment.ordinal,
                commandId = command.commandId,
                userId = command.userId ?: instanceId,
                appId = command.appId,
                creationTime = command.creationTime,
                requestNewTicket = null,
                ticketId = null,
                commentId = null,
                comment = null,
                rating = null,
                token = command.token,
                tokenType = command.tokenType,
            )
        }
        val attachments = when (command) {
            is SyncRequest.Command.CreateComment -> command.attachments?.map {
                map(command.commandId, it)
            }

            is SyncRequest.Command.MarkTicketAsRead -> null
            is SyncRequest.Command.SetPushToken -> null
        }


        return CommandWithAttachmentsEntity(entity, attachments)
    }

    private fun mapToCommentInfo(account: Account, command: CommandWithAttachmentsEntity): TicketHeader.LastComment {
        return TicketHeader.LastComment(
            id = command.command.localId,
            author = mapToAuthor(account, command.command.userId),
            body = command.command.comment,
            lastAttachmentName = command.attachments?.lastOrNull()?.name,
            creationDate = command.command.creationTime
        )
    }

    private fun mapToTicketHeader(
        account: Account,
        ticketId: Long,
        userId: String,
        addCommentCommands: List<CommandWithAttachmentsEntity>,
    ): TicketHeader {
        val commands = addCommentCommands.sortedWith(compareBy({ it.command.creationTime }, { -it.command.localId }))

        val lastCommand = commands.lastOrNull()
        val firstComment = commands.firstOrNull()
        val lastComment = lastCommand?.let { mapToCommentInfo(account, it) }
        val isLoading = commands.any { !it.command.isError }

        return TicketHeader(
            subject = firstComment?.command?.comment,
            isRead = true,
            lastComment = lastComment,
            isActive = true,
            userId = userId,
            ticketId = ticketId,
            isLoading = isLoading,
            createdAt = firstComment?.command?.creationTime,
        )
    }

    fun mapToFullTicket(
        ticketId: Long,
        userId: String,
        addCommentCommands: List<CommandWithAttachmentsEntity>,
        orgLogoUrl: String?,
    ): FullTicket {
        val comments = addCommentCommands
            .map(::map)
            .sortedWith(compareBy({ it.creationTime }, { it.id }))

        val firstComment = comments.firstOrNull()

        return FullTicket(
            subject = firstComment?.body,
            comments = comments,
            showRating = false,
            showRatingText = null,
            userId = userId,
            ticketId = ticketId,
            orgLogoUrl = orgLogoUrl,
            isActive = true,
            isRead = true,
        )
    }

    private fun findUser(account: Account, userId: String): User? {
        return account.getUsers().find { it.userId == userId }
    }

    private fun mapToSmallAcc(account: Account): List<SmallUser> = when(account) {
        is Account.V1 -> listOf(SmallUser(account.instanceId, account.appId))
        is Account.V2 -> listOf(SmallUser(account.userId, account.appId))
        is Account.V3 -> account.users.map { SmallUser(it.userId, it.appId) }
    }

    private data class SmallUser(
        val userId: String,
        val appId: String,
    )

}

private class TicketHeaderComparator : Comparator<TicketHeader> {

    override fun compare(o1: TicketHeader, o2: TicketHeader): Int {
        val o1IsActive = if (o1.isActive) 1 else 0
        val o2IsActive = if (o2.isActive) 1 else 0
        return when {
            o1IsActive < o2IsActive -> 1
            o1IsActive > o2IsActive -> -1
            o1.lastComment == null -> return when {
                o2.lastComment == null -> {
                    o2.createdAt?.let { o1.createdAt?.compareTo(o2.createdAt) }
                        ?: o1.ticketId.compareTo(o2.ticketId)
                }
                else -> 1
            }
            o2.lastComment == null -> -1
            o1.lastComment.creationDate < o2.lastComment.creationDate -> 1
            o1.lastComment.creationDate > o2.lastComment.creationDate -> -1
            o1.lastComment.id < o2.lastComment.id -> 1
            o1.lastComment.id > o2.lastComment.id -> -1
            else -> 0
        }
    }
}