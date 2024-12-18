package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket

import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract.Effect
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract.Message
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract.State
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketView.Event
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketView.Model
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.new_entries.CommentEntryV2
import com.pyrus.pyrusservicedesk._ref.utils.BYTES_IN_MEGABYTE
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.HtmlTagUtils
import com.pyrus.pyrusservicedesk.presentation.ui.view.ContentType
import com.pyrus.pyrusservicedesk.presentation.ui.view.Status
import com.pyrus.pyrusservicedesk.sdk.data.Attachment
import com.pyrus.pyrusservicedesk.sdk.data.Comment
import com.pyrus.pyrusservicedesk.sdk.data.Ticket

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
            toolbarTitleText = state.ticket?.subject,
        )
        State.Loading -> Model(
            inputText = "",
            sendEnabled = false,
            comments = null,
            isLoading = true,
            showNoConnectionError = false,
            toolbarTitleText = null,
        )
        State.Error -> Model(
            inputText = "",
            sendEnabled = false,
            comments = null,
            isLoading = false,
            showNoConnectionError = true,
            toolbarTitleText = null,
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
        Event.OnBackClick -> Message.Outer.OnBackClick
    }

    fun map(effect: Effect.Outer) = when(effect) {
        is Effect.Outer.CopyToClipboard -> TicketView.Effect.CopyToClipboard(effect.text)
        is Effect.Outer.MakeToast -> TicketView.Effect.MakeToast(effect.text)
    }

    private fun mapComments(
        ticket: Ticket,
        welcomeMessage: String?,
    ): List<CommentEntryV2> {
        val entries = ArrayList<CommentEntryV2>()
        val freshList = ticket.comments ?: emptyList()

        if (welcomeMessage != null) addWelcomeEntries(entries, welcomeMessage, freshList)

        addCommentEntries(entries, freshList)

        val entriesWithDates = toListWithDates(entries).toMutableList()

        if (ticket.showRating) {
            if (!ticket.showRatingText.isNullOrBlank()) entriesWithDates += CommentEntryV2.SimpleText(
                entryId = RATING_TEXT_ID,
                message = ticket.showRatingText
            )
            entriesWithDates += CommentEntryV2.RatingSelector
        }

        if (!ticket.showRating) {
            val buttonEntry = ticket.comments?.lastOrNull()?.let { extractButtons(it) }
            if (buttonEntry != null) entriesWithDates += buttonEntry
        }

        return entriesWithDates
    }

    private fun addWelcomeEntries(
        entries: ArrayList<CommentEntryV2>,
        welcomeMessage: String,
        freshList: List<Comment>
    ) {
        val firstComment = freshList.firstOrNull()
        val welcomeCreationTime = firstComment?.creationDate?.time ?: System.currentTimeMillis()

        val welcomeEntry = CommentEntryV2.Comment(
            WELCOME_MESSAGE_ID.toString(),
            WELCOME_MESSAGE_ID,
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
        freshList: List<Comment>,
    ) {
        for (comment in freshList) {
            addCommentEntries(entries, comment, "") // TODO
        }
    }

    private fun addCommentEntries(
        entries: ArrayList<CommentEntryV2>,
        comment: Comment,
        baseUrl: String,
    ) {

        // TODO
//        val avatarId = comment.author.avatarId
//        if (avatarId == 0) {
//
//        }
//        val avatarUrl = "${baseUrl}Avatar/${comment.author.avatarId}"
        val avatarUrl: String? = null

        val commentBody = comment.body
        if (!commentBody.isNullOrBlank()) {
            entries += toTextEntry(commentBody, comment, avatarUrl)
        }

        val attachments = comment.attachments
        if (!attachments.isNullOrEmpty()) {
            for (attach in attachments) {
                entries += toAttachEntry(comment, attach, avatarUrl)
            }
        }
    }

    private fun toTextEntry(commentBody: String, comment: Comment, avatarUrl: String?): CommentEntryV2.Comment {
        val isLocal = comment.isLocal()
        return CommentEntryV2.Comment(
            entryId = "${comment.commentId}",
            id = comment.commentId,
            isInbound = comment.isInbound,
            hasError = false, // TODO
            isLocal = isLocal,
            isWelcomeMessage = false,
            timeText = "time ", // TODO
            status = Status.Completed, // TODO
            contentType = ContentType.Text,
            authorName = comment.author.name,
            avatarUrl = avatarUrl,
            content = CommentEntryV2.CommentContent.Text(commentBody),
        )
    }

    private fun toAttachEntry(comment: Comment, attach: Attachment, avatarUrl: String?): CommentEntryV2.Comment {

        val bytesSize = attach.bytesSize
        val isMegabytes = bytesSize >= BYTES_IN_MEGABYTE / 10
//        val toShow = when {
//            isMegabytes -> bytesSize / BYTES_IN_MEGABYTE
//            else -> bytesSize / BYTES_IN_KILOBYTE
//        }
//        val textResId = when{
//            isMegabytes -> com.pyrus.pyrusservicedesk.R.string.psd_file_size_mb
//            else -> R.string.psd_file_size_kb
//        }

        val size = "getString(textResId, toShow)"
        TODO()

//        return CommentEntryV2.Comment(
//            entryId = "${comment.commentId}_${attach.id}",
//            id = comment.commentId,
//            isInbound = comment.isInbound,
//            hasError = false, // TODO
//            isLocal = comment.isLocal(),
//            isWelcomeMessage = false,
//            timeText = "time", // TODO
//            status = Status.Completed,
//            authorName = null,
//            avatarUrl = comment.author.name,
//            contentType = ContentType.Attachment, // TODO
//            content = CommentEntryV2.CommentContent.Image(
//                null, // TODO
//                attach.name,
//                null,
//                size,
//                null,
//            ),
//        )
    }

    private fun extractButtons(lastComment: Comment): CommentEntryV2.Buttons? {
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