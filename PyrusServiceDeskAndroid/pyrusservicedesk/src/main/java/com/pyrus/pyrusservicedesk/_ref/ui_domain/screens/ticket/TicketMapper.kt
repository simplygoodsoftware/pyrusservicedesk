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
import com.pyrus.pyrusservicedesk._ref.utils.getDateText
import com.pyrus.pyrusservicedesk._ref.utils.isImage
import com.pyrus.pyrusservicedesk._ref.utils.plus
import com.pyrus.pyrusservicedesk._ref.utils.textRes
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.HtmlTagUtils
import com.pyrus.pyrusservicedesk.presentation.ui.view.ContentType
import com.pyrus.pyrusservicedesk.presentation.ui.view.Status
import java.util.Calendar

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
            toolbarTitleText = state.ticket?.subject?.let(HtmlTagUtils::cleanTags)?.textRes()
                ?: (R.string.new_ticket).textRes(),
        )
        State.Loading -> Model(
            inputText = "",
            sendEnabled = false,
            comments = null,
            isLoading = true,
            showNoConnectionError = false,
            isRefreshing = false,
            toolbarTitleText = null,
        )
        State.Error -> Model(
            inputText = "",
            sendEnabled = false,
            comments = null,
            isLoading = false,
            showNoConnectionError = true,
            isRefreshing = false,
            toolbarTitleText = null,
        )
    }

    fun map(event: Event): Message.Outer = when(event) {
        Event.OnCloseClick -> Message.Outer.OnCloseClick
        is Event.OnCopyClick -> Message.Outer.OnCopyClick(event.text)
        is Event.OnMessageChanged -> Message.Outer.OnMessageChanged(event.text)
        is Event.OnButtonClick -> Message.Outer.OnButtonClick(event.buttonText)
        is Event.OnPreviewClick -> Message.Outer.OnPreviewClick(event.commentId, event.attachmentId)
        is Event.OnRatingClick -> Message.Outer.OnRatingClick(event.rating)
        is Event.OnErrorCommentClick -> Message.Outer.OnErrorCommentClick(event.localId)
        is Event.OnSendClick -> Message.Outer.OnSendClick
        is Event.OnShowAttachVariantsClick -> Message.Outer.OnShowAttachVariantsClick
        is Event.OnRefresh -> Message.Outer.OnRefresh
        is Event.OnBackClick -> Message.Outer.OnBackClick
        is Event.OnCancelUploadClick -> Message.Outer.OnCancelUploadClick(event.localId, event.attachmentId)
    }

    fun map(effect: Effect.Outer): TicketView.Effect = when(effect) {
        is Effect.Outer.CopyToClipboard -> TicketView.Effect.CopyToClipboard(effect.text)
        is Effect.Outer.MakeToast -> TicketView.Effect.MakeToast(effect.text)
        is Effect.Outer.ShowAttachVariants -> TicketView.Effect.ShowAttachVariants(effect.key)
        is Effect.Outer.ShowErrorCommentDialog -> TicketView.Effect.ShowErrorCommentDialog(effect.key)
    }

    private fun mapComments(
        freshList: FullTicket,
        welcomeMessage: String?,
    ): List<CommentEntryV2> {
        val entries = ArrayList<CommentEntryV2>()

        if (welcomeMessage != null) addWelcomeEntries(entries, welcomeMessage, freshList)

        addCommentEntries(entries, freshList)

        val entriesWithDates = toListWithDates(entries).toMutableList()

        val lastCreationTime = freshList.comments.lastOrNull()?.creationTime ?: System.currentTimeMillis()

        if (freshList.showRating) {
            if (!freshList.showRatingText.isNullOrBlank()) entriesWithDates += CommentEntryV2.SimpleText(
                creationTime = lastCreationTime,
                entryId = RATING_TEXT_ID,
                message = freshList.showRatingText
            )
            entriesWithDates += CommentEntryV2.RatingSelector(creationTime = lastCreationTime)
        }

        if (!freshList.showRating) {
            val buttonEntry = freshList.comments.lastOrNull()?.let { extractButtons(it) }
            if (buttonEntry != null) entriesWithDates += buttonEntry
        }

        entriesWithDates.reverse()
        var resentInbound: Boolean? = null
        var resentAuthorKey: String? = null
        for (i in entriesWithDates.indices) {
            val current = entriesWithDates[i]
            val currentIsInbound = (current as? CommentEntryV2.Comment)?.isInbound
            val currentAuthorKey = (current as? CommentEntryV2.Comment)?.authorKey
            val showAvatar = currentIsInbound != resentInbound || currentAuthorKey != resentAuthorKey
            resentInbound = currentIsInbound
            resentAuthorKey = currentAuthorKey

            if (current is CommentEntryV2.Comment) {
                val prev = entriesWithDates.getOrNull(i + 1)
                val prevAuthorKey = (prev as? CommentEntryV2.Comment)?.authorKey
                val showAuthorName = when {
                    prev !is CommentEntryV2.Comment -> true
                    else -> prevAuthorKey != currentAuthorKey
                }

                entriesWithDates[i] = current.copy(showAvatar = showAvatar, showAuthorName = showAuthorName)
            }
        }
        entriesWithDates.reverse()

        return entriesWithDates
    }

    private fun addWelcomeEntries(
        entries: ArrayList<CommentEntryV2>,
        welcomeMessage: String,
        freshList: FullTicket
    ) {
        val firstComment = freshList.comments.firstOrNull()
        val creationTime = firstComment?.creationTime ?: System.currentTimeMillis()

        val entryText = HtmlTagUtils.cleanTags(welcomeMessage)
        val welcomeEntry = CommentEntryV2.Comment(
            creationTime = creationTime,
            entryId = WELCOME_MESSAGE_ID.toString(),
            id = WELCOME_MESSAGE_ID,
            isInbound = false,
            hasError = false,
            isLocal = false,
            isWelcomeMessage = true,
            timeText = null,
            status = Status.Completed,
            contentType = ContentType.Text,
            authorName = null,
            showAuthorName = false,
            avatarUrl = null,
            authorKey = null,
            showAvatar = true,
            content = CommentEntryV2.CommentContent.Text(entryText),
        )
        entries += welcomeEntry
    }

    private fun addCommentEntries(
        entries: ArrayList<CommentEntryV2>,
        freshList: FullTicket,
    ) {
        val commentEntries = ArrayList<CommentEntryV2>()
        for (comment in freshList.comments) {
            addCommentEntries(commentEntries, comment, freshList.orgLogoUrl)
        }

        entries += commentEntries
    }

    private fun addCommentEntries(
        entries: ArrayList<CommentEntryV2>,
        comment: Comment,
        orgLogoUrl: String?
    ) {
        val body = comment.body
        val attachments = comment.attachments
        val rating = comment.rating

        if (body.isNullOrBlank() && attachments.isNullOrEmpty() && rating == null) {
            return
        }

        val status = when {
            comment.isLocal -> {
                if (comment.isSending) Status.Processing
                else Status.Error
            }
            else -> Status.Completed
        }

        val avatarUrl = if (comment.isSupport) orgLogoUrl else null
//        val avatarUrl = comment.author?.avatarUrl ?: if (comment.isSupport) orgLogoUrl else null

        if (!body.isNullOrBlank()) {
            entries += toTextEntry(body, comment, status, avatarUrl)
        }

        attachments?.forEach { attach ->
            entries += toAttachEntry(comment, attach, status, avatarUrl)
        }
    }

    private fun getAuthorName(comment: Comment): TextProvider? =
        if (!comment.isSupport) comment.author?.name?.textRes()
        else "${comment.author?.name} (".textRes() + R.string.psd_support.textRes() + ")"

    private fun toTextEntry(
        commentBody: String,
        comment: Comment,
        status: Status,
        avatarUrl: String?
    ): CommentEntryV2.Comment {
        val entryText = HtmlTagUtils.cleanTags(commentBody)
        return CommentEntryV2.Comment(
            creationTime = comment.creationTime,
            entryId = "${comment.id}",
            id = comment.id,
            isInbound = comment.isInbound,
            hasError = status == Status.Error,
            isLocal = comment.isLocal,
            isWelcomeMessage = false,
            timeText = TextProvider.Date(comment.creationTime, R.string.psd_time_format),
            status = status,
            contentType = ContentType.Text,
            authorName = getAuthorName(comment),
            authorKey = getAuthorKey(comment),
            showAuthorName = false,
            avatarUrl = avatarUrl,
            showAvatar = false,
            content = CommentEntryV2.CommentContent.Text(entryText),
        )
    }

    private fun getAuthorKey(comment: Comment): String {
        return "${comment.author?.authorId}${comment.author?.name}${comment.isInbound}"
    }

    private fun toAttachEntry(
        comment: Comment,
        attach: Attachment,
        status: Status,
        avatarUrl: String?
    ): CommentEntryV2.Comment {

        val contentType = when {
            attach.name.isImage() -> ContentType.PreviewableAttachment
            else -> ContentType.Attachment
        }

        return CommentEntryV2.Comment(
            creationTime = comment.creationTime,
            entryId = "${comment.id}_${attach.id}",
            id = comment.id,
            isInbound = comment.isInbound,
            hasError = status == Status.Error,
            isLocal = comment.isLocal,
            isWelcomeMessage = false,
            timeText = TextProvider.Date(comment.creationTime, R.string.psd_time_format),
            status = status,
            authorName = getAuthorName(comment),
            authorKey = getAuthorKey(comment),
            showAuthorName = false,
            avatarUrl = avatarUrl,
            showAvatar = false,
            contentType = contentType,
            content = CommentEntryV2.CommentContent.Image(
                attachId = attach.id,
                attachUrl = attach.uri,
                attachmentName = attach.name,
                isImage = attach.name.isImage(),
                fileSize = attach.bytesSize.toFloat(),
                fileProgressStatus = attach.status,
                uploadProgress = attach.progress
            ),
        )
    }

    private fun extractButtons(lastComment: Comment): CommentEntryV2.Buttons? {
        val buttons = HtmlTagUtils.extractButtons(lastComment.body)
        if (buttons.isEmpty()) {
            return null
        }

        return CommentEntryV2.Buttons(lastComment.creationTime, lastComment.id, buttons)
    }

    private fun toListWithDates(entries: List<CommentEntryV2>): List<CommentEntryV2> {
        val listWithDates = ArrayList<CommentEntryV2>(entries.size)

        val now = Calendar.getInstance()
        val calendar = Calendar.getInstance()
        var prevDate: Long? = null
        for (entry in entries) {
            val creationTime = (entry as? CommentEntryV2.WithCreationTime)?.creationTime
            if (creationTime != null) {
                calendar.timeInMillis = creationTime
                calendar[Calendar.HOUR_OF_DAY] = 0
                calendar[Calendar.MINUTE] = 0
                calendar[Calendar.SECOND] = 0
                calendar[Calendar.MILLISECOND] = 0
                val date = calendar.timeInMillis
                if (date != prevDate) {
                    listWithDates += CommentEntryV2.Date(getDateText(calendar, now))
                }
                prevDate = date
            }
            listWithDates += entry
        }

        return listWithDates
    }

}