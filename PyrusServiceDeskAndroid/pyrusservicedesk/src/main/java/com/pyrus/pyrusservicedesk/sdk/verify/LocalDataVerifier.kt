package com.pyrus.pyrusservicedesk.sdk.verify

import com.pyrus.pyrusservicedesk._ref.data.Comment
import com.pyrus.pyrusservicedesk.sdk.sync.TicketCommandDto

/**
 * Checks local data validness
 */
internal interface LocalDataVerifier : LocalFileVerifier {
    /**
     * Checks whether [localComment] is considered empty
     */
    fun isLocalCommentEmpty(localComment: Comment): Boolean

    /**
     * Checks whether [localCommand] is considered empty
     */
    fun isLocalCommandEmpty(localComment: TicketCommandDto): Boolean
}