package com.pyrus.pyrusservicedesk.sdk.sync

import com.pyrus.pyrusservicedesk._ref.utils.PolymorphicJsonAdapterFactory
import com.pyrus.pyrusservicedesk.sdk.data.AttachmentDataDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

internal sealed interface CommandParamsDto {

    val commandType: CommandsParamsType

    companion object {
        val factory: PolymorphicJsonAdapterFactory<CommandParamsDto> = PolymorphicJsonAdapterFactory
            .of(CommandParamsDto::class.java, "__type")
            .withSubtype(CreateComment::class.java, CommandsParamsType.CreateComment.typeName)
            .withSubtype(MarkTicketAsRead::class.java, CommandsParamsType.MarkTicketAsRead.typeName)
            .withSubtype(SetPushToken::class.java, CommandsParamsType.SetPushToken.typeName)
            .withDefaultJsonAdapter { moshi -> CommandParamsDto_MarkTicketAsReadJsonAdapter(moshi) }
    }

    enum class CommandsParamsType(val typeName: String) {
        CreateComment("CreateComment"),
        MarkTicketAsRead("MarkTicketAsRead"),
        SetPushToken("SetPushToken"),
    }

    /**
     * @param requestNewTicket TRUE if need to create new ticket.
     * @param userId user id.
     * @param appId extension id.
     * @param comment comment text.
     * @param attachments list of attachments in comment.
     * @param ticketId ticket id.
     * @param rating rating given by the user in the ticket.
     */

    @JsonClass(generateAdapter = true)
    data class CreateComment(
        @Json(name = "request_new_ticket") val requestNewTicket: Boolean,
        @Json(name = "user_id") val userId: String,
        @Json(name = "app_id") val appId: String,
        @Json(name = "comment") val comment: String?,
        @Json(name = "attachments") val attachments: List<AttachmentDataDto>?,
        @Json(name = "ticket_id") val ticketId: Long,
        @Json(name = "rating") val rating: Int?
    ) : CommandParamsDto {
        override val commandType = CommandsParamsType.CreateComment
    }

    /**
     * @param commentId id of the last comment read.
     * @param userId user id.
     * @param appId extension id.
     * @param ticketId ticket id.
     */
    @JsonClass(generateAdapter = true)
    data class MarkTicketAsRead(
        @Json(name = "ticket_id") val ticketId: Long,
        @Json(name = "user_id") val userId: String,
        @Json(name = "app_id") val appId: String,
        @Json(name = "comment_id") val commentId: Long?, // readAll if null
    ) : CommandParamsDto  {
        override val commandType = CommandsParamsType.MarkTicketAsRead
    }

    /**
     * @param token if token is null, then we delete the token for the specified user_id in the database.
     * @param userId user id.
     * @param appId extension id.
     * @param type device type (ios or android).
     */
    @JsonClass(generateAdapter = true)
    data class SetPushToken(
        @Json(name = "user_id") val userId: String,
        @Json(name = "app_id") val appId: String,
        @Json(name = "type") val type: String,
        @Json(name = "token") val token: String?, // if null back will remove token from bd
    ) : CommandParamsDto  {
        override val commandType = CommandsParamsType.SetPushToken
    }


}