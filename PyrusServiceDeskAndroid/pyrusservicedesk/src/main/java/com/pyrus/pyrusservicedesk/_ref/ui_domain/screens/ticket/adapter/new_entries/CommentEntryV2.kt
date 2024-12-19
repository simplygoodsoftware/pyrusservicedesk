package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.new_entries

import android.net.Uri
import com.pyrus.pyrusservicedesk._ref.utils.TextProvider
import com.pyrus.pyrusservicedesk.presentation.ui.view.ContentType
import com.pyrus.pyrusservicedesk.presentation.ui.view.Status

internal sealed interface CommentEntryV2 {

    data class Comment(
        val entryId: String,
        val id: Long,
        val isInbound: Boolean,
        val hasError: Boolean,
        val isLocal: Boolean,
        val isWelcomeMessage: Boolean,
        val timeText: TextProvider?,
        val status: Status,
        val authorName: String,
        val avatarUrl: String?,

        val contentType: ContentType,
        val content: CommentContent,
    ) : CommentEntryV2

    sealed interface CommentContent {
        data class Text(val text: String) : CommentContent
        data class Image(
            val attachUrl: Uri,
            val attachmentName: String,
            val isImage: Boolean,
            val fileSize: Float,
            val fileProgressStatus: Status,
        ) : CommentContent
    }

    data class Buttons(
        val id: Long,
        val buttons: List<ButtonEntry>,
    ) : CommentEntryV2

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

    data class Date(val date: String) : CommentEntryV2

    data class Rating(
        val id: Long,
        val hasError: Boolean,
        val isLocal: Boolean,
        val rating: Int,
    ) : CommentEntryV2

    data class SimpleText(val entryId: Long, val message: String) : CommentEntryV2

    data object RatingSelector : CommentEntryV2

}