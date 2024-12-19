package com.pyrus.pyrusservicedesk._ref.data

import com.google.gson.annotations.SerializedName

internal data class Comment(
    @SerializedName("id") val id: Long,
    @SerializedName("is_local") val isLocal: Boolean,
    @SerializedName("body") val body: String?,
    @SerializedName("is_inbound") val isInbound: Boolean,
    @SerializedName("attachments") val attachments: List<Attachment>?,
    @SerializedName("creation_time") val creationTime: Long,
    @SerializedName("rating") val rating: Int?,
    @SerializedName("author") val author: Author,
    @SerializedName("is_sending") val isSending: Boolean,
)