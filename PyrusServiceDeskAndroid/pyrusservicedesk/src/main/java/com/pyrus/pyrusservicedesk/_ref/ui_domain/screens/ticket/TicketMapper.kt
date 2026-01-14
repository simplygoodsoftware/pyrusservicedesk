package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket

import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.data.Attachment
import com.pyrus.pyrusservicedesk._ref.data.Comment
import com.pyrus.pyrusservicedesk._ref.data.FullTicket
import com.pyrus.pyrusservicedesk._ref.data.RatingSettings
import com.pyrus.pyrusservicedesk._ref.data.RatingTextValues
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract.Effect
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract.Message
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract.RecordState
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract.State
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketView.Event
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketView.Model
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.entries.CommentEntry
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.fingerprints.ContentType
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.fingerprints.Status
import com.pyrus.pyrusservicedesk._ref.utils.TextProvider
import com.pyrus.pyrusservicedesk._ref.utils.getDateText
import com.pyrus.pyrusservicedesk._ref.utils.isAudio
import com.pyrus.pyrusservicedesk._ref.utils.isImage
import com.pyrus.pyrusservicedesk._ref.utils.plus
import com.pyrus.pyrusservicedesk._ref.utils.textRes
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.HtmlUtils
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.cleanTags
import com.pyrus.pyrusservicedesk.sdk.data.SatisfactionDisplayType
import java.util.Calendar

internal object TicketMapper {

    private const val WELCOME_MESSAGE_ID = -2L
    private const val RATING_TEXT_ID = -3L

    fun map(state: State): Model = when(state) {
        is State.Content -> Model(
            inputText = state.inputText,
            sendEnabled = state.sendEnabled,
            comments = state.ticket?.let {
                mapComments(
                    it,
                    state.welcomeMessage,
                    state.previousTicketLastCommentId
                )
            },
            isLoading = false,
            showNoConnectionError = false,
            isRefreshing = state.isLoading,
            toolbarTitleText = state.ticket?.subject?.cleanTags()?.textRes()
                ?: (R.string.new_ticket).textRes(),
            showInputPanel = true,
            wavesIsVisible = state.recordState is RecordState.Recording || state.recordState is RecordState.HoldRecording,
            recordState = state.recordState,
            pendingAudio = state.pendingRecord,
            actionButtonIsSend = actionButtonIsSend(state),
            canDragRecordMic = state.recordState is RecordState.None || state.recordState is RecordState.Recording,
            scrollDownIsVisible = state.recordState !is RecordState.Recording && state.recordState !is RecordState.HoldRecording,
            ratingTextRvVisibility = SatisfactionDisplayType.fromInt(state.ticket?.ratingSettings?.type) == SatisfactionDisplayType.Text,
            smileLl5Visibility = getSmile5LlVisibility(state.ticket?.ratingSettings),
            smileLlVisibility = getSmileLlVisibility(state.ticket?.ratingSettings),
            likeLlVisibility = SatisfactionDisplayType.fromInt(state.ticket?.ratingSettings?.type) == SatisfactionDisplayType.Like,
            rating2MiniVisibility = getSmileLlVisibility(state.ticket?.ratingSettings) && state.ticket?.ratingSettings?.size == 3,
            ratingText = state.ticket?.showRatingText,
            size = state.ticket?.ratingSettings?.size,
            type = state.ticket?.ratingSettings?.type,
            ratingTextValues = state.ticket?.ratingSettings?.ratingTextValues?.map {
                mapToRatingTextValuesEntry(
                    it
                )
            },
            showRating = state.ticket?.showRating == true,
            operatorTimeMessage = state.ticket?.operatorTimeMessage,
        )
        State.Loading -> Model(
            inputText = "",
            sendEnabled = false,
            comments = null,
            isLoading = true,
            showNoConnectionError = false,
            isRefreshing = false,
            toolbarTitleText = null,
            showInputPanel = false,
            wavesIsVisible = false,
            recordState = RecordState.None,
            pendingAudio = null,
            actionButtonIsSend = true,
            canDragRecordMic = false,
            scrollDownIsVisible = false,
            ratingTextRvVisibility = false,
            smileLl5Visibility = false,
            smileLlVisibility = false,
            likeLlVisibility = false,
            rating2MiniVisibility = false,
            ratingText = null,
            size = null,
            type = null,
            ratingTextValues = null,
            showRating = false,
            operatorTimeMessage = null,
        )
        State.Error -> Model(
            inputText = "",
            sendEnabled = false,
            comments = null,
            isLoading = false,
            showNoConnectionError = true,
            isRefreshing = false,
            toolbarTitleText = null,
            showInputPanel = false,
            wavesIsVisible = false,
            recordState = RecordState.None,
            pendingAudio = null,
            actionButtonIsSend = true,
            canDragRecordMic = false,
            scrollDownIsVisible = false,
            ratingTextRvVisibility = false,
            smileLl5Visibility = false,
            smileLlVisibility = false,
            likeLlVisibility = false,
            rating2MiniVisibility = false,
            ratingText = null,
            size = null,
            type = null,
            ratingTextValues = null,
            showRating = false,
            operatorTimeMessage = null,
        )
    }

