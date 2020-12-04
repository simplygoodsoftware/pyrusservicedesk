package com.pyrus.pyrusservicedesk.sdk.data

import android.net.Uri
import androidx.annotation.MainThread
import com.pyrus.pyrusservicedesk.sdk.FileResolver
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileData
import com.pyrus.pyrusservicedesk.sdk.repositories.offline.OfflineRepository
import com.pyrus.pyrusservicedesk.utils.ConfigUtils
import kotlinx.coroutines.runBlocking
import java.util.*

/**
 * Provides local instances of data.
 * Also is responsible for converting local instances to the server ones.
 * Each new local comment is guaranteed to have its unique [Comment.localId].
 *
 * @param fileResolver helper for composing local attachment instances.
 */
internal class LocalDataProvider(offlineRepository: OfflineRepository,
                                 private val fileResolver: FileResolver) {

    private var lastLocalCommentId: Int = 0

    private var lastLocalAttachmentId : Int = 0

    init {
        runBlocking {
            // assigns last pending comment id as last local
            lastLocalCommentId = offlineRepository
                .getPendingFeedComments()
                .getData()
                ?.comments
                ?.lastOrNull()
                ?.localId
                ?: 0
        }
    }

    /**
     * Creates local comment instance using given [text] and [fileUri].
     *
     * @return [Comment] instance with [Comment.isLocal] is TRUE.
     */
    @MainThread
    fun createLocalComment(text: String = "", fileUri: Uri? = null, rating: Int? = null): Comment {
        return Comment(
            body = text,
            isInbound = true,
            author = Author(ConfigUtils.getUserName()),
            attachments = fileResolver.getFileData(fileUri)?.let {
                listOf(createLocalAttachment(it))
            },
            creationDate = Calendar.getInstance().time,
            localId = --lastLocalCommentId,
            rating = rating
        )
    }

    /**
     * Convert given [localComment] to the server one using new [serverCommentId].
     * Caller is responsible for checking the relation between the local comment and the id that is passed to
     * this method.
     *
     * NB: If [localComment] contains local attachment, returned comment not equals to the pure server comment
     * as local attachments points to a local file in [Attachment.localUri], and still doesn't have [Attachment.id]
     *
     * @return comment instance with the substituted [serverCommentId]
     */
    fun convertLocalCommentToServer(localComment: Comment, serverCommentId: Int, attachments: List<Attachment>?): Comment {
        return Comment(
            serverCommentId,
            localComment.body,
            localComment.isInbound,
            attachments,
            localComment.creationDate,
            localComment.author,
            localComment.localId,
            localComment.rating
        )
    }

    /**
     * Creates a local attachment with local id.
     *
     * @param uri of the file for attachment.
     */
    fun createLocalAttachmentFromUri(uri: Uri): Attachment? {
        val fileData = fileResolver.getFileData(uri) ?: return null
        return createLocalAttachment(fileData)
    }

    private fun createLocalAttachment(fileData: FileData): Attachment {
        return Attachment(id = --lastLocalAttachmentId, name = fileData.fileName, bytesSize = fileData.bytesSize, localUri = fileData.uri)
    }
}

