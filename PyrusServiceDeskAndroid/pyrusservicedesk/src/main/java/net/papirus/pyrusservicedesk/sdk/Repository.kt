package net.papirus.pyrusservicedesk.sdk

import net.papirus.pyrusservicedesk.PyrusServiceDesk
import net.papirus.pyrusservicedesk.sdk.request.AddCommentRequest1
import net.papirus.pyrusservicedesk.sdk.request.CreateTicketRequest1
import net.papirus.pyrusservicedesk.sdk.response.*

internal const val BASE_URL = "https://pyrus.com/servicedeskapi/v1/"

internal interface Repository{
    fun getConversation(): GetConversationResponse1
    fun getTickets(): GetTicketsResponse1
    fun getTicket(ticketId: Int): GetTicketResponse1
    fun addComment(request: AddCommentRequest1): AddCommentResponse1
    fun createTicket(request: CreateTicketRequest1): CreateTicketResponse1
}

internal fun getAvatarUrl(avatarId: Int): String = "$BASE_URL/Avatar/$avatarId"
internal fun getFileUrl(fileId: Int): String {
    return with(PyrusServiceDesk.getInstance()){
        "$BASE_URL/DownloadFile/$fileId?user_id=$clientId&app_id=$appId"
    }
}