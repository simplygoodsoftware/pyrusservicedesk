package net.papirus.pyrusservicedesk.sdk.web_service

import android.arch.lifecycle.LiveData
import net.papirus.pyrusservicedesk.PyrusServiceDesk
import net.papirus.pyrusservicedesk.sdk.web_service.response.*
import net.papirus.pyrusservicedesk.sdk.web_service.retrofit.request.*

internal const val BASE_URL = "https://pyrus.com/servicedeskapi/v1/"

internal interface WebService{
    fun getConversation(request: RequestBase): LiveData<GetConversationResponse>
    fun getTickets(request: RequestBase): LiveData<GetTicketsResponse>
    fun getTicket(request: GetTicketRequest): LiveData<GetTicketResponse>
    fun createTicket(request: CreateTicketRequest): LiveData<CreateTicketResponse>
    fun addComment(request: AddCommentRequest): LiveData<AddCommentResponse>
    fun uploadFile(request: UploadFileRequest): LiveData<UploadFileResponse>
}

internal fun getAvatarUrl(avatarId: Int): String = "$BASE_URL/Avatar/$avatarId"
internal fun getFileUrl(fileId: Int): String {
  return with(PyrusServiceDesk.getInstance()){
      "$BASE_URL/DownloadFile/$fileId?user_id=$clientId&app_id=$appId"
  }
}
