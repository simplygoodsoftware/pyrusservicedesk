package net.papirus.pyrusservicedesk.repository.data

import com.google.gson.annotations.SerializedName
import net.papirus.pyrusservicedesk.repository.data.intermediate.Attachments
import net.papirus.pyrusservicedesk.utils.parseUtcIsoDate
import java.util.*

internal data class Comment(
        @SerializedName("CommentId")
        val commentId: Int? = null,
        @SerializedName("Body")
        val body: String = "",
        @SerializedName("IsInbound")
        val isInbound: Boolean,
        @SerializedName("Attachments")
        val attachments: Attachments? = null,
        @SerializedName("CreatedAt")
        private val creationDateString: String? = null){

    fun getCreationDate() = creationDateString?.let { parseUtcIsoDate(creationDateString) }

    fun hasAttachments() = attachments != null
}