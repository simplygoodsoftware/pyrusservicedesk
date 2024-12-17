package com.pyrus.pyrusservicedesk.sdk.verify

import com.pyrus.pyrusservicedesk.sdk.data.CommentDto

/**
 * Checks local data validness
 */
internal interface LocalDataVerifier : LocalFileVerifier {
    /**
     * Checks whether [localComment] is considered empty
     */
    fun isLocalCommentEmpty(localComment: CommentDto): Boolean
}