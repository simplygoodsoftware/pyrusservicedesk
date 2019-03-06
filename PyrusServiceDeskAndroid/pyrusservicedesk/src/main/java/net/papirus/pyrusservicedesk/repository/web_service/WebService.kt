package net.papirus.pyrusservicedesk.repository.web_service

import android.arch.lifecycle.LiveData
import net.papirus.pyrusservicedesk.repository.data.Ticket
import net.papirus.pyrusservicedesk.repository.web_service.response.*
import net.papirus.pyrusservicedesk.repository.web_service.retrofit.request.*
import java.io.InputStream

internal const val BASE_URL = "https://pyrus.com/servicedeskapi/v1/"

internal interface WebService{
    fun getTickets(request: GetTicketsRequest): LiveData<GetTicketsResponse>
    fun getTicket(request: GetTicketRequest): LiveData<GetTicketResponse>
    fun createTicket(request: CreateTicketRequest): LiveData<CreateTicketResponse>
    fun addComment(request: AddCommentRequest): LiveData<AddCommentResponse>
    fun uploadFile(request: UploadFileRequest): LiveData<UploadFileResponse>
}