    private fun actionButtonIsSend(state: State.Content): Boolean {
        if (state.voiceMessage && state.recordState is RecordState.HoldRecording) return true
        if (state.voiceMessage && state.recordState is RecordState.Recording) return false
        if (state.voiceMessage && state.recordState is RecordState.PendingRecord) return true
        return state.inputText.isNotBlank()
    }

    fun map(event: Event): Message.Outer = when(event) {
        is Event.OnCloseClick -> Message.Outer.OnCloseClick
        is Event.OnCopyClick -> Message.Outer.OnCopyClick(event.text)
        is Event.OnMessageChanged -> Message.Outer.OnMessageChanged(event.text)
        is Event.OnButtonClick -> Message.Outer.OnButtonClick(event.buttonText)
        is Event.OnPreviewClick -> Message.Outer.OnPreviewClick(event.commentId, event.attachmentId)
        is Event.OnRatingClick -> Message.Outer.OnRatingClick(event.rating, event.rateUsText)
        is Event.OnErrorCommentClick -> Message.Outer.OnErrorCommentClick(event.localId)
        is Event.OnSendClick -> Message.Outer.OnSendClick
        is Event.OnShowAttachVariantsClick -> Message.Outer.OnShowAttachVariantsClick
        is Event.OnRefresh -> Message.Outer.OnRefresh
        is Event.OnBackClick -> Message.Outer.OnBackClick
        is Event.OnCancelUploadClick -> Message.Outer.OnCancelUploadClick(event.localId, event.attachmentId)
        is Event.OnInfoClick -> Message.Outer.OnInfoClick
        is Event.OnStartRecord -> Message.Outer.OnStartRecord
        is Event.OnStopRecord -> Message.Outer.OnStopRecord
        is Event.OnStopEndSendRecord -> Message.Outer.OnStopEndSendRecord
        is Event.OnMicShortClicked -> Message.Outer.OnMicShortClicked
        is Event.OnCancelRecord -> Message.Outer.OnCancelRecord
        is Event.OnLockRecord -> Message.Outer.OnLockRecord
        is Event.OnRemovePendingAudioClick -> Message.Outer.OnRemovePendingAudioClick
        is Event.SetAttachVariant -> Message.Outer.SetAttachVariant(event.key, event.uri)
        is Event.SetErrorCommentResult -> Message.Outer.SetErrorCommentResult(event.localId, event.key, event.action)
    }

    fun map(effect: Effect.Outer): TicketView.Effect = when(effect) {
        is Effect.Outer.CopyToClipboard -> TicketView.Effect.CopyToClipboard(effect.text)
        is Effect.Outer.MakeToast -> TicketView.Effect.MakeToast(effect.text)
        is Effect.Outer.ShowAttachVariants -> TicketView.Effect.ShowAttachVariants(effect.key)
        is Effect.Outer.ShowErrorCommentDialog -> TicketView.Effect.ShowErrorCommentDialog(effect.key, effect.localId)
        is Effect.Outer.ShowInfoBottomSheetFragment -> TicketView.Effect.ShowInfoBottomSheetFragment(effect.ticketId, effect.userName, effect.createData)
        is Effect.Outer.PlayAudio -> TicketView.Effect.PlayAudio(effect.audioFile, effect.guid)
        is Effect.Outer.UpdateRecordWave -> TicketView.Effect.UpdateRecordWave(effect.recordedSegmentValues)
        is Effect.Outer.ShowAudioRecordTooltip -> TicketView.Effect.ShowAudioRecordTooltip
        is Effect.Outer.Exit -> TicketView.Effect.Exit
        is Effect.Outer.OpenPreview -> TicketView.Effect.OpenPreview(effect.fileData)
        is Effect.Outer.OpenRatingComment -> TicketView.Effect.OpenRatingComment(effect.rateUsText)
    }

