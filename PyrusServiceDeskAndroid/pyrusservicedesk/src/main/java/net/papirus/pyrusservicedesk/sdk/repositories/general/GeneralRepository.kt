package net.papirus.pyrusservicedesk.sdk.repositories.general

import net.papirus.pyrusservicedesk.PyrusServiceDesk
import net.papirus.pyrusservicedesk.sdk.data.Comment
import net.papirus.pyrusservicedesk.sdk.data.TicketDescription
import net.papirus.pyrusservicedesk.sdk.response.*
import net.papirus.pyrusservicedesk.sdk.web.UploadFileHooks
import java.net.URLEncoder

internal const val BASE_URL = "https://pyrus.com/servicedeskapi/v1/"

internal interface GeneralRepository{
    suspend fun getFeed(): GetFeedResponse
    suspend fun getTickets(): GetTicketsResponse
    suspend fun getTicket(ticketId: Int): GetTicketResponse
    suspend fun addComment(ticketId: Int, comment: Comment, uploadFileHooks: UploadFileHooks? = null): AddCommentResponse
    suspend fun addFeedComment(comment: Comment, uploadFileHooks: UploadFileHooks? = null): AddCommentResponse
    suspend fun createTicket(description: TicketDescription, uploadFileHooks: UploadFileHooks? = null): CreateTicketResponse
}

internal fun getAvatarUrl(avatarId: Int): String = "$BASE_URL/Avatar/$avatarId"
internal fun getFileUrl(fileId: Int): String {
    return with(PyrusServiceDesk.getInstance()){
        "$BASE_URL/DownloadFile/$fileId" +
                "?user_id=" +
                URLEncoder.encode(userId, "UTF-8") +
                "&app_id=" +
                URLEncoder.encode(appId, "UTF-8")
    }
}