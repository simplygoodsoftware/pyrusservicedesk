package net.papirus.pyrusservicedesk.repository.web_service.retrofit

import net.papirus.pyrusservicedesk.repository.data.Ticket
import net.papirus.pyrusservicedesk.repository.data.intermediate.FileUploadData
import net.papirus.pyrusservicedesk.repository.data.intermediate.Tickets
import net.papirus.pyrusservicedesk.repository.web_service.response.UploadFileResponse
import net.papirus.pyrusservicedesk.repository.web_service.retrofit.request.body.AddCommentRequestBody
import net.papirus.pyrusservicedesk.repository.web_service.retrofit.request.body.CreateTicketRequestBody
import net.papirus.pyrusservicedesk.repository.web_service.retrofit.request.body.RequestBodyBase
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

internal interface ServiceDeskApi {

    @POST("gettickets")
    fun getTickets(@Body requestBody: RequestBodyBase)
            : Call<Tickets>

    @POST("getticket/{ticketid}")
    fun getTicket(@Body requestBody: RequestBodyBase,
                  @Path("ticketid") ticketId: Int)
            : Call<Ticket>

    @POST("CreateTicket")
    fun createTicket(@Body requestBody: CreateTicketRequestBody): Call<ResponseBody>

    @POST("UpdateTicket")
    fun addComment(@Body requestBody: AddCommentRequestBody): Call<ResponseBody>

    @Multipart
    @POST("UploadFile")
    fun uploadFile(@Part file: MultipartBody.Part): Call<FileUploadData>
}