package com.pyrus.pyrusservicedesk.sdk.verify

import android.net.Uri
import com.pyrus.pyrusservicedesk._ref.data.Comment
import com.pyrus.pyrusservicedesk.sdk.FileResolver
import com.pyrus.pyrusservicedesk.sdk.sync.TicketCommandDto

internal class LocalDataVerifierImpl(private val fileResolver: FileResolver) : LocalDataVerifier {

    override fun isLocalCommentEmpty(localComment: Comment): Boolean {
        return localComment.body?.isEmpty() == true
                && (localComment.attachments.isNullOrEmpty()
                || !localComment.attachments.any { isLocalFileExists(it.uri) })
                && localComment.rating == null
    }

    override fun isLocalCommandEmpty(localCommand: TicketCommandDto): Boolean {
        return localCommand.commandId.isNotEmpty()
    }

    override fun isLocalFileExists(localFileUri: Uri?): Boolean {
        return fileResolver.isLocalFileExists(localFileUri)
    }
}