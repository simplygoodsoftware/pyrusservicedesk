package com.pyrus.pyrusservicedesk.sdk.web.retrofit

import androidx.annotation.Keep
import com.pyrus.pyrusservicedesk.sdk.data.Ticket
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.*
import com.pyrus.pyrusservicedesk.sdk.web.request_body.AddCommentRequestBody
import com.pyrus.pyrusservicedesk.sdk.web.request_body.CreateTicketRequestBody
import com.pyrus.pyrusservicedesk.sdk.web.request_body.RequestBodyBase
import com.pyrus.pyrusservicedesk.sdk.web.request_body.SetPushTokenBody
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

/**
 * Interface that is used for making api calls using [RetrofitWebRepository].
 */
@Keep
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
    fun createTicket(@Body requestBody: CreateTicketRequestBody): Call<CreateTicketResponseData>

    /**
     * Api call for sending comment to the ticket with the given [ticketId].
     */
    @POST("UpdateTicket/{ticket_id}")
    fun addComment(@Body requestBody: AddCommentRequestBody,
                   @Path("ticket_id") ticketId: Int)
            : Call<AddCommentResponseData>

    /**
     * Api call for sending comment to the feed.
     */
    @POST("UpdateTicketFeed")
    fun addFeedComment(@Body requestBody: AddCommentRequestBody): Call<AddCommentResponseData>

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