package com.pyrus.pyrusservicedesk.sdk.verify

import android.net.Uri
import com.pyrus.pyrusservicedesk.sdk.FileResolver
import com.pyrus.pyrusservicedesk.sdk.data.CommentDto

internal class LocalDataVerifierImpl(private val fileResolver: FileResolver) : LocalDataVerifier {

    override fun isLocalCommentEmpty(localComment: CommentDto): Boolean {
        return localComment.body?.isEmpty() == true
                && (localComment.attachments.isNullOrEmpty()
                || !localComment.attachments.any { isLocalFileExists(it.localUri) })
                && localComment.rating == null
    }

    override fun isLocalFileExists(localFileUri: Uri?): Boolean {
        return fileResolver.isLocalFileExists(localFileUri)
    }
}