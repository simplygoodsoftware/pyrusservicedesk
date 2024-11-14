package com.pyrus.pyrusservicedesk.sdk.web.request_body

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.pyrus.pyrusservicedesk.sdk.data.UserData

/**
 * Base request body for sending to the server. Contains fields that are required for almost every request.
 * @param appId id of the app that obtained through special Pyrus form
 * @param userId UID of user. Generated installation id is used by default.
 * @param securityKey used as key for external authorization.
 */
@Keep
internal open class RequestBodyBase(
        @SerializedName("need_full_info")
        val needFullInfo: Boolean? = false,
        @SerializedName("additional_users")
        val additionalUsers: List<UserData>? = null,
////        @SerializedName("commands") //TODO
////        val commands: String?,
//        @SerializedName("author_id")
//        val authorId: String?,
//        @SerializedName("author_name")
//        val authorName: String?,
        @SerializedName("app_id")
        val appId: String,
        @SerializedName("user_id")
        val userId: String,
        @SerializedName("security_key")
        val securityKey: String?,
        @SerializedName("instance_id")
        val instanceId: String?,
        @SerializedName("version")
        val version: Int,
//        @SerializedName("api_sign")
//        val apiSign: String?,
)