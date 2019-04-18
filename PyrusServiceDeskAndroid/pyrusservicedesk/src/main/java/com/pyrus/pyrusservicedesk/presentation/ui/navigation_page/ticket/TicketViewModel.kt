package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.support.v7.util.DiffUtil
import android.widget.Toast
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.presentation.call.*
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries.*
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.DiffResultWithNewItems
import com.pyrus.pyrusservicedesk.presentation.viewmodel.ConnectionViewModelBase
import com.pyrus.pyrusservicedesk.sdk.data.Attachment
import com.pyrus.pyrusservicedesk.sdk.data.Comment
import com.pyrus.pyrusservicedesk.sdk.data.EMPTY_TICKET_ID
import com.pyrus.pyrusservicedesk.sdk.updates.OnUnreadTicketCountChangedSubscriber
import com.pyrus.pyrusservicedesk.sdk.web.OnCancelListener
import com.pyrus.pyrusservicedesk.sdk.web.UploadFileHooks
import com.pyrus.pyrusservicedesk.utils.ConfigUtils
import com.pyrus.pyrusservicedesk.utils.MILLISECONDS_IN_SECOND
import com.pyrus.pyrusservicedesk.utils.RequestUtils.Companion.MAX_FILE_SIZE_BYTES
import com.pyrus.pyrusservicedesk.utils.RequestUtils.Companion.MAX_FILE_SIZE_MEGABYTES
import com.pyrus.pyrusservicedesk.utils.getWhen
import java.util.*
import kotlin.collections.ArrayList

/**
 * ViewModel for the ticket screen.
 */
