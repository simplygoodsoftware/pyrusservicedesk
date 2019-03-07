package net.papirus.pyrusservicedesk.repository.data

import com.google.gson.annotations.SerializedName
import net.papirus.pyrusservicedesk.utils.parseUtcIsoDate
import java.util.*

internal data class Comment(
        @SerializedName("comment_id")
        val commentId: Int = 0,
        @SerializedName("body")
        val body: String = "",
        // inbound for support, not for user
        @SerializedName("is_inbound")
        val isInbound: Boolean = false,
        @SerializedName("attachments")
        val attachments: List<Attachment>? = null,
        @SerializedName("created_at")
        private val creationDateString: String,
        @SerializedName("author")
        val author: Author){

    private var creationDate: Calendar? = null

    // TODO should be made by custom deserializer
    fun getCreationDate(): Calendar {
        if (creationDate == null)
            creationDate = parseUtcIsoDate(creationDateString)
        return creationDate!!
    }

    fun hasAttachments() = attachments != null
}