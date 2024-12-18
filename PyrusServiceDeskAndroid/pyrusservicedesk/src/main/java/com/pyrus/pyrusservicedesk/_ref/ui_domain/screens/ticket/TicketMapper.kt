package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket

import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.data.Attachment
import com.pyrus.pyrusservicedesk._ref.data.Comment
import com.pyrus.pyrusservicedesk._ref.data.FullTicket
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract.Effect
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract.Message
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract.State
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketView.Event
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketView.Model
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.new_entries.CommentEntryV2
import com.pyrus.pyrusservicedesk._ref.utils.TextProvider
import com.pyrus.pyrusservicedesk._ref.utils.isImage
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.HtmlTagUtils
import com.pyrus.pyrusservicedesk.presentation.ui.view.ContentType
import com.pyrus.pyrusservicedesk.presentation.ui.view.Status

internal object TicketMapper {

    private const val WELCOME_MESSAGE_ID = -2L
    private const val RATING_TEXT_ID = -3L

    fun map(state: State): Model = when(state) {
        is State.Content -> Model(
            inputText = state.inputText,
            sendEnabled = state.sendEnabled,
            comments = state.ticket?.let { mapComments(it, state.welcomeMessage) },
            isLoading = false,
            showNoConnectionError = false,
            isRefreshing = state.isLoading,
        )
        State.Loading -> Model(
            inputText = "",
            sendEnabled = false,
            comments = null,
            isLoading = true,
            showNoConnectionError = false,
            isRefreshing = false,
        )
        State.Error -> Model(
            inputText = "",
            sendEnabled = false,
            comments = null,
            isLoading = false,
            showNoConnectionError = true,
            isRefreshing = false,
        )
    }

    fun map(event: Event): Message.Outer = when(event) {
        is Event.OnAttachmentSelected -> Message.Outer.OnAttachmentSelected(event.fileUri)
        Event.OnCloseClick -> Message.Outer.OnCloseClick
        is Event.OnCopyClick -> Message.Outer.OnCopyClick(event.text)
        is Event.OnMessageChanged -> Message.Outer.OnMessageChanged(event.text)
        is Event.OnPreviewClick -> Message.Outer.OnPreviewClick(event.uri)
        is Event.OnRatingClick -> Message.Outer.OnRatingClick(event.rating)
        is Event.OnRetryClick -> Message.Outer.OnRetryAddCommentClick(event.id)
        Event.OnSendClick -> Message.Outer.OnSendClick
        Event.OnShowAttachVariantsClick -> Message.Outer.OnShowAttachVariantsClick
        Event.OnRefresh -> Message.Outer.OnRefresh
    }

    fun map(effect: Effect.Outer): TicketView.Effect = when(effect) {
        is Effect.Outer.CopyToClipboard -> TicketView.Effect.CopyToClipboard(effect.text)
        is Effect.Outer.MakeToast -> TicketView.Effect.MakeToast(effect.text)
        Effect.Outer.ShowAttachVariants -> TicketView.Effect.ShowAttachVariants
    }

    private fun mapComments(
        freshList: FullTicket,
        welcomeMessage: String?,
    ): List<CommentEntryV2> {
        val entries = ArrayList<CommentEntryV2>()

        if (welcomeMessage != null) addWelcomeEntries(entries, welcomeMessage, freshList)

        addCommentEntries(entries, freshList)

        val entriesWithDates = toListWithDates(entries).toMutableList()

        if (freshList.showRating) {
            if (!freshList.showRatingText.isNullOrBlank()) entriesWithDates += CommentEntryV2.SimpleText(
                entryId = RATING_TEXT_ID,
                message = freshList.showRatingText
            )
            entriesWithDates += CommentEntryV2.RatingSelector
        }

        if (!freshList.showRating) {
            val buttonEntry = freshList.comments.lastOrNull()?.let { extractButtons(it) }
            if (buttonEntry != null) entriesWithDates += buttonEntry
        }

        return entriesWithDates
    }

    private fun addWelcomeEntries(
        entries: ArrayList<CommentEntryV2>,
        welcomeMessage: String,
        freshList: FullTicket
    ) {
        val firstComment = freshList.comments.firstOrNull()
        val creationTime = firstComment?.creationTime ?: System.currentTimeMillis() // TODO

        val welcomeEntry = CommentEntryV2.Comment(
            entryId = WELCOME_MESSAGE_ID.toString(),
            id = WELCOME_MESSAGE_ID,
            isInbound = false,
            hasError = false,
            isLocal = false,
            isWelcomeMessage = true,
            timeText = null,
            status = Status.Completed,
            contentType = ContentType.Text,
            authorName = "",
            avatarUrl = null,
            content = CommentEntryV2.CommentContent.Text(welcomeMessage),
        )
        entries += welcomeEntry
    }

    private fun addCommentEntries(
        entries: ArrayList<CommentEntryV2>,
        freshList: FullTicket,
    ) {
        for (comment in freshList.comments) {
            addCommentEntries(entries, comment)
        }
    }

    private fun addCommentEntries(
        entries: ArrayList<CommentEntryV2>,
        comment: Comment,
    ) {

        val status = when {
            comment.isLocal -> {
                if (comment.isSending) Status.Processing
                else Status.Error
            }
            else -> Status.Completed
        }

        val avatarUrl: String? = null

        val commentBody = comment.body
        if (!commentBody.isNullOrBlank()) {
            entries += toTextEntry(commentBody, comment, avatarUrl, status)
        }

        comment.attachments?.forEach { attach ->
            entries += toAttachEntry(comment, attach, avatarUrl, status)
        }
    }

    private fun toTextEntry(
        commentBody: String,
        comment: Comment,
        avatarUrl: String?,
        status: Status
    ): CommentEntryV2.Comment {
        return CommentEntryV2.Comment(
            entryId = "${comment.id}",
            id = comment.id,
            isInbound = comment.isInbound,
            hasError = false, // TODO
            isLocal = comment.isLocal,
            isWelcomeMessage = false,
            timeText = TextProvider.Date(comment.creationTime, R.string.psd_time_format),
            status = status,
            contentType = ContentType.Text,
            authorName = comment.author.name,
            avatarUrl = avatarUrl,
            content = CommentEntryV2.CommentContent.Text(commentBody),
        )
    }

    private fun toAttachEntry(
        comment: Comment,
        attach: Attachment,
        avatarUrl: String?,
        status: Status,
    ): CommentEntryV2.Comment {

        val contentType = when {
            attach.name.isImage() -> ContentType.PreviewableAttachment
            else -> ContentType.Attachment
        }

        return CommentEntryV2.Comment(
            entryId = "${comment.id}_${attach.id}",
            id = comment.id,
            isInbound = comment.isInbound,
            hasError = false, // TODO
            isLocal = comment.isLocal,
            isWelcomeMessage = false,
            timeText = TextProvider.Date(comment.creationTime, R.string.psd_time_format),
            status = status,
            authorName = comment.author.name,
            avatarUrl = avatarUrl,
            contentType = contentType,
            content = CommentEntryV2.CommentContent.Image(
                attach.uri,
                attach.name,
                attach.name.isImage(),
                attach.bytesSize.toFloat(),
                attach.status,
            ),
        )
    }

    private fun extractButtons(lastComment: Comment): CommentEntryV2.Buttons? {
        val buttons = HtmlTagUtils.extractButtons(lastComment)
        if (buttons.isEmpty()) {
            return null
        }

        return CommentEntryV2.Buttons(lastComment.id, buttons)
    }

    private fun toListWithDates(entries: List<CommentEntryV2>): List<CommentEntryV2> {
        return entries // TODO sds
    }

}