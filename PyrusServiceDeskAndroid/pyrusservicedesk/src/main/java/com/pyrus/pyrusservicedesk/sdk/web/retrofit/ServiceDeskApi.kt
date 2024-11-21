package com.pyrus.pyrusservicedesk.sdk.web.retrofit

import androidx.annotation.Keep
import com.pyrus.pyrusservicedesk.sdk.data.Ticket
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.*
import com.pyrus.pyrusservicedesk.sdk.web.request_body.*
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
    fun getTicketFeed(@Body requestBody: GetFeedBody): Call<Comments>

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