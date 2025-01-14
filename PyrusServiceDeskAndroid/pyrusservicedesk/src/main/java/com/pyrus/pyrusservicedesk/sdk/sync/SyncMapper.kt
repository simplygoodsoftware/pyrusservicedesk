package com.pyrus.pyrusservicedesk.sdk.sync

import com.pyrus.pyrusservicedesk.AppResourceManager
import com.pyrus.pyrusservicedesk._ref.data.Attachment
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk.core.Account
import com.pyrus.pyrusservicedesk.core.getAdditionalUsers
import com.pyrus.pyrusservicedesk.core.getAppId
import com.pyrus.pyrusservicedesk.core.getAuthorId
import com.pyrus.pyrusservicedesk.core.getInstanceId
import com.pyrus.pyrusservicedesk.core.getSecurityKey
import com.pyrus.pyrusservicedesk.core.getUserId
import com.pyrus.pyrusservicedesk.core.getVersion
import com.pyrus.pyrusservicedesk.sdk.data.AttachmentDataDto
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.TicketsDto
import com.pyrus.pyrusservicedesk.sdk.web.request_body.RequestBodyBase
import kotlin.math.max

internal object SyncMapper {

    private const val API_FLAG = "AAAAAAAAAAAU"

    fun mapToGetFeedRequest(syncRequests: List<SyncReqRes>, localState: TicketsDto?, account: Account, resourceManager: AppResourceManager): RequestBodyBase {

        val request = RequestBodyBase(
            needFullInfo = true,
            additionalUsers = account.getAdditionalUsers(localState),
            lastNoteId = calcLastNoteId(localState, account.getUserId()),
            commands = syncRequests.mapNotNull { mapToCommand(it.request) },
            authorId = account.getAuthorId(),
            authorName = ConfigUtils.getAuthorName(resourceManager),
            appId = account.getAppId(),
            userId = account.getUserId(),
            securityKey = account.getSecurityKey(),
            instanceId = account.getInstanceId(),
            version = account.getVersion(),
            apiSign = API_FLAG,
        )
        return request
    }

    fun calcLastNoteId(localState: TicketsDto?, userId: String): Long? {
        val lastNoteId: Long?
        if (localState == null) lastNoteId = null
        else {
            val tickets = localState.tickets ?: return null

            var maxNoteId: Long = 0
            for (ticket in tickets) {
                val lastComment = ticket.lastComment
                if (lastComment != null && ticket.userId == userId) {
                    maxNoteId = max(lastComment.commentId, maxNoteId)
                }
            }
            lastNoteId = if (maxNoteId <= 0L) null else maxNoteId
        }
        return lastNoteId
    }

    private fun mapToCommand(request: SyncRequest): TicketCommandDto? = when(request) {
        is SyncRequest.Command.AddComment -> TicketCommandDto(
            commandId = request.commandId,
            type = TicketCommandType.CreateComment.ordinal,
            appId = request.appId,
            userId = request.userId,
            params = CommandParamsDto.CreateComment(
                requestNewTicket = false,
                userId = request.userId,
                appId = request.appId,
                comment = request.comment,
                attachments = request.attachments?.map(::mapToAttachmentDataDto),
                ticketId = request.ticketId,
                rating = request.rating,
            ),
        )
        is SyncRequest.Command.CreateTicket -> TicketCommandDto(
            commandId = request.commandId,
            type = TicketCommandType.CreateComment.ordinal,
            appId = request.appId,
            userId = request.userId,
            params = CommandParamsDto.CreateComment(
                requestNewTicket = true,
                userId = request.userId,
                appId = request.appId,
                comment = request.comment,
                attachments = request.attachments?.map(::mapToAttachmentDataDto),
                ticketId = request.ticketId,
                rating = request.rating,
            ),
        )
        is SyncRequest.Command.MarkTicketAsRead -> TicketCommandDto(
            commandId = request.commandId,
            type = TicketCommandType.MarkTicketAsRead.ordinal,
            appId = request.appId,
            userId = request.userId,
            params = CommandParamsDto.MarkTicketAsRead(
                ticketId = request.ticketId,
                userId = request.userId,
                appId = request.appId,
                commentId = null,
            ),
        )
        is SyncRequest.Command.SetPushToken -> TicketCommandDto(
            commandId = request.commandId,
            type = TicketCommandType.SetPushToken.ordinal,
            appId = request.appId,
            userId = request.userId,
            params = CommandParamsDto.SetPushToken(
                userId = request.userId,
                appId = request.appId,
                token = request.token,
                type = request.tokenType,
            ),
        )
        is SyncRequest.Data -> null
    }

    private fun mapToAttachmentDataDto(attachment: Attachment) = AttachmentDataDto(
        guid = attachment.guid!!,
        type = 0,
        name = attachment.name,
    )

}