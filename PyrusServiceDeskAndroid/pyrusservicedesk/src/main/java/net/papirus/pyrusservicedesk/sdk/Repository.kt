package net.papirus.pyrusservicedesk.sdk

import net.papirus.pyrusservicedesk.PyrusServiceDesk
import net.papirus.pyrusservicedesk.sdk.data.Comment
import net.papirus.pyrusservicedesk.sdk.data.TicketDescription
import net.papirus.pyrusservicedesk.sdk.response.*
import net.papirus.pyrusservicedesk.sdk.web.UploadFileHooks

internal const val BASE_URL = "https://pyrus.com/servicedeskapi/v1/"

internal interface Repository{
    suspend fun getConversation(): GetConversationResponse
    suspend fun getTickets(): GetTicketsResponse
    suspend fun getTicket(ticketId: Int): GetTicketResponse
    suspend fun addComment(ticketId: Int, comment: Comment, uploadFileHooks: UploadFileHooks): AddCommentResponse
    suspend fun createTicket(description: TicketDescription, uploadFileHooks: UploadFileHooks): CreateTicketResponse
}

internal fun getAvatarUrl(avatarId: Int): String = "$BASE_URL/Avatar/$avatarId"
internal fun getFileUrl(fileId: Int): String {
    return with(PyrusServiceDesk.getInstance()){
        "$BASE_URL/DownloadFile/$fileId?user_id=$clientId&app_id=$appId"
    }
}