package net.papirus.pyrusservicedesk.sdk.web.retrofit

import net.papirus.pyrusservicedesk.sdk.data.Ticket
import net.papirus.pyrusservicedesk.sdk.data.intermediate.Comments
import net.papirus.pyrusservicedesk.sdk.data.intermediate.FileUploadResponseData
import net.papirus.pyrusservicedesk.sdk.data.intermediate.Tickets
import net.papirus.pyrusservicedesk.sdk.web.request_body.AddCommentRequestBody
import net.papirus.pyrusservicedesk.sdk.web.request_body.CreateTicketRequestBody
import net.papirus.pyrusservicedesk.sdk.web.request_body.RequestBodyBase
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

internal interface ServiceDeskApi {

    @POST("getfeed")
    fun getConversation(@Body requestBody: RequestBodyBase): Call<Comments>

    @POST("gettickets")
    fun getTickets(@Body requestBody: RequestBodyBase)
            : Call<Tickets>

    @POST("getticket/{ticket_id}")
    fun getTicket(@Body requestBody: RequestBodyBase,
                  @Path("ticket_id") ticketId: Int)
            : Call<Ticket>

    @POST("CreateTicket")
    fun createTicket(@Body requestBody: CreateTicketRequestBody): Call<ResponseBody>

    @POST("UpdateTicket/{ticket_id}")
    fun addComment(@Body requestBody: AddCommentRequestBody,
                   @Path("ticket_id") ticketId: Int)
            : Call<ResponseBody>

    @Multipart
    @POST("UploadFile")
    fun uploadFile(@Part file: MultipartBody.Part): Call<FileUploadResponseData>
}