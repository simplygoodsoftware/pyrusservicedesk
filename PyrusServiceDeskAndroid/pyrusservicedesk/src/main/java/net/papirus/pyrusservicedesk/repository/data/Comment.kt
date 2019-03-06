package net.papirus.pyrusservicedesk.repository.data

import com.google.gson.annotations.SerializedName
import net.papirus.pyrusservicedesk.repository.data.intermediate.Attachments
import net.papirus.pyrusservicedesk.utils.parseUtcIsoDate
import java.util.*

internal data class Comment(
        @SerializedName("comment_id")
        val commentId: Int = 0,
        @SerializedName("body")
        val body: String = "",
        @SerializedName("is_inbound")
        val isInbound: Boolean = false,
        @SerializedName("attachments")
        val attachments: Attachments? = null,
        @SerializedName("created_at")
        private val creationDateString: String? = null,
        @SerializedName("author")
        val author: Author){

    fun getCreationDate() = creationDateString?.let { parseUtcIsoDate(creationDateString) }

    fun hasAttachments() = attachments != null
}