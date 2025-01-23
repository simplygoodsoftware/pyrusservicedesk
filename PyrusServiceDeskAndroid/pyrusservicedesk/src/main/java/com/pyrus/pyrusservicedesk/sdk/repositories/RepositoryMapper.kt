package com.pyrus.pyrusservicedesk.sdk.repositories

import android.net.Uri
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.User
import com.pyrus.pyrusservicedesk._ref.data.Attachment
import com.pyrus.pyrusservicedesk._ref.data.Author
import com.pyrus.pyrusservicedesk._ref.data.Comment
import com.pyrus.pyrusservicedesk._ref.data.FullTicket
import com.pyrus.pyrusservicedesk._ref.data.TicketHeader
import com.pyrus.pyrusservicedesk._ref.data.multy_chat.TicketSetInfo
import com.pyrus.pyrusservicedesk._ref.utils.RequestUtils
import com.pyrus.pyrusservicedesk._ref.utils.RequestUtils.getAvatarUrl
import com.pyrus.pyrusservicedesk._ref.utils.TextProvider
import com.pyrus.pyrusservicedesk._ref.utils.isImage
import com.pyrus.pyrusservicedesk._ref.utils.isVideo
import com.pyrus.pyrusservicedesk._ref.utils.textRes
import com.pyrus.pyrusservicedesk.core.Account
import com.pyrus.pyrusservicedesk.core.getUserId
import com.pyrus.pyrusservicedesk.core.getUsers
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
    private val idStore: IdStore,
) {

    fun mergeTickets(
        account: Account,
        ticketsDto: TicketsDto?,
        commands: List<CommandEntity>,
    ): List<TicketSetInfo> {

        val tickets = ArrayList<TicketHeader>()

        val commandsById = commands.groupBy { it.ticketId }
        tickets += ticketsDto?.tickets?.mapNotNull { ticketDto ->
            val ticketCommands = commandsById[ticketDto.ticketId] ?: emptyList()

            val localId = idStore.getTicketLocalId(ticketDto.ticketId)
            val commandsWithLocalId = commandsById[localId] ?: emptyList()

            val userId = ticketDto.userId ?: return@mapNotNull null

            mergeTicketHeader(
                userId = userId,
                ticketDto = ticketDto,
                commands = ticketCommands + commandsWithLocalId,
                account = account
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

        val smallUsers = mapToSmallAcc(account)
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
            val sortedTicketsData = setTickets.toHashSet().sortedWith(TicketHeaderComparator())

            val application = applications?.get(appId)
            val orgName = application?.orgName
            val orgLogoUrl = application?.orgLogoUrl
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
        ticketDto: TicketDto,
        commands: List<CommandEntity>,
        orgLogoUrl: String?,
    ): FullTicket {

        val comments = (ticketDto.comments?.map { map(account, userId, it)} ?: emptyList()).toMutableList()
        val serverCommentIds = comments.map { it.id }.toSet()
        val createCommentCommands = commands.filter { it.commandType == CreateComment.ordinal }
        comments += createCommentCommands
            .filter { idStore.getCommentServerId(it.localId) !in serverCommentIds }
            .map(::map)

        comments.sortBy { it.creationTime }

        val hasReadCommands = commands.any { it.commandType == MarkTicketAsRead.ordinal }
        val isRead = ticketDto.isRead == true || hasReadCommands

        return mapToFullTicket(ticketDto, comments, userId, isRead, orgLogoUrl)
    }

    private fun mergeTicketHeader(
        userId: String,
        ticketDto: TicketDto,
        commands: List<CommandEntity>,
        account: Account,
    ): TicketHeader {

        val comments = ticketDto.comments?.sortedBy { it.creationDate } ?: emptyList()
        val serverCommentIds = comments.map { it.commentId }.toSet()
        val createCommentCommands = commands.filter { it.commandType == CreateComment.ordinal }

        val filteredCommands = createCommentCommands
            .filter { idStore.getCommentServerId(it.localId) !in serverCommentIds }
            .sortedBy { it.creationTime }

        val hasReadCommands = commands.any { it.commandType == MarkTicketAsRead.ordinal }
        val isRead = ticketDto.isRead == true || hasReadCommands
        val isLoading = filteredCommands.isNotEmpty()

        val lastCommand = filteredCommands.lastOrNull()
        val lastServerComment = comments.lastOrNull()

        fun CommentDto?.olderThan(entity: CommandEntity?): Boolean {
            return (this?.creationDate ?: Long.MIN_VALUE) >= (entity?.creationTime ?: Long.MIN_VALUE)
        }

        val lastCommentText: TextProvider? = when {
            lastServerComment.olderThan(lastCommand) -> lastServerComment?.getLastComment(account as Account.V3)
            else -> lastCommand?.getLastCommentFromAttachmentEntity()
        }
        val lastCommentCreationDate: Long? = when {
            lastServerComment.olderThan(lastCommand) -> lastServerComment?.creationDate
            else -> lastCommand?.creationTime
        }

        return TicketHeader(
            userId = userId,
            ticketId = ticketDto.ticketId,
            subject = ticketDto.subject,
            isRead = isRead,
            lastCommentText = lastCommentText,
            lastCommentCreationDate = lastCommentCreationDate,
            isActive = ticketDto.isActive,
            isLoading = isLoading,
        )
    }

    private fun CommentDto.getLastComment(account: Account.V3?): TextProvider {
        when {
            !this.body.isNullOrBlank() && isYou(this.author, account) ->
                return TextProvider.Format(R.string.last_comment_you, listOf(this.body))
            !this.body.isNullOrBlank() ->
                return TextProvider.Format(R.string.last_comment, listOf(this.author?.name ?: "", this.body))
            !this.attachments.isNullOrEmpty() && this.attachments.last().name.isVideo() && isYou(this.author, account) ->
                return R.string.last_comment_clip_video_y.textRes()
            !this.attachments.isNullOrEmpty() && this.attachments.last().name.isVideo() ->
                return TextProvider.Format(R.string.last_comment_clip_video, listOf(this.author?.name ?: ""))
            !this.attachments.isNullOrEmpty() && this.attachments.last().name.isImage() && isYou(this.author, account)->
                return R.string.last_comment_clip_photo_y.textRes()
            !this.attachments.isNullOrEmpty() && this.attachments.last().name.isImage() ->
                return TextProvider.Format(R.string.last_comment_clip_photo, listOf(this.author?.name ?: ""))
            !this.attachments.isNullOrEmpty() ->
                return TextProvider.Format(R.string.last_comment_clip_file, listOf(this.author?.name ?: ""))
        }
        return "".textRes()
    }
    private fun isYou(author: AuthorDto?, account: Account.V3?): Boolean {
        return author?.authorId == account?.authorId
    }

    private fun  CommandEntity.getLastCommentFromAttachmentEntity(): TextProvider {
        when {
            !this.comment.isNullOrBlank()  ->
                return TextProvider.Format(R.string.last_comment_you, listOf(this.comment))
            !this.attachments.isNullOrEmpty() && this.attachments.last().name.isImage() ->
                return R.string.last_comment_clip_photo_y.textRes()
            !this.attachments.isNullOrEmpty() ->
                return R.string.last_comment_clip_video_y.textRes()
        }
        return "".textRes()
    }

    private fun mapToFullTicket(
        ticket: TicketDto,
        comments: List<Comment>,
        userId: String,
        isRead: Boolean,
        orgLogoUrl: String?,
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
            orgLogoUrl = orgLogoUrl
        )
    }

    private fun map(account: Account, userId: String, commentDto: CommentDto): Comment = Comment(
        id = commentDto.commentId,
        isLocal = false,
        body = commentDto.body,
        isInbound = commentDto.isInbound && commentDto.author?.authorId == (account as? Account.V3)?.authorId,
        isSupport = !commentDto.isInbound,
        attachments = commentDto.attachments?.map{ map(account, userId, it)},
        creationTime = commentDto.creationDate,
        rating = commentDto.rating,
        author = commentDto.author?.let { map(account, it) },
        isSending = false,
    )

    fun map(commandEntity: CommandEntity): Comment = Comment(
        id = commandEntity.localId,
        isLocal = true,
        body = commandEntity.comment,
        isInbound = true,
        isSupport = false,
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

    fun map(account: Account, userId: String, attachmentDto: AttachmentDto): Attachment = Attachment(
        id = attachmentDto.id,
        name = attachmentDto.name,
        isImage = attachmentDto.name.isImage(),
        isText = attachmentDto.isText,
        bytesSize = attachmentDto.bytesSize,
        isVideo = attachmentDto.isVideo,
        uri =  Uri.parse(RequestUtils.getPreviewUrl(attachmentDto.id, account, findUser(account, userId))),
        status = Status.Completed,
        progress = null,
        guid = attachmentDto.guid,
    )

    private fun map(account: Account, authorDto: AuthorDto) = Author(
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

    fun mapToCommandEntity(isError: Boolean, command: SyncRequest.Command): CommandEntity {
        return when(command) {
            is SyncRequest.Command.CreateComment -> CommandEntity(
                isError = isError,
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
                isError = isError,
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
                isError = isError,
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
            lastCommentText = lastComment?.comment?.textRes(),
            lastCommentCreationDate = lastComment?.creationTime,
            isActive = true,
            userId = userId,
            ticketId = ticketId,
            isLoading = true,
        )
    }

    fun mapToFullTicket(
        ticketId: Long,
        userId: String,
        addCommentCommands: List<CommandEntity>,
        orgLogoUrl: String?
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
            ticketId = ticketId,
            orgLogoUrl = orgLogoUrl,
        )
    }

    private fun findUser(account: Account, userId: String): User? {
        return account.getUsers().find { it.userId == userId }
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