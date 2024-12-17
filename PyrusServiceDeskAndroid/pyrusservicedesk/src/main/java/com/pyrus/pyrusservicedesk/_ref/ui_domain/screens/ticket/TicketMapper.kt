package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket

import android.net.Uri
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract.Effect
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract.Message
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract.State
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketView.Event
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketView.Model
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.new_entries.CommentEntryV2
import com.pyrus.pyrusservicedesk._ref.utils.RequestUtils
import com.pyrus.pyrusservicedesk._ref.utils.isImage
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.HtmlTagUtils
import com.pyrus.pyrusservicedesk.presentation.ui.view.ContentType
import com.pyrus.pyrusservicedesk.presentation.ui.view.Status
import com.pyrus.pyrusservicedesk.sdk.data.AttachmentDto
import com.pyrus.pyrusservicedesk.sdk.data.CommentDto
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.Comments

internal object TicketMapper {

    private const val WELCOME_MESSAGE_ID = -2L
    private const val RATING_TEXT_ID = -3L

    fun map(state: State): Model = when(state) {
        is State.Content -> Model(
            inputText = state.inputText,
            sendEnabled = state.sendEnabled,
            comments = state.comments?.let { mapComments(it, state.welcomeMessage) },
            isLoading = false,
            showNoConnectionError = false,
        )
        State.Loading -> Model(
            inputText = "",
            sendEnabled = false,
            comments = null,
            isLoading = true,
            showNoConnectionError = false,
        )
        State.Error -> Model(
            inputText = "",
            sendEnabled = false,
            comments = null,
            isLoading = false,
            showNoConnectionError = true,
        )
    }

    fun map(event: Event): Message.Outer = when(event) {
        is Event.OnAttachmentSelected -> Message.Outer.OnAttachmentSelected(event.fileUri)
        Event.OnCloseClick -> Message.Outer.OnCloseClick
        is Event.OnCopyClick -> Message.Outer.OnCopyClick(event.text)
        is Event.OnMessageChanged -> Message.Outer.OnMessageChanged(event.text)
        is Event.OnPreviewClick -> Message.Outer.OnPreviewClick(event.uri)
        is Event.OnRatingClick -> Message.Outer.OnRatingClick(event.rating)
        is Event.OnRetryClick -> Message.Outer.OnRetryClick(event.id)
        Event.OnSendClick -> Message.Outer.OnSendClick
        Event.OnShowAttachVariantsClick -> Message.Outer.OnShowAttachVariantsClick
    }

    fun map(effect: Effect.Outer) = when(effect) {
        is Effect.Outer.CopyToClipboard -> TicketView.Effect.CopyToClipboard(effect.text)
        is Effect.Outer.MakeToast -> TicketView.Effect.MakeToast(effect.text)
    }

    private fun mapComments(
        freshList: Comments,
        welcomeMessage: String?,
    ): List<CommentEntryV2> {
        val entries = ArrayList<CommentEntryV2>()

        if (welcomeMessage != null) addWelcomeEntries(entries, welcomeMessage, freshList)

        addCommentEntries(entries, freshList)

        val entriesWithDates = toListWithDates(entries).toMutableList()

        if (freshList.showRating) {
            if (freshList.showRatingText.isNotBlank()) entriesWithDates += CommentEntryV2.SimpleText(
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
        freshList: Comments
    ) {
        val firstComment = freshList.comments.firstOrNull()
        val welcomeCreationTime = firstComment?.creationDate?.time ?: System.currentTimeMillis()

        val welcomeEntry = CommentEntryV2.Comment(
            entryId = WELCOME_MESSAGE_ID.toString(),
            id = WELCOME_MESSAGE_ID,
            isInbound = false,
            hasError = false,
            isLocal = false,
            isWelcomeMessage = true,
            timeText = "Time ", // TODO sds
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
        freshList: Comments,
    ) {
        for (comment in freshList.comments) {
            addCommentEntries(entries, comment, "") // TODO
        }
    }

    private fun addCommentEntries(
        entries: ArrayList<CommentEntryV2>,
        comment: CommentDto,
        baseUrl: String,
    ) {

        // TODO
//        val avatarId = comment.author.avatarId
//        if (avatarId == 0) {
//
//        }
//        val avatarUrl = "${baseUrl}Avatar/${comment.author.avatarId}"


        val status = when {
            comment.isLocal() -> {
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
        comment: CommentDto,
        avatarUrl: String?,
        status: Status
    ): CommentEntryV2.Comment {
        val isLocal = comment.isLocal()
        return CommentEntryV2.Comment(
            entryId = "${comment.commentId}",
            id = comment.commentId,
            isInbound = comment.isInbound,
            hasError = false, // TODO
            isLocal = isLocal,
            isWelcomeMessage = false,
            timeText = "time ", // TODO
            status = status,
            contentType = ContentType.Text,
            authorName = comment.author.name,
            avatarUrl = avatarUrl,
            content = CommentEntryV2.CommentContent.Text(commentBody),
        )
    }

    private fun toAttachEntry(
        comment: CommentDto,
        attach: AttachmentDto,
        avatarUrl: String?,
        status: Status,
    ): CommentEntryV2.Comment {

        val contentType = when {
            attach.name.isImage() -> ContentType.PreviewableAttachment
            else -> ContentType.Attachment
        }

        val attachUri = attach.localUri ?: Uri.parse(RequestUtils.getPreviewUrl(attach.id, account))

        return CommentEntryV2.Comment(
            entryId = "${comment.commentId}_${attach.id}",
            id = comment.commentId,
            isInbound = comment.isInbound,
            hasError = false, // TODO
            isLocal = comment.isLocal(),
            isWelcomeMessage = false,
            timeText = "time", // TODO
            status = status,
            authorName = comment.author.name,
            avatarUrl = avatarUrl,
            contentType = contentType,
            content = CommentEntryV2.CommentContent.Image(
                attachUri,
                attach.name,
                attach.name.isImage(),
                attach.bytesSize.toFloat(),
                attach.status,
            ),
        )
    }

    private fun extractButtons(lastComment: CommentDto): CommentEntryV2.Buttons? {
        val buttons = HtmlTagUtils.extractButtons(lastComment)
        if (buttons.isEmpty()) {
            return null
        }

        return CommentEntryV2.Buttons(lastComment.commentId, buttons)
    }

    private fun toListWithDates(entries: List<CommentEntryV2>): List<CommentEntryV2> {
        return entries // TODO sds
    }

}