internal class TicketViewModel(
        serviceDesk: PyrusServiceDesk,
        arguments: Intent)
    : ConnectionViewModelBase(serviceDesk),
        OnClickedCallback<CommentEntry>,
        OnUnreadTicketCountChangedSubscriber {

    private companion object {

        const val TICKET_UPDATE_INTERVAL = 30L

        fun checkComment(comment: Comment): CheckCommentError? {
            return when{
                comment.isEmpty() -> CheckCommentError.CommentIsEmpty
                comment.hasAttachmentWithExceededSize() -> CheckCommentError.FileSizeExceeded
                else -> null
            }
        }

        fun Comment.isEmpty(): Boolean = body.isBlank() && attachments.isNullOrEmpty()

        fun Comment.hasAttachmentWithExceededSize(): Boolean =
            attachments?.let { it.any { attach -> attach.hasExceededFileSize()} } ?: false

        fun Attachment.hasExceededFileSize(): Boolean = bytesSize > MAX_FILE_SIZE_BYTES
    }

    /**
     * Denotes whether [PyrusServiceDesk.isSingleChat] is enabled.
     */
    val isFeed = serviceDesk.isSingleChat

    /**
     * Drafted text. Assigned once when view model is created.
     */
    val draft: String

    private val draftRepository = serviceDesk.draftRepository

    private var isCreateTicketSent = false
    private var ticketId: Int = TicketActivity.getTicketId(arguments)

    private val unreadCounter = MutableLiveData<Int>()
    private val commentDiff = MutableLiveData<DiffResultWithNewItems<TicketEntry>>()

    private var ticketEntries: List<TicketEntry> = emptyList()

    private val mainHandler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            mainHandler.postDelayed(this, TICKET_UPDATE_INTERVAL * MILLISECONDS_IN_SECOND)
            update()
        }
    }

    private val lastServerCommentIdHolder = LastCommentIdHolder()

    /**
     * Comments that currently are being processed.
     * This is necessary to minimize comments list inconsistency when getFeed response already contains
     * comments ids that are expected to be received by addComment response.
     *
     * NB: addComment responses that are delivered with an error should not remove the requested comment from
     * the list, otherwise comments with error are lost
     */
    private val commentsInProcess: MutableList<Comment> by lazy { mutableListOf<Comment>() }

    init {
        draft = draftRepository.getDraft()

        if (!isFeed) {
            unreadCounter.value = TicketActivity.getUnreadTicketsCount(arguments)
        }

        if (canBeUpdated() && isNetworkConnected.value == true) {
            loadData()
        }
        maybeStartAutoRefresh()
        liveUpdates.subscribeOnUnreadTicketCountChanged(this)
    }

    override fun onLoadData() {
        update()
    }

    override fun onUnreadTicketCountChanged(unreadTicketCount: Int) {
        this.unreadCounter.value = unreadTicketCount
    }

    override fun onClicked(item: CommentEntry) {
        if (!item.hasError())
            return
        else {
            commentsInProcess -= item.comment
            applyCommentUpdate(item, ChangeType.Cancelled)
            sendAddComment(item.comment, item.uploadFileHooks.also { it?.resetProgress() })
        }
    }

    override fun onCleared() {
        super.onCleared()
        mainHandler.removeCallbacks(updateRunnable)
        liveUpdates.unsubscribeFromTicketCountChanged(this)
    }

    /**
     * Callback to be invoked when user clicks "send" button.
     *
     * @param text text that is entered in an input field
     */
    fun onSendClicked(text: String) {
        if (text.isBlank()) {
            return
        }
        val localComment = localDataProvider.createLocalComment(text.trim())
        sendAddComment(localComment)
    }

    /**
     * Callback to be invoked when user picked file to send.
     *
     * @param attachmentUri URI of the file to be sent
     */
    fun onAttachmentSelected(attachmentUri: Uri) {
        val localComment = localDataProvider.createLocalComment(fileUri = attachmentUri)
        val fileHooks = UploadFileHooks()
        fileHooks.subscribeOnCancel(object : OnCancelListener {
            override fun onCancel() {
                return applyCommentUpdate(
                    CommentEntry(localComment, onClickedCallback = this@TicketViewModel), ChangeType.Cancelled)
            }
        })
        sendAddComment(localComment, fileHooks)
    }

    /**
     * Provides live data that delivers [DiffResultWithNewItems] which contains list of
     * current [TicketEntry]s and [DiffUtil.DiffResult] that is used for correctly apply
     * changes to UI.
     */
    fun getCommentDiffLiveData(): LiveData<DiffResultWithNewItems<TicketEntry>> = commentDiff

    /**
     * Provides live data that delivers counter of unread tickets.
     */
    fun getUnreadCounterLiveData(): LiveData<Int> = unreadCounter

    /**
     * Callback to be invoked when user input changed.
     */
    fun onInputTextChanged(text: String) {
        draftRepository.saveDraft(text)
    }

    private fun update() {
        val call = when {
            isFeed -> GetFeedCall(this@TicketViewModel, requests).execute()
            else -> GetTicketCall(this@TicketViewModel, requests, ticketId).execute()
        }
        val observer = Observer<CallResult<List<Comment>>> { result ->
                if (result == null)
                    return@Observer
                when {
                    result.hasError() -> {  }
                    else -> applyTicketUpdate(result.data!!)
                }
            }
        call.observeForever(observer)
    }

    private fun maybeStartAutoRefresh() {
        if (canBeUpdated()) {
            // delayed to prevent launching second unnecessary update in [init]
            mainHandler.postDelayed(updateRunnable, TICKET_UPDATE_INTERVAL * MILLISECONDS_IN_SECOND)
        }
    }

    private fun canBeUpdated() = isFeed || !isNewTicket()

    private fun isNewTicket() = ticketId == EMPTY_TICKET_ID

    private fun List<Comment>.toTicketEntries(): MutableList<TicketEntry> {
        val now = Calendar.getInstance()
        var prevDateGroup: String? = null
        return foldIndexed(ArrayList(size)){
            index, acc, comment ->
            comment.creationDate.getWhen(getApplication(), now).let {
                if (index == 0 || it != prevDateGroup) {
                    acc.add(DateEntry(it))
                    prevDateGroup = it
                }
            }
            acc.addAll(comment.splitToEntries())
            acc
        }
    }

    private fun sendAddComment(localComment: Comment,
                               uploadFileHooks: UploadFileHooks? = null) {

        if (commentContainsError(localComment))
            return

        commentsInProcess += localComment

        val toNewTicket = !isFeed && isNewTicket() && !isCreateTicketSent

        applyCommentUpdate(
            CommentEntry(localComment, uploadFileHooks = uploadFileHooks, onClickedCallback = this),
            ChangeType.Added
        )
        val call = when {
            toNewTicket -> {
                isCreateTicketSent = true
                CreateTicketCall(this, requests, localComment, uploadFileHooks).execute()
            }
            isFeed -> AddFeedCommentCall(this, requests, localComment, uploadFileHooks).execute()
            else -> AddCommentCall(this, requests, ticketId, localComment, uploadFileHooks).execute()
        }
        call.observeForever(getObserverForAddCommentCall(localComment, uploadFileHooks, toNewTicket))
    }

    private fun getObserverForAddCommentCall(localComment: Comment,
                                             uploadFileHooks: UploadFileHooks?,
                                             toNewTicket: Boolean): Observer<CallResult<Int>> {

        return Observer { res ->
            res?.let { result ->
                isCreateTicketSent = false
                if (uploadFileHooks?.isCancelled == true || !commentsInProcess.contains(localComment))
                    return@let
                val entry = when {
                    result.hasError() -> CommentEntry(
                        localComment,
                        uploadFileHooks, // for retry purpose
                        this@TicketViewModel,
                        error = result.error
                    )
                    else -> {
                        if (toNewTicket) {
                            ticketId = result.data!!
                            maybeStartAutoRefresh()
                        }
                        val commentId = if (toNewTicket) localComment.localId else result.data!!
                        if(commentId > lastServerCommentIdHolder.commentId){
                            lastServerCommentIdHolder.setLastCommentIdFromAddComment(commentId)
                        }
                        commentsInProcess -= localComment
                        CommentEntry(
                            localDataProvider.convertLocalCommentToServer(localComment, commentId),
                            onClickedCallback = this@TicketViewModel
                        )
                    }
                }
                applyCommentUpdate(entry, ChangeType.Changed)
            }
        }
    }

    private fun commentContainsError(localComment: Comment): Boolean {
        when (checkComment(localComment)) {
            CheckCommentError.CommentIsEmpty -> return true
            CheckCommentError.FileSizeExceeded -> {
                (getApplication() as Context).run {
                    Toast.makeText(
                        this,
                        this.getString(R.string.psd_file_size_exceeded_message, MAX_FILE_SIZE_MEGABYTES),
                        Toast.LENGTH_SHORT)
                        .show()
                }
                return true
            }
        }
        return false
    }

    private fun applyTicketUpdate(freshList: List<Comment>) {
        when{
            freshList.isEmpty() -> {
                ConfigUtils.getWelcomeMessage()?.let {
                    publishEntries(ticketEntries, listOf(WelcomeMessageEntry(it)))
                }
                onDataLoaded()
                return
            }
            freshList.last().commentId < lastServerCommentIdHolder.commentId -> return
            // Last comment may have been obtained from the addComment event, so
            // there are new comments might be before the added comment.
            // If last comment was obtained via getFeed this is not the case.
            !lastServerCommentIdHolder.isFromAddComment
                    && freshList.last().commentId == lastServerCommentIdHolder.commentId -> {
                onDataLoaded()
                return
            }
        }
        val lastCurrentMyCommentId =
            (ticketEntries.findLast {
                (it.type == Type.Comment) && !(it as CommentEntry).comment.isInbound
            } as? CommentEntry)
                ?.comment
                ?.commentId
                ?: 0
        var myNewCommentsCount = 0
        for (i in freshList.lastIndex downTo 0) {
            val comment = freshList[i]
            if (comment.commentId <= lastCurrentMyCommentId) {
                break
            }
            if (!comment.isInbound)
                myNewCommentsCount++
        }
        removeCommentsFromProcessingHead(myNewCommentsCount)

        val listOfLocalEntries = mutableListOf<TicketEntry>()
        if (hasRealComments()) {
            for (i in ticketEntries.lastIndex downTo 0) {
                val entry = ticketEntries[i]
                if (entry.type == Type.Comment
                    && (entry as CommentEntry).comment.commentId == lastServerCommentIdHolder.commentId)
                    break
                if (entry.type == Type.Comment
                    && (entry as CommentEntry).comment.isLocal()
                    && commentsInProcess.contains(entry.comment)) {

                    listOfLocalEntries.add(0, entry)
                }
            }
        }
        val toPublish = mutableListOf<TicketEntry>().apply {
            ConfigUtils.getWelcomeMessage()?.let { add(0, WelcomeMessageEntry(it)) }
            addAll(freshList.toTicketEntries())
            addAll(listOfLocalEntries)

        }
        publishEntries(ticketEntries, toPublish)
        lastServerCommentIdHolder.setLastCommentId(freshList.last().commentId)
        onDataLoaded()
    }

    private fun removeCommentsFromProcessingHead(commentCountToRemove: Int) {
        if (commentCountToRemove == 0 || commentsInProcess.size < commentCountToRemove)
            return
        for (i in 0..commentCountToRemove) {
            commentsInProcess.removeAt(0)
        }
    }

    private fun hasRealComments(): Boolean = ticketEntries.any { it.type == Type.Comment }

    private fun applyCommentUpdate(commentEntry: CommentEntry, changeType: ChangeType) {
        val newEntries = ticketEntries.toMutableList()
        when (changeType) {
            ChangeType.Added -> {
                maybeAddDate(commentEntry, newEntries)
                newEntries.add(commentEntry)
            }
            ChangeType.Changed -> {
                maybeAddDate(commentEntry, newEntries)
                newEntries.findIndex(commentEntry).let {
                    when(it){
                        -1 -> newEntries.add(commentEntry)
                        else -> newEntries[it] = commentEntry
                    }
                }
            }
            ChangeType.Cancelled -> {
                val indexOfComment = newEntries.findIndex(commentEntry)
                when (indexOfComment){
                    -1 -> return
                    0 -> newEntries.removeAt(0)
                    newEntries.lastIndex -> {
                        newEntries.removeAt(newEntries.lastIndex)
                        if (newEntries.last() is DateEntry)
                            newEntries.removeAt(newEntries.lastIndex)
                    }
                    else -> {
                        newEntries.removeAt(indexOfComment)
                        if (newEntries[indexOfComment].type == Type.Date && newEntries[indexOfComment - 1].type == Type.Date)
                            newEntries.removeAt(indexOfComment - 1)
                    }
                }
            }
        }
        publishEntries(ticketEntries, newEntries)
    }

    private fun maybeAddDate(commentEntry: CommentEntry, newEntries: MutableList<TicketEntry>){
        commentEntry.comment.creationDate.getWhen(getApplication(), Calendar.getInstance()).let {
            if (!hasRealComments()
                || newEntries.findLast { entry -> entry.type == Type.Date}
                    ?.let { dateEntry ->
                        (dateEntry as DateEntry).date == it
                    } == false) {

                newEntries.add(DateEntry(commentEntry.comment.creationDate.getWhen(getApplication(), Calendar.getInstance())))
            }
        }
    }

    private fun List<TicketEntry>.findIndex(commentEntry: CommentEntry): Int{
        return findLast {
            (it.type == Type.Comment) && (it as CommentEntry).comment.localId == commentEntry.comment.localId
        }?.let{
            indexOf(it)
        }?: -1
    }

    private fun publishEntries(oldEntries: List<TicketEntry>, newEntries: List<TicketEntry>) {
        ticketEntries = newEntries
        commentDiff.value = DiffResultWithNewItems(
            DiffUtil.calculateDiff(
                CommentsDiffCallback(oldEntries, ticketEntries), false),
            ticketEntries)
    }

    private fun Comment.splitToEntries(): Collection<TicketEntry> {
        if (!this.hasAttachments())
            return listOf(CommentEntry(this, onClickedCallback = this@TicketViewModel))
        val result = mutableListOf<CommentEntry>()
        if (!body.isBlank())
            result.add(CommentEntry(
                Comment(
                    this.commentId,
                    this.body,
                    this.isInbound,
                    null,
                    this.creationDate,
                    this.author,
                    this.localId),
                onClickedCallback = this@TicketViewModel
            ))
        return this.attachments!!.fold(result){
                entriesList, attachment ->
            entriesList.add(CommentEntry(
                Comment(
                    this.commentId,
                    this.body,
                    this.isInbound,
                    listOf(attachment),
                    this.creationDate,
                    this.author,
                    this.localId),
                onClickedCallback = this@TicketViewModel))
            entriesList
        }
    }

}

private class LastCommentIdHolder() {
    var isFromAddComment = false
        private set
    var commentId = 0
        private set

    fun setLastCommentIdFromAddComment(commentId: Int) {
        isFromAddComment = true
        this.commentId = commentId
    }

    fun setLastCommentId(commentId: Int) {
        isFromAddComment = false
        this.commentId = commentId
    }
}

private enum class ChangeType {
    Added,
    Changed,
    Cancelled
}

private enum class CheckCommentError {
    CommentIsEmpty,
    FileSizeExceeded
}