    private fun mapComments(
        freshList: FullTicket,
        welcomeMessage: String?,
        previousTicketLastCommentId: Long?,
    ): List<CommentEntry> {
        val entries = ArrayList<CommentEntry>()

        if (welcomeMessage != null) {
            entries += welcomeEntry(welcomeMessage, freshList)
        }

        addCommentEntries(entries, freshList, welcomeMessage, previousTicketLastCommentId)

        val entriesWithDates = toListWithDates(entries).toMutableList()

        var buttonsEntry: CommentEntry.Buttons? = null

        if (freshList.comments.isEmpty() || isLastWelcome(entries)) {
            buttonsEntry = (entries.lastOrNull() as? CommentEntry.Comment.CommentText)?.let { extractButtonsFromWelcome(it, welcomeMessage) }
        }

        if (!freshList.showRating) {
            buttonsEntry = freshList.comments.lastOrNull()?.let { extractButtons(it) }
        }

        if (buttonsEntry != null) entriesWithDates += buttonsEntry

        entriesWithDates.reverse()
        var resentInbound: Boolean? = null
        var resentAuthorKey: String? = null
        for (i in entriesWithDates.indices) {
            val current = entriesWithDates[i]
            val currentIsInbound = (current as? CommentEntry.Comment)?.isInbound
            val currentAuthorKey = (current as? CommentEntry.Comment)?.authorKey
            val showAvatar = (currentIsInbound != resentInbound || currentAuthorKey != resentAuthorKey) && currentIsInbound == false
            resentInbound = currentIsInbound
            resentAuthorKey = currentAuthorKey

            if (current is CommentEntry.Comment) {
                val prev = entriesWithDates.getOrNull(i + 1)
                val prevAuthorKey = (prev as? CommentEntry.Comment)?.authorKey
                val showAuthorName = when {
                    current.isWelcomeMessage -> false
                    prev !is CommentEntry.Comment -> true
                    else -> prevAuthorKey != currentAuthorKey
                }
                val updatedEntry = when(current) {
                    is CommentEntry.Comment.CommentAttachment -> current.copy(
                        showAvatar = showAvatar,
                        showAuthorName = showAuthorName
                    )
                    is CommentEntry.Comment.CommentPreviewableAttachment -> current.copy(
                        showAvatar = showAvatar,
                        showAuthorName = showAuthorName
                    )
                    is CommentEntry.Comment.CommentText -> current.copy(
                        showAvatar = showAvatar,
                        showAuthorName = showAuthorName
                    )

                    is CommentEntry.Comment.CommentAudio -> current.copy(
                        showAvatar = showAvatar,
                        showAuthorName = showAuthorName
                    )

                    is CommentEntry.Comment.CommentSystemText -> current.copy(
                        showAvatar = false,
                        showAuthorName = false,
                    )
                }
                entriesWithDates[i] = updatedEntry
            }
        }
        entriesWithDates.reverse()

        return entriesWithDates
    }

    private fun isLastWelcome(entries: List<CommentEntry>): Boolean {
        val entry = entries.lastOrNull()
        if (entry == null)
            return false
        if ((entry as? CommentEntry.Comment.CommentText)?.isWelcomeMessage == true)
            return true
        return false
    }

    private fun mapToRatingTextValuesEntry(ratingTextValue: RatingTextValues) = CommentEntry.RatingTextValues(
        rating = ratingTextValue.rating,
        text = ratingTextValue.text
    )


    private fun getSmileLlVisibility(ratingSettings: RatingSettings?): Boolean {
        return SatisfactionDisplayType.fromInt(ratingSettings?.type) == SatisfactionDisplayType.Emoji
            && ratingSettings?.size != null
            && ratingSettings.size < 5
    }
    private fun getSmile5LlVisibility(ratingSettings: RatingSettings?): Boolean {
        return SatisfactionDisplayType.fromInt(ratingSettings?.type) == SatisfactionDisplayType.Emoji
            && ratingSettings?.size == 5
            || ratingSettings == null
    }

    private fun welcomeEntry(
        welcomeMessage: String,
        freshList: FullTicket
    ): CommentEntry. Comment. CommentText {
        val firstComment = freshList.comments.firstOrNull()
        val creationTime = firstComment?.creationTime ?: System.currentTimeMillis()

        val entryText = welcomeMessage.cleanTags(removeLinkTag = false)
        return CommentEntry.Comment.CommentText(
            creationTime = creationTime,
            entryId = WELCOME_MESSAGE_ID.toString(),
            id = WELCOME_MESSAGE_ID,
            isInbound = false,
            hasError = false,
            isLocal = false,
            isWelcomeMessage = true,
            timeText = null,
            status = Status.Completed,
            authorName = null,
            showAuthorName = false,
            avatarUrl = freshList.orgLogoUrl,
            authorKey = null,
            showAvatar = true,
            isSupport = true,
            text = entryText,
        )
    }

    private fun addCommentEntries(
        entries: ArrayList<CommentEntry>,
        freshList: FullTicket,
        welcomeMessage: String?,
        previousTicketLastCommentId: Long?,
    ) {
        val commentEntries = ArrayList<CommentEntry>()
        for (comment in freshList.comments) {
            addCommentEntries(commentEntries, comment, freshList.orgLogoUrl)
            if (comment.id == previousTicketLastCommentId && welcomeMessage != null)
                commentEntries += welcomeEntry(welcomeMessage, freshList.copy(comments = emptyList()))
        }

        entries += commentEntries
    }

