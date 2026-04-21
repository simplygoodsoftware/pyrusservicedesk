package com.pyrus.pyrusservicedesk.sdk.sync

import com.pyrus.pyrusservicedesk.AppResourceManager
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk.core.Account
import com.pyrus.pyrusservicedesk.core.getAdditionalUsers
import com.pyrus.pyrusservicedesk.core.getAuthorId
import com.pyrus.pyrusservicedesk.core.getInstanceId
import com.pyrus.pyrusservicedesk.core.getSecurityKey
import com.pyrus.pyrusservicedesk.core.getVersion
import com.pyrus.pyrusservicedesk.sdk.data.AttachmentDataDto
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.TicketEntity
import com.pyrus.pyrusservicedesk.sdk.web.request_body.RequestBodyBase
import java.util.Locale
import kotlin.math.max

internal object SyncMapper {

    private const val API_FLAG = "AAAAAAAAAAAUAAs="

    fun mapToGetFeedRequest(
        syncRequests: List<SyncReqRes>,
        tickets: List<TicketEntity>,
        account: Account,
        resourceManager: AppResourceManager,
        firstUserId: String,
        firstAppId: String,
    ): RequestBodyBase {

        val currentLocale: Locale = Locale.getDefault()
        val language = currentLocale.language
        val country = currentLocale.country
        val locale = if (language.isNullOrBlank()) "ru" else
            if (country.isNullOrBlank()) language else "$language-$country"

        val request = RequestBodyBase(
            needFullInfo = true,
            additionalUsers = account.getAdditionalUsers(tickets),
            lastNoteId = calcLastNoteId(tickets, firstUserId),
            commands = syncRequests.mapNotNull { mapToCommand(it.request, account.getInstanceId()) },
            authorId = account.getAuthorId(),
            authorName = ConfigUtils.getAuthorName(resourceManager),
            appId = firstAppId,
            userId = if (firstUserId == account.getInstanceId()) null else firstUserId,
            securityKey = account.getSecurityKey(),
            instanceId = account.getInstanceId(),
            version = account.getVersion(),
            apiSign = API_FLAG,
            locale = locale,
        )
        return request
    }

    fun calcLastNoteId(tickets: List<TicketEntity>, userId: String): Long? {
        val lastNoteId: Long?

        var maxNoteId: Long = 0
        for (ticket in tickets) {
            val lastCommentId = ticket.lastComment?.commentId
            if (lastCommentId != null && ticket.userId == userId) {
                maxNoteId = max(lastCommentId, maxNoteId)
            }
        }
        lastNoteId = if (maxNoteId <= 0L) null else maxNoteId
        return lastNoteId
    }

    private fun mapToCommand(request: SyncRequest, instanceId: String): TicketCommandDto? = when(request) {
        is SyncRequest.Command.CreateComment -> TicketCommandDto(
            commandId = request.commandId,
            type = TicketCommandType.CreateComment.ordinal,
            appId = request.appId,
            userId = if (request.userId != instanceId) request.userId else null,
            params = CommandParamsDto.CreateComment(
                requestNewTicket = request.requestNewTicket,
                userId = if (request.userId != instanceId) request.userId else null,
                appId = request.appId,
                comment = request.comment,
                attachments = request.attachments?.map { AttachmentDataDto(it.guid!!, 0, it.name) },
                ticketId = request.ticketId,
                rating = request.rating,
                ratingComment = request.ratingComment,
                extraFields = request.extraFields,
            ),
        )
        is SyncRequest.Command.MarkTicketAsRead -> TicketCommandDto(
            commandId = request.commandId,
            type = TicketCommandType.MarkTicketAsRead.ordinal,
            appId = request.appId,
            userId = if (request.userId != instanceId) request.userId else null,
            params = CommandParamsDto.MarkTicketAsRead(
                ticketId = request.ticketId,
                userId = if (request.userId != instanceId) request.userId else null,
                appId = request.appId,
                commentId = null,
            ),
        )
        is SyncRequest.Command.SetPushToken -> TicketCommandDto(
            commandId = request.commandId,
            type = TicketCommandType.SetPushToken.ordinal,
            appId = request.appId,
            userId = if (request.userId != instanceId) request.userId else null,
            params = CommandParamsDto.SetPushToken(
                userId = if (request.userId != instanceId) request.userId else null,
                appId = request.appId,
                token = request.token,
                type = request.tokenType,
            ),
        )
        is SyncRequest.Command.CalcOperatorTime -> TicketCommandDto(
            commandId = request.commandId,
            type = TicketCommandType.CalcOperatorTime.ordinal,
            appId = request.appId,
            userId = request.userId,
            params = CommandParamsDto.CalcOperatorTime(
                ticketId = request.ticketId,
                userId = request.userId,
                appId = request.appId,
            )
        )
        is SyncRequest.Data -> null
    }

}