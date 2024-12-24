package com.pyrus.pyrusservicedesk.sdk.web.retrofit

import com.pyrus.pyrusservicedesk._ref.utils.Try
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.AddCommentResponseData
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.CommentsDto
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileUploadResponseData
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.TicketsDto
import com.pyrus.pyrusservicedesk.sdk.web.request_body.AddCommentRequestBody
import com.pyrus.pyrusservicedesk.sdk.web.request_body.GetFeedBody
import com.pyrus.pyrusservicedesk.sdk.web.request_body.RequestBodyBase
import com.pyrus.pyrusservicedesk.sdk.web.request_body.SetPushTokenBody
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * Interface that is used for making api calls using [RemoteStore].
 */
internal interface ServiceDeskApi {

    /**
     * Api call for getting ticket feed.
     */
    @POST("GetTicketFeed")
    suspend fun getTicketFeed(@Body requestBody: GetFeedBody): Try<CommentsDto>

    /**
     * Api call for getting tikets.
     */
    @POST("gettickets")
    suspend fun getTickets(@Body requestBody: RequestBodyBase) : Try<TicketsDto>

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