    private fun addCommentEntries(
        entries: ArrayList<CommentEntry>,
        comment: Comment,
        orgLogoUrl: String?
    ) {
        val body = comment.body?.cleanTags("")
        val attachments = comment.attachments
        val rating = comment.rating

        val textIsEmpty = body?.replace("\n", "").isNullOrBlank()
        val attachmentsIsEmpty = attachments.isNullOrEmpty()
        if (textIsEmpty && attachmentsIsEmpty && rating == null) {
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

        if (!body.isNullOrBlank() && !comment.isSystem) {
            entries += toTextEntry(comment.body, comment, status, avatarUrl)
        }

        if (!body.isNullOrBlank() && comment.isSystem) {
            entries += toSystemTextEntry(comment.body, comment, status, avatarUrl)
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
    ): CommentEntry.Comment.CommentText {
        val entryText = commentBody.cleanTags(removeLinkTag = false)
        return CommentEntry.Comment.CommentText(
            creationTime = comment.creationTime,
            entryId = "${comment.persistentId}",
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
            isSupport = comment.isSupport,
            text = entryText
        )
    }

    private fun toSystemTextEntry(
        commentBody: String,
        comment: Comment,
        status: Status,
        avatarUrl: String?
    ): CommentEntry.Comment.CommentSystemText {
        val entryText = commentBody.cleanTags(removeLinkTag = false)
        return CommentEntry.Comment.CommentSystemText(
            creationTime = comment.creationTime,
            entryId = "${comment.persistentId}",
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
            isSupport = comment.isSupport,
            text = entryText
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
    ): CommentEntry {

        val contentType = when {
            attach.name.isImage() -> ContentType.PreviewableAttachment
            attach.name.isAudio() -> ContentType.AudioAttachment
            else -> ContentType.Attachment
        }

        val image = CommentEntry.Attach(
            attachId = attach.id,
            attachUrl = attach.uri,
            attachmentName = attach.name,
            isImage = attach.name.isImage(),
            fileSize = attach.bytesSize.toFloat(),
            fileProgressStatus = attach.status,
            uploadProgress = attach.progress
        )
        return when (contentType)  {
            ContentType.PreviewableAttachment -> CommentEntry.Comment.CommentPreviewableAttachment(
                creationTime = comment.creationTime,
                entryId = "${comment.persistentId}_${attach.id}",
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
                isSupport = comment.isSupport,
                attach = image,
            )
            ContentType.AudioAttachment -> CommentEntry.Comment.CommentAudio(
                creationTime = comment.creationTime,
                entryId = "${comment.persistentId}_${attach.id}",
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
                isSupport = comment.isSupport,
                attach = image,
            )
            else -> CommentEntry.Comment.CommentAttachment(
                creationTime = comment.creationTime,
                entryId = "${comment.persistentId}_${attach.id}",
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
                isSupport = comment.isSupport,
                attach = image,
            )
        }
    }

    private fun extractButtons(lastComment: Comment): CommentEntry.Buttons? {
        val buttons = HtmlUtils.extractButtons(lastComment.body)
        if (buttons.isEmpty()) {
            return null
        }

        return CommentEntry.Buttons(lastComment.creationTime, lastComment.id, buttons)
    }

    private fun extractButtonsFromWelcome(welcomeMessage: CommentEntry.Comment.CommentText, text: String?): CommentEntry.Buttons? {
        val buttons = HtmlUtils.extractButtons(text)
        if (buttons.isEmpty()) {
            return null
        }

        return CommentEntry.Buttons(welcomeMessage.creationTime, welcomeMessage.id, buttons)
    }

    private fun toListWithDates(entries: List<CommentEntry>): List<CommentEntry> {
        val listWithDates = ArrayList<CommentEntry>(entries.size)

        val now = Calendar.getInstance()
        val calendar = Calendar.getInstance()
        var prevDate: Long? = null
        for (entry in entries) {
            val creationTime = (entry as? CommentEntry.Comment)?.creationTime
            if (creationTime != null) {
                calendar.timeInMillis = creationTime
                calendar[Calendar.HOUR_OF_DAY] = 0
                calendar[Calendar.MINUTE] = 0
                calendar[Calendar.SECOND] = 0
                calendar[Calendar.MILLISECOND] = 0
                val date = calendar.timeInMillis
                if (date != prevDate) {
                    listWithDates += CommentEntry.Date(getDateText(calendar, now))
                }
                prevDate = date
            }
            listWithDates += entry
        }

        return listWithDates
    }

}