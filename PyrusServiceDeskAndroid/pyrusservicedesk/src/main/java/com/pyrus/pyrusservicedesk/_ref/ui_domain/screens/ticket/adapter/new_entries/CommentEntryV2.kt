package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.new_entries

import android.net.Uri
import com.pyrus.pyrusservicedesk.presentation.ui.view.ContentType
import com.pyrus.pyrusservicedesk.presentation.ui.view.Status

internal sealed interface CommentEntryV2 {

    data class Comment(
        val isInbound: Boolean,
        val hasError: Boolean,
        val isLocal: Boolean,
        val isWelcomeMessage: Boolean,
        val timeText: String,
        val status: Status,
        val contentType: ContentType,
        val authorName: String,
        val avatarUrl: String,

        val text: String,

        val attachUrl: Uri?,
        val attachmentName: String,
        val isImage: Boolean,
        val fileSize: Float,
        val fileProgressStatus: Status,
    ) : CommentEntryV2

    data class Buttons(
        val buttons: List<ButtonEntry>,
        val onButtonClick: (text: String) -> Unit,
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
        val hasError: Boolean,
        val isLocal: Boolean,
        val rating: Int,
    ) : CommentEntryV2

    data class WelcomeMessage(val message: String) : CommentEntryV2

    data object SelectRating : CommentEntryV2

}