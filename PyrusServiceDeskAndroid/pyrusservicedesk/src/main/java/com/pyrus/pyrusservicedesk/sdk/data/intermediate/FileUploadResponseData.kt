package com.pyrus.pyrusservicedesk.sdk.data.intermediate

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * Intermediate data that is used for parsing response of the upload file request.
 */
@Keep
internal class FileUploadResponseData(
    @SerializedName("guid")
    val guid: String,
    @SerializedName("md5_hash")
    val hash: String)