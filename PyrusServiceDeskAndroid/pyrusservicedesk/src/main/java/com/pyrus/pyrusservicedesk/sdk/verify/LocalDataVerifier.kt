package com.pyrus.pyrusservicedesk.sdk.verify

import com.pyrus.pyrusservicedesk.sdk.data.Comment

/**
 * Checks local data validness
 */
internal interface LocalDataVerifier : LocalFileVerifier {
    /**
     * Checks whether [localComment] is considered empty
     */
    fun isLocalCommentEmpty(localComment: Comment): Boolean
}