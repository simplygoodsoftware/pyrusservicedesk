package com.pyrus.pyrusservicedesk.sdk.web.retrofit

import androidx.annotation.Keep
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.*
import com.pyrus.pyrusservicedesk.sdk.web.request_body.*
import com.pyrus.pyrusservicedesk.utils.Try
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.*

/**
 * Interface that is used for making api calls using [RemoteStore].
 */
@Keep
internal interface ServiceDeskApi {

    /**
     * Api call for getting ticket feed.
     */
    @POST("GetTicketFeed")
    suspend fun getTicketFeed(@Body requestBody: GetFeedBody): Try<Comments>

    /**
     * Api call for getting tikets.
     */
    @POST("gettickets")
    suspend fun getTickets(@Body requestBody: RequestBodyBase) : Try<Tickets>

    /**
     * Api call for sending comment to the feed.
     */
    @POST("UpdateTicketFeed")
    suspend fun addFeedComment(@Body requestBody: AddCommentRequestBody): Try<AddCommentResponseData>

    /**
     * Api call for uploading files.
     */
    @Multipart
    @POST("UploadFile")
    suspend fun uploadFile(@Part file: MultipartBody.Part): Try<FileUploadResponseData>

    /**
     * Api call for registering push token.
     */
    @POST("SetPushToken")
    suspend fun setPushToken(@Body setPushTokenBody: SetPushTokenBody): Try<ResponseBody>
}