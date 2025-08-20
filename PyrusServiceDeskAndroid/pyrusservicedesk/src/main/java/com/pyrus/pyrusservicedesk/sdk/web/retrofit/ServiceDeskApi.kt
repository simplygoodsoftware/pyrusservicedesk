package com.pyrus.pyrusservicedesk.sdk.web.retrofit

import com.pyrus.pyrusservicedesk._ref.utils.Try
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileUploadResponseData
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.TicketsDto
import com.pyrus.pyrusservicedesk.sdk.web.request_body.RequestBodyBase
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * Interface that is used for making api calls using [Synchronizer] and [RemoteFileStore].
 */
internal interface ServiceDeskApi {

    /**
     * Api call for getting tikets.
     */
    @POST("gettickets")
    suspend fun getTickets(@Body requestBody: RequestBodyBase) : Try<TicketsDto>

    /**
     * Api call for uploading files.
     */
    @Multipart
    @POST("UploadFile")
    suspend fun uploadFile(@Part file: MultipartBody.Part): Try<FileUploadResponseData>

}