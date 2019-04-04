package net.papirus.pyrusservicedesk.sdk.web.retrofit

import net.papirus.pyrusservicedesk.sdk.data.Ticket
import net.papirus.pyrusservicedesk.sdk.data.intermediate.Comments
import net.papirus.pyrusservicedesk.sdk.data.intermediate.FileUploadResponseData
import net.papirus.pyrusservicedesk.sdk.data.intermediate.Tickets
import net.papirus.pyrusservicedesk.sdk.web.request_body.AddCommentRequestBody
import net.papirus.pyrusservicedesk.sdk.web.request_body.CreateTicketRequestBody
import net.papirus.pyrusservicedesk.sdk.web.request_body.RequestBodyBase
import net.papirus.pyrusservicedesk.sdk.web.request_body.SetPushTokenBody
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

/**
 * Interface that is used for making api calls using [RetrofitWebRepository].
 */
internal interface ServiceDeskApi {

    /**
     * Api call for getting ticket feed.
     */
    @POST("GetTicketFeed")
    fun getTicketFeed(@Body requestBody: RequestBodyBase): Call<Comments>

    /**
     * Api call for getting tikets.
     */
    @POST("gettickets")
    fun getTickets(@Body requestBody: RequestBodyBase)
            : Call<Tickets>

    /**
     * Api call for getting ticket with the given [ticketId].
     */
    @POST("getticket/{ticket_id}")
    fun getTicket(@Body requestBody: RequestBodyBase,
                  @Path("ticket_id") ticketId: Int)
            : Call<Ticket>

    /**
     * Api call for creating ticket.
     */
    @POST("CreateTicket")
    fun createTicket(@Body requestBody: CreateTicketRequestBody): Call<ResponseBody>

    /**
     * Api call for sending comment to the ticket with the given [ticketId].
     */
    @POST("UpdateTicket/{ticket_id}")
    fun addComment(@Body requestBody: AddCommentRequestBody,
                   @Path("ticket_id") ticketId: Int)
            : Call<ResponseBody>

    /**
     * Api call for sending comment to the feed.
     */
    @POST("UpdateTicketFeed")
    fun addFeedComment(@Body requestBody: AddCommentRequestBody): Call<ResponseBody>

    /**
     * Api call for uploading files.
     */
    @Multipart
    @POST("UploadFile")
    fun uploadFile(@Part file: MultipartBody.Part): Call<FileUploadResponseData>

    /**
     * Api call for registering push token.
     */
    @POST("SetPushToken")
    fun setPushToken(@Body setPushTokenBody: SetPushTokenBody): Call<ResponseBody>
}