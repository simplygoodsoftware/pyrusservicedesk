package net.papirus.pyrusservicedesk.sdk.data

import android.net.Uri
import java.util.*
internal class LocalDataProvider(
    clientName: String,
    initialLocalCommentId: Int = -1) {

    private val me = Author(clientName)
    private var lastLocalCommentId = initialLocalCommentId

    fun newLocalTextComment(text: String = ""): Comment {
        return Comment(
            body = text,
            isInbound = true,
            author = me,
            creationDate = Calendar.getInstance().time,
            localId = --lastLocalCommentId
        )
    }

    fun newLocalAttachmentComment(fileName: String, fileSize: Int, fileUri: Uri): Comment {
        return Comment(
            isInbound = true,
            author = me,
            attachments = listOf(newLocalAttachment(fileName, fileSize, fileUri)),
            creationDate = Calendar . getInstance ().time,
            localId = --lastLocalCommentId
        )
    }

    fun localToServerComment(localComment: Comment, serverCommentId: Int): Comment {
        return Comment(
            serverCommentId,
            localComment.body,
            localComment.isInbound,
            localComment.attachments,
            localComment.creationDate,
            localComment.author,
            localComment.localId
        )
    }

    fun updateLocalToServerAttachment(localComment: Comment, guid: String): Comment {
        return Comment(
            isInbound = true,
            author = me,
            attachments = listOf(
                Attachment(
                    name = localComment.attachments?.get(0)?.name ?: "",
                    bytesSize = localComment.attachments?.get(0)?.bytesSize ?: 0,
                    guid = guid)),
            creationDate = Calendar . getInstance ().time,
            localId = localComment.localId
        )
    }

    private fun newLocalAttachment(fileName: String, fileSize: Int, fileUri: Uri): Attachment {
        return Attachment(name = fileName, bytesSize = fileSize, uri = fileUri)
    }
}
