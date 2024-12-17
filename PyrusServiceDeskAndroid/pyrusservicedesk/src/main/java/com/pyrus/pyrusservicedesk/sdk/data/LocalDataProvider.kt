package com.pyrus.pyrusservicedesk.sdk.data

import android.net.Uri
import androidx.annotation.MainThread
import com.pyrus.pyrusservicedesk.sdk.FileResolver
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileData
import com.pyrus.pyrusservicedesk.sdk.repositories.LocalStore
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import kotlinx.coroutines.runBlocking
import java.util.*

/**
 * Provides local instances of data.
 * Also is responsible for converting local instances to the server ones.
 * Each new local comment is guaranteed to have its unique [CommentDto.localId].
 *
 * @param fileResolver helper for composing local attachment instances.
 */
internal class LocalDataProvider(
    offlineRepository: LocalStore,
    private val fileResolver: FileResolver,
) {

    private var lastLocalCommentId: Long = 0

    init {
        runBlocking {
            // assigns last pending comment id as last local
            lastLocalCommentId = offlineRepository.getPendingFeedComments().lastOrNull()?.id ?: 0L
        }
    }

    /**
     * Creates local comment instance using given [text] and [fileUri].
     *
     * @return [CommentDto] instance with [CommentDto.isLocal] is TRUE.
     */
    @MainThread
    fun createLocalComment(text: String = "", fileUri: Uri? = null, rating: Int? = null): CommentDto {
        return CommentDto(
            body = text,
            isInbound = true,
            author = AuthorDto(ConfigUtils.getUserName(), null, null),
            attachments = fileResolver.getFileData(fileUri)?.let { fileData ->
                listOf(createLocalAttachment(fileData))
            },
            creationDate = Calendar.getInstance().time,
//            localId = --lastLocalCommentId,
            rating = rating
        )
    }

    /**
     * Convert given [localComment] to the server one using new [serverCommentId].
     * Caller is responsible for checking the relation between the local comment and the id that is passed to
     * this method.
     *
     * NB: If [localComment] contains local attachment, returned comment not equals to the pure server comment
     * as local attachments points to a local file in [AttachmentDto.localUri], and still doesn't have [AttachmentDto.id]
     *
     * @return comment instance with the substituted [serverCommentId]
     */
    fun convertLocalCommentToServer(localComment: CommentDto, serverCommentId: Long, attachments: List<AttachmentDto>?): CommentDto {
        return CommentDto(
            serverCommentId,
            localComment.body,
            localComment.isInbound,
            attachments,
            localComment.creationDate,
            localComment.author,
//            localComment.localId,
            localComment.rating
        )
    }

    private fun createLocalAttachment(fileData: FileData): AttachmentDto {
        return AttachmentDto(name = fileData.fileName, bytesSize = fileData.bytesSize, localUri = fileData.uri)
    }
}

