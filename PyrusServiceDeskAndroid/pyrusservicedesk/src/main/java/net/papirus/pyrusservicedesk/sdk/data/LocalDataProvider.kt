package net.papirus.pyrusservicedesk.sdk.data

import android.net.Uri
import net.papirus.pyrusservicedesk.sdk.FileResolver
import net.papirus.pyrusservicedesk.utils.ConfigUtils
import java.util.*
internal class LocalDataProvider(initialLocalCommentId: Int = -1,
                                 private val fileResolver: FileResolver) {

    private var lastLocalCommentId = initialLocalCommentId

    fun newLocalComment(text: String = "", fileUri: Uri? = null): Comment {
        return Comment(
            body = text,
            isInbound = true,
            author = Author(ConfigUtils.getUserName()),
            attachments = fileResolver.getFileData(fileUri)?.let {
                listOf(newLocalAttachment(it.fileName, it.bytesSize, it.uri))
            },
            creationDate = Calendar.getInstance().time,
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

    private fun newLocalAttachment(fileName: String, fileSize: Int, fileUri: Uri): Attachment {
        return Attachment(name = fileName, bytesSize = fileSize, uri = fileUri)
    }
}

