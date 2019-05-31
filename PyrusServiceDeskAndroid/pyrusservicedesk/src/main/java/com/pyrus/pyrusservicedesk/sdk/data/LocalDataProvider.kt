package com.pyrus.pyrusservicedesk.sdk.data

import android.net.Uri
import android.support.annotation.MainThread
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

    init {
        runBlocking {
            // assigns last pending comment id as last local
            lastLocalCommentId = offlineRepository
                .getPendingFeedComments()
                .getData()
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
    fun createLocalComment(text: String = "", fileUri: Uri? = null): Comment {
        return Comment(
            body = text,
            isInbound = true,
            author = Author(ConfigUtils.getUserName()),
            attachments = fileResolver.getFileData(fileUri)?.let {
                listOf(createLocalAttachment(it))
            },
            creationDate = Calendar.getInstance().time,
            localId = --lastLocalCommentId
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
    fun convertLocalCommentToServer(localComment: Comment, serverCommentId: Int): Comment {
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

    private fun createLocalAttachment(fileData: FileData): Attachment {
        return Attachment(name = fileData.fileName, bytesSize = fileData.bytesSize, localUri = fileData.uri)
    }
}

