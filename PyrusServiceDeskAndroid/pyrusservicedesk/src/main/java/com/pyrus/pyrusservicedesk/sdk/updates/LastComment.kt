package com.pyrus.pyrusservicedesk.sdk.updates

import com.pyrus.pyrusservicedesk.sdk.data.Comment
import com.pyrus.pyrusservicedesk.utils.getFirstNSymbols

internal data class LastComment(
    val id: Int,
    val isRead: Boolean,
    val isShown: Boolean,
    val text: String?,
    val attaches: List<String>?,
    val attachesCount: Int,
    val utcTime: Long
) {
    companion object {
        internal fun mapFromComment(isShown: Boolean, isRead: Boolean, comment: Comment): LastComment {
            return LastComment(
                comment.commentId,
                isRead,
                isShown,
                trimCommentText(comment.body),
                comment.attachments?.subList(0, 10)?.map { attachment -> attachment.name },
                comment.attachments?.size ?: 0,
                comment.creationDate.time
            )
        }

        private fun trimCommentText(text: String?): String? {
            if (text == null)
                return null
            val moreThanMax = text.length > 500
            val result = StringBuilder(text.getFirstNSymbols(500 - if (moreThanMax) 1 else 0))
            if (moreThanMax)
                result.append('…')
            return result.toString()
        }

    }
}