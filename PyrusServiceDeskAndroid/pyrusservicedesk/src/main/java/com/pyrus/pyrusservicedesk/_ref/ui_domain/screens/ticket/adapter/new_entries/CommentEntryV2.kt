package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.new_entries

import android.net.Uri
import com.pyrus.pyrusservicedesk._ref.utils.TextProvider
import com.pyrus.pyrusservicedesk.presentation.ui.view.ContentType
import com.pyrus.pyrusservicedesk.presentation.ui.view.Status

internal sealed interface CommentEntryV2 {

    data class Comment(
        override val creationTime: Long,
        val entryId: String,
        val id: Long,
        val isInbound: Boolean,
        val hasError: Boolean,
        val isLocal: Boolean,
        val isWelcomeMessage: Boolean,
        val timeText: TextProvider?,
        val status: Status,
        val authorName: String,
        val showAuthorName: Boolean,
        val avatarUrl: String?,
        val showAvatar: Boolean,

        val contentType: ContentType,
        val content: CommentContent,
    ) : CommentEntryV2, WithCreationTime

    sealed interface CommentContent {
        data class Text(val text: String) : CommentContent
        data class Image(
            val attachId: Int,
            val attachUrl: Uri,
            val attachmentName: String,
            val isImage: Boolean,
            val fileSize: Float,
            val fileProgressStatus: Status,
            val uploadProgress: Int?, // 0 - 100
        ) : CommentContent
    }

    data class Buttons(
        override val creationTime: Long,
        val id: Long,
        val buttons: List<ButtonEntry>,
    ) : CommentEntryV2, WithCreationTime

    sealed interface ButtonEntry {

        val text: String

        data class Simple(
            override val text: String,
        ) : ButtonEntry

        data class Link(
            override val text: String,
            val link: String,
        ) : ButtonEntry

    }

    data class Date(val date: TextProvider) : CommentEntryV2

    data class Rating(
        override val creationTime: Long,
        val id: Long,
        val hasError: Boolean,
        val isLocal: Boolean,
        val rating: Int,
    ) : CommentEntryV2, WithCreationTime

    data class SimpleText(
        override val creationTime: Long,
        val entryId: Long,
        val message: String,
    ) : CommentEntryV2, WithCreationTime

    data class RatingSelector(
        override val creationTime: Long,
    ) : CommentEntryV2, WithCreationTime

    interface WithCreationTime {
        val creationTime: Long
    }

}