package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.entries

import android.net.Uri
import androidx.annotation.DrawableRes
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.fingerprints.Status
import com.pyrus.pyrusservicedesk._ref.utils.TextProvider

internal sealed interface CommentEntry {

    sealed interface Comment: CommentEntry, WithCreationTime {

        val entryId: String
        val id: Long
        val isInbound: Boolean
        val isSupport: Boolean
        val hasError: Boolean
        val isLocal: Boolean
        val isWelcomeMessage: Boolean
        val timeText: TextProvider?
        val status: Status
        val authorName: TextProvider?
        val authorKey: String?
        val showAuthorName: Boolean
        val avatarUrl: String?
        val showAvatar: Boolean

        data class CommentText(
            override val creationTime: Long,
            override val entryId: String,
            override val id: Long,
            override val isInbound: Boolean,
            override val isSupport: Boolean,
            override val hasError: Boolean,
            override val isLocal: Boolean,
            override val isWelcomeMessage: Boolean,
            override val timeText: TextProvider?,
            override val status: Status,
            override val authorName: TextProvider?,
            override val authorKey: String?,
            override val showAuthorName: Boolean,
            override val avatarUrl: String?,
            override val showAvatar: Boolean,
            val text: String
        ) : Comment

        data class CommentPreviewableAttachment(
            override val creationTime: Long,
            override val entryId: String,
            override val id: Long,
            override val isInbound: Boolean,
            override val isSupport: Boolean,
            override val hasError: Boolean,
            override val isLocal: Boolean,
            override val isWelcomeMessage: Boolean,
            override val timeText: TextProvider?,
            override val status: Status,
            override val authorName: TextProvider?,
            override val authorKey: String?,
            override val showAuthorName: Boolean,
            override val avatarUrl: String?,
            override val showAvatar: Boolean,
            val attach: Attach
        ) : Comment

        data class CommentAttachment(
            override val creationTime: Long,
            override val entryId: String,
            override val id: Long,
            override val isInbound: Boolean,
            override val isSupport: Boolean,
            override val hasError: Boolean,
            override val isLocal: Boolean,
            override val isWelcomeMessage: Boolean,
            override val timeText: TextProvider?,
            override val status: Status,
            override val authorName: TextProvider?,
            override val authorKey: String?,
            override val showAuthorName: Boolean,
            override val avatarUrl: String?,
            override val showAvatar: Boolean,
            val attach: Attach
        ) : Comment

        data class CommentAudio(
            override val creationTime: Long,
            override val entryId: String,
            override val id: Long,
            override val isInbound: Boolean,
            override val isSupport: Boolean,
            override val hasError: Boolean,
            override val isLocal: Boolean,
            override val isWelcomeMessage: Boolean,
            override val timeText: TextProvider?,
            override val status: Status,
            override val authorName: TextProvider?,
            override val authorKey: String?,
            override val showAuthorName: Boolean,
            override val avatarUrl: String?,
            override val showAvatar: Boolean,
            val attach: Attach
        ) : Comment
    }

    data class Attach(
        val attachId: Long,
        val attachUrl: Uri,
        val attachmentName: String,
        val isImage: Boolean,
        val fileSize: Float,
        val fileProgressStatus: Status,
        val uploadProgress: Int?, // 0 - 100
    )

    data class Buttons(
        override val creationTime: Long,
        val id: Long,
        val buttons: List<ButtonEntry>,
    ) : CommentEntry, WithCreationTime

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

    data class Date(val date: TextProvider) : CommentEntry

    data class Rating(
        override val creationTime: Long,
        val id: Long,
        val hasError: Boolean,
        val isLocal: Boolean,
        val rating: Int,
        @DrawableRes val statusIconRes: Int,
        val statusIconIsVisible: Boolean,
    ) : CommentEntry, WithCreationTime

    data class SimpleText(
        override val creationTime: Long,
        val entryId: Long,
        val message: String,
        val avatarUrl: String?,
    ) : CommentEntry, WithCreationTime

    data class RatingSelector(
        override val creationTime: Long,
    ) : CommentEntry, WithCreationTime

    interface WithCreationTime {
        val creationTime: Long
    }

}