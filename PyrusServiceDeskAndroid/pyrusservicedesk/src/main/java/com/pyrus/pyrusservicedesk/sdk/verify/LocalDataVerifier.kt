package com.pyrus.pyrusservicedesk.sdk.verify

import android.net.Uri
import com.pyrus.pyrusservicedesk._ref.data.Comment
import com.pyrus.pyrusservicedesk.sdk.FileResolver
import com.pyrus.pyrusservicedesk.sdk.repositories.CommandEntity
import com.pyrus.pyrusservicedesk.sdk.sync.TicketCommandDto

internal class LocalDataVerifier(private val fileResolver: FileResolver) {

    /**
     * Checks whether [localComment] is considered empty
     */
    fun isLocalCommentEmpty(localComment: Comment): Boolean {
        return localComment.body?.isEmpty() == true
                && (localComment.attachments.isNullOrEmpty()
                || !localComment.attachments.any { isLocalFileExists(it.uri) })
                && localComment.rating == null
    }

    /**
     * Checks whether [localCommand] is considered empty
     */
    fun isLocalCommandEmpty(localCommand: CommandEntity): Boolean {
        return localCommand.commandId.isNotEmpty()
    }

    private fun isLocalFileExists(localFileUri: Uri?): Boolean {
        return fileResolver.isLocalFileExists(localFileUri)
    }
}