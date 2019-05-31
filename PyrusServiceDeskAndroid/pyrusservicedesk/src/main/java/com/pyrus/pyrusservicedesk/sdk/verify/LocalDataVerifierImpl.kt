package com.pyrus.pyrusservicedesk.sdk.verify

import android.net.Uri
import com.pyrus.pyrusservicedesk.sdk.FileResolver
import com.pyrus.pyrusservicedesk.sdk.data.Comment

internal class LocalDataVerifierImpl(private val fileResolver: FileResolver) : LocalDataVerifier {

    override fun isLocalCommentEmpty(localComment: Comment): Boolean {
        return localComment.body.isEmpty()
                && (localComment.attachments.isNullOrEmpty()
                || !localComment.attachments.any { isLocalFileExists(it.localUri) })
    }

    override fun isLocalFileExists(localFileUri: Uri?): Boolean {
        return fileResolver.isLocalFileExists(localFileUri)
    }
}