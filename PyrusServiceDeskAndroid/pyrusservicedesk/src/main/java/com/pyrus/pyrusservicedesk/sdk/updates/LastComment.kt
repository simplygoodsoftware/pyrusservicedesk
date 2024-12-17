package com.pyrus.pyrusservicedesk.sdk.updates

import com.pyrus.pyrusservicedesk.sdk.data.CommentDto
import com.pyrus.pyrusservicedesk._ref.utils.getFirstNSymbols

internal data class LastComment(
    val id: Long,
    val isRead: Boolean,
    val isShown: Boolean,
    val text: String?,
    val attaches: List<String>?,
    val attachesCount: Int,
    val utcTime: Long
) {
    companion object {

        private const val MAX_COMMENT_LENGTH = 500

        internal fun mapFromComment(isShown: Boolean, isRead: Boolean, comment: CommentDto): LastComment {
            return LastComment(
                comment.commentId,
                isRead,
                isShown,
                trimCommentText(comment.body),
                getFirstTenElements(comment.attachments)?.map { attachment -> attachment.name },
                comment.attachments?.size ?: 0,
                comment.creationDate.time
            )
        }

        private fun <E> getFirstTenElements(list: List<E>?): List<E>? = when {
            list == null -> null
            list.size < 10 -> list.toMutableList()
            else -> list.subList(0, 10)
        }

        private fun trimCommentText(text: String?): String? {
            return text?.getFirstNSymbols(MAX_COMMENT_LENGTH)
        }

    }
}