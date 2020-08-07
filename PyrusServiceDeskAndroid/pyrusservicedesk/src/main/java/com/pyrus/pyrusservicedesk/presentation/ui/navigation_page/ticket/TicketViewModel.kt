package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.ServiceDeskProvider
import com.pyrus.pyrusservicedesk.presentation.call.*
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.attached_files.AttachmentEntry
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.attached_files.AttachmentsDiffCallback
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.attached_files.ImageEntry
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.attached_files.TextEntry
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries.*
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.DiffResultWithNewItems
import com.pyrus.pyrusservicedesk.presentation.viewmodel.ConnectionViewModelBase
import com.pyrus.pyrusservicedesk.sdk.data.Attachment
import com.pyrus.pyrusservicedesk.sdk.data.Comment
import com.pyrus.pyrusservicedesk.sdk.data.EMPTY_TICKET_ID
import com.pyrus.pyrusservicedesk.sdk.data.LocalDataProvider
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.AddCommentResponseData
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.Comments
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.CreateTicketResponseData
import com.pyrus.pyrusservicedesk.sdk.response.PendingDataError
import com.pyrus.pyrusservicedesk.sdk.updates.OnUnreadTicketCountChangedSubscriber
import com.pyrus.pyrusservicedesk.sdk.verify.LocalDataVerifier
import com.pyrus.pyrusservicedesk.sdk.web.OnCancelListener
import com.pyrus.pyrusservicedesk.sdk.web.UploadFileHooks
import com.pyrus.pyrusservicedesk.utils.ConfigUtils
import com.pyrus.pyrusservicedesk.utils.MILLISECONDS_IN_SECOND
import com.pyrus.pyrusservicedesk.utils.RequestUtils.Companion.MAX_FILE_SIZE_BYTES
import com.pyrus.pyrusservicedesk.utils.RequestUtils.Companion.MAX_FILE_SIZE_MEGABYTES
import com.pyrus.pyrusservicedesk.utils.getWhen
import com.pyrus.pyrusservicedesk.utils.isImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList

/**
 * ViewModel for the ticket screen.
 *
 * [isFeed] Denotes whether [PyrusServiceDesk.isSingleChat] is enabled.
 */
internal class TicketViewModel(serviceDeskProvider: ServiceDeskProvider,
                               arguments: Intent,
                               val isFeed: Boolean)
    : ConnectionViewModelBase(serviceDeskProvider),
        OnUnreadTicketCountChangedSubscriber {

    private companion object {

        const val TICKET_UPDATE_INTERVAL = 30L

        fun Comment.hasAttachmentWithExceededSize(): Boolean =
            attachments?.let { it.any { attach -> attach.hasExceededFileSize() } } ?: false

        fun Attachment.hasExceededFileSize(): Boolean = bytesSize > MAX_FILE_SIZE_BYTES
    }

    /**
     * Drafted text. Assigned once when view model is created.
     */
    val draft: String

    private val draftRepository = serviceDeskProvider.getDraftRepository()
    private val localDataProvider: LocalDataProvider = serviceDeskProvider.getLocalDataProvider()
    private val localDataVerifier: LocalDataVerifier = serviceDeskProvider.getLocalDataVerifier()

    private var isCreateTicketSent = false
    private var ticketId: Int = TicketActivity.getTicketId(arguments)

    private val unreadCounter = MutableLiveData<Int>()
    private val commentDiff = MutableLiveData<DiffResultWithNewItems<TicketEntry>>()

    private var ticketEntries: List<TicketEntry> = emptyList()

    private val attachmentDiff = MutableLiveData<DiffResultWithNewItems<AttachmentEntry>>()
    private var attachmentEntries: List<AttachmentEntry> = emptyList()

    private val mainHandler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            mainHandler.postDelayed(this, TICKET_UPDATE_INTERVAL * MILLISECONDS_IN_SECOND)
            update()
        }
    }

    private var pendingCommentUnderAction: CommentEntry? = null

    init {
        draft = draftRepository.getDraft()

        if (!isFeed) {
            unreadCounter.value = TicketActivity.getUnreadTicketsCount(arguments)
        } else {
            runBlocking {
                val response = requests.getPendingFeedCommentsRequest().execute()
                if (!response.hasError() && !response.getData()?.comments.isNullOrEmpty()) {
                    applyTicketUpdate(response.getData()!!, true)
                }
            }
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
        if (text.isNotBlank()) {
            val localComment = localDataProvider.createLocalComment(text.trim())
            sendAddComment(localComment)
        }
        if (attachmentEntries.isNotEmpty()) {
            for (attachmentEntry in attachmentEntries) {
                val attachment = when (attachmentEntry) {
                    is ImageEntry -> attachmentEntry.attachment
                    is TextEntry -> attachmentEntry.attachment
                    else -> null
                }?: continue

                val localComment = localDataProvider.createLocalComment(fileUri = attachment.localUri)
                val fileHooks = UploadFileHooks()
                fileHooks.subscribeOnCancel(object : OnCancelListener {
                    override fun onCancel() {
                        return applyCommentUpdate(
                            CommentEntry(localComment), CommentChangeType.Cancelled
                        )
                    }
                })
                sendAddComment(localComment, fileHooks)

                onAttachmentRemoved(attachmentEntry)
            }
        }
    }

    /**
     * Callback to be invoked when user picked file to send.
     *
     * @param attachmentUri URI of the file to be sent
     */
    fun onAttachmentSelected(attachmentUri: Uri) {
        val attachment = localDataProvider.createLocalAttachmentFromUri(attachmentUri)
        val entryToAdd = when {
            attachment.name.isImage() -> ImageEntry(attachment)
            else -> TextEntry(attachment)
        }

        entryToAdd.let {
            launch {
                withContext(Dispatchers.Main) {
                    applyAttachmentUpdate(it, AttachmentChangeType.Added)
                }
            }
        }
    }

    /**
     * Callback to be invoked when user picked file to send.
     *

     */
    fun onAttachmentRemoved(entryToRemove : AttachmentEntry) {
        entryToRemove.let {
            applyAttachmentUpdate(it, AttachmentChangeType.Removed)
            pendingCommentUnderAction = null
        }
    }

    /**
     * Provides live data that delivers [DiffResultWithNewItems] which contains list of
     * current [TicketEntry]s and [DiffUtil.DiffResult] that is used for correctly apply
     * changes to UI.
     */
    fun getCommentDiffLiveData(): LiveData<DiffResultWithNewItems<TicketEntry>> = commentDiff

    /**

     */
    fun getAttachmentDiffLiveData(): LiveData<DiffResultWithNewItems<AttachmentEntry>> = attachmentDiff

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

    /**
     * Callback to be invoked when user chooses to retry sending pending comment
     */
    fun onPendingCommentRetried() {
        pendingCommentUnderAction?.let { comment ->
            applyCommentUpdate(comment, CommentChangeType.Cancelled)
            sendAddComment(comment.comment, comment.uploadFileHooks.also { it?.resetProgress() })
        }
        pendingCommentUnderAction = null
    }

    /**
     * Callback to be invoked when user chooses to delete pending comment
     */
    fun onPendingCommentDeleted() {
        pendingCommentUnderAction?.let {
            launch {
                if (!requests.getRemovePendingCommentRequest(it.comment).execute().hasError()) {
                    withContext(Dispatchers.Main) {
                        applyCommentUpdate(it, CommentChangeType.Cancelled)
                        pendingCommentUnderAction = null
                    }
                }
            }
        }
    }

    /**
     * Callback to be invoked when user decided not to perform any action on a pending comment
     */
    fun onChoosingCommentActionCancelled() {
        pendingCommentUnderAction = null
    }

    /**
     * Callback to be invoked when user starts choosing action on given pending [commentEntry]
     */
    fun onUserStartChoosingCommentAction(commentEntry: CommentEntry) {
        pendingCommentUnderAction = commentEntry
    }

    private fun update() {
        val call = when {
            isFeed -> GetFeedCall(this@TicketViewModel, requests).execute()
            else -> GetTicketCall(this@TicketViewModel, requests, ticketId).execute()
        }
        val observer = Observer<CallResult<Comments>> { result ->
            if (result == null)
                return@Observer
            when {
                result.hasError() -> {
                }
                else -> applyTicketUpdate(result.data!!, false)
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
        return foldIndexed(ArrayList(size)) { index, acc, comment ->
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

    private fun sendAddComment(
        localComment: Comment,
        uploadFileHooks: UploadFileHooks? = null
    ) {

        if (commentContainsError(localComment))
            return

        val toNewTicket = !isFeed && isNewTicket() && !isCreateTicketSent

        applyCommentUpdate(
            CommentEntry(localComment, uploadFileHooks = uploadFileHooks),
            CommentChangeType.Added
        )
        when {
            toNewTicket -> {
                isCreateTicketSent = true
                CreateTicketCall(this, requests, localComment, uploadFileHooks)
                    .execute()
                    .observeForever(CreateTicketObserver(uploadFileHooks, localComment))
            }
            isFeed ->
                AddFeedCommentCall(this, requests, localComment, uploadFileHooks)
                    .execute()
                    .observeForever(AddCommentObserver(uploadFileHooks, localComment))
            else ->
                AddCommentCall(this, requests, ticketId, localComment, uploadFileHooks)
                    .execute()
                    .observeForever(AddCommentObserver(uploadFileHooks, localComment))
        }
    }

    private fun hasComment(commentId: Int): Boolean {
        return ticketEntries.findLast {
            it.type == Type.Comment && (it as CommentEntry).comment.commentId == commentId
        } != null
    }

    private fun commentContainsError(localComment: Comment): Boolean {
        val commentError = when{
            localDataVerifier.isLocalCommentEmpty(localComment) -> CheckCommentError.CommentIsEmpty
            localComment.hasAttachmentWithExceededSize() -> CheckCommentError.FileSizeExceeded
            else -> null
        }
        when (commentError) {
            CheckCommentError.CommentIsEmpty -> return true
            CheckCommentError.FileSizeExceeded -> {
                (getApplication() as Context).run {
                    Toast.makeText(
                        this,
                        this.getString(R.string.psd_file_size_exceeded_message, MAX_FILE_SIZE_MEGABYTES),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
                return true
            }
        }
        return false
    }

    private fun applyTicketUpdate(freshList: Comments, arePendingComments: Boolean) {
        if (!arePendingComments && !needUpdateCommentsList(freshList.comments)) {
            onDataLoaded()
            return
        }
        val listOfLocalEntries = mutableListOf<TicketEntry>()
        if (hasRealComments()) {
            for (i in ticketEntries.lastIndex downTo 0) {
                val entry = ticketEntries[i]
                if (entry.type == Type.Comment
                    && (entry as CommentEntry).comment.isLocal()
                ) {

                    listOfLocalEntries.add(0, entry)
                }
            }
        }
        val toPublish = mutableListOf<TicketEntry>().apply {
            ConfigUtils.getWelcomeMessage()?.let { add(0, WelcomeMessageEntry(it)) }
            addAll(freshList.comments.toTicketEntries())
            listOfLocalEntries.forEach {
                maybeAddDate(it as CommentEntry, this)
                add(it)
            }
            if (freshList.showRating)
                add(RatingEntry())
        }
        publishEntries(ticketEntries, toPublish)
        if (!arePendingComments)
            onDataLoaded()
    }

    private fun needUpdateCommentsList(freshList: List<Comment>): Boolean {
        if (freshList.isEmpty()) {
            return !hasRealComments()
        }
        if (!hasRealComments())
            return true
        val iterator = ticketEntries.asReversed().iterator()
        for (i in freshList.lastIndex downTo 0) {
            val serverId = freshList[i].commentId
            loop@ while (iterator.hasNext()) {
                val entry = iterator.next()
                if (entry.type != Type.Comment)
                    continue
                entry as CommentEntry
                return when {
                    entry.comment.isLocal() -> continue@loop
                    entry.comment.commentId > serverId -> false
                    entry.comment.commentId < serverId -> true
                    else -> break@loop
                }
            }
            if (!iterator.hasNext())
                break
        }
        return true
    }

    private fun hasRealComments(): Boolean = ticketEntries.any { it.type == Type.Comment }

    private fun applyCommentUpdate(commentEntry: CommentEntry, changeType: CommentChangeType) {
        val newEntries = ticketEntries.toMutableList()
        when (changeType) {
            CommentChangeType.Added -> {
                maybeAddDate(commentEntry, newEntries)
                newEntries.add(commentEntry)
            }
            CommentChangeType.Changed -> {
                maybeAddDate(commentEntry, newEntries)
                newEntries.findIndex(commentEntry).let {
                    when (it) {
                        -1 -> newEntries.add(commentEntry)
                        else -> newEntries[it] = commentEntry
                    }
                }
            }
            CommentChangeType.Cancelled -> {
                when (val indexOfComment = newEntries.findIndex(commentEntry)) {
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
        newEntries.removeAll { it.type == Type.Rating }
        publishEntries(ticketEntries, newEntries)
    }

    private fun applyAttachmentUpdate(attachmentEntry: AttachmentEntry, changeType: AttachmentChangeType) {
        val newEntries = attachmentEntries.toMutableList()
        when (changeType) {
            AttachmentChangeType.Added -> newEntries.add(attachmentEntry)

            AttachmentChangeType.Removed -> newEntries.remove(attachmentEntry)
        }
        publishAttachmentEntries(attachmentEntries, newEntries)
    }

    private fun maybeAddDate(commentEntry: CommentEntry, newEntries: MutableList<TicketEntry>) {
        commentEntry.comment.creationDate.getWhen(getApplication(), Calendar.getInstance()).let {
            if (!hasRealComments()
                || newEntries.findLast { entry -> entry.type == Type.Date }
                    ?.let { dateEntry ->
                        (dateEntry as DateEntry).date == it
                    } == false
            ) {

                newEntries.add(
                    DateEntry(
                        commentEntry.comment.creationDate.getWhen(
                            getApplication(),
                            Calendar.getInstance()
                        )
                    )
                )
            }
        }
    }

    private fun List<TicketEntry>.findIndex(commentEntry: CommentEntry): Int {
        return findLast {
            (it.type == Type.Comment) && (it as CommentEntry).comment.localId == commentEntry.comment.localId
        }?.let {
            indexOf(it)
        } ?: -1
    }

    private fun publishEntries(oldEntries: List<TicketEntry>, newEntries: List<TicketEntry>) {
        ticketEntries = newEntries
        commentDiff.value = DiffResultWithNewItems(
            DiffUtil.calculateDiff(
                CommentsDiffCallback(oldEntries, ticketEntries), false
            ),
            ticketEntries
        )
    }

    private fun publishAttachmentEntries(oldEntries: List<AttachmentEntry>, newEntries: List<AttachmentEntry>) {
        attachmentEntries = newEntries
        attachmentDiff.value = DiffResultWithNewItems(
            DiffUtil.calculateDiff(
                AttachmentsDiffCallback(oldEntries, attachmentEntries), false
            ),
            attachmentEntries
        )
    }

    private fun Comment.splitToEntries(): Collection<TicketEntry> {
        val pendingError = when{
            isLocal() -> PendingDataError()
            else -> null
        }
        if (!this.hasAttachments())
            return listOf(CommentEntry(this, error = pendingError))
        val result = mutableListOf<CommentEntry>()
        if (body?.isBlank() == false)
            result.add(
                CommentEntry(
                    Comment(
                        this.commentId,
                        this.body,
                        this.isInbound,
                        null,
                        this.creationDate,
                        this.author,
                        this.localId),
                    error = pendingError)
            )
        return this.attachments!!
            .fold(result){ entriesList, attachment ->
                entriesList.add(
                    CommentEntry(
                        Comment(
                            this.commentId,
                            this.body,
                            this.isInbound,
                            listOf(attachment),
                            this.creationDate,
                            this.author,
                            this.localId),
                        error = pendingError)
                )
                entriesList
            }
    }

    fun onRatingClick(rating: Int) =
        sendAddComment(localDataProvider.createLocalComment(rating = rating))

    private inner class AddCommentObserver(
        uploadFileHooks: UploadFileHooks?,
        localComment: Comment
    ) : AddCommentObserverBase<CallResult<AddCommentResponseData>, AddCommentResponseData>(
        uploadFileHooks,
        localComment
    ) {

        override fun getAttachments(data: AddCommentResponseData) = data.sentAttachments

        override fun getCommentId(data: AddCommentResponseData): Int = data.commentId

        override fun onSuccess(data: AddCommentResponseData) {}

    }

    private inner class CreateTicketObserver(
        uploadFileHooks: UploadFileHooks?,
        localComment: Comment
    ) : AddCommentObserverBase<CallResult<CreateTicketResponseData>, CreateTicketResponseData>(
        uploadFileHooks,
        localComment
    ) {

        override fun getAttachments(data: CreateTicketResponseData) = data.sentAttachments

        override fun getCommentId(data: CreateTicketResponseData): Int {
            return localComment.localId
        }

        override fun onSuccess(data: CreateTicketResponseData) {
            ticketId = data.ticketId
            maybeStartAutoRefresh()
        }
    }

    private abstract inner class AddCommentObserverBase<T : CallResult<U>, U>(
        val uploadFileHooks: UploadFileHooks?,
        val localComment: Comment
    ) : Observer<T> {

        override fun onChanged(t: T?) {
            t?.let { result ->
                isCreateTicketSent = false
                if (uploadFileHooks?.isCancelled == true)
                    return@let
                val entry = when {
                    result.hasError() -> CommentEntry(
                        localComment,
                        uploadFileHooks, // for retry purpose
                        error = result.error
                    )
                    else -> {
                        onSuccess(result.data!!)
                        val commentId = getCommentId(result.data)
                        val attachments = getAttachments(result.data)
                        if (hasComment(commentId))
                            return@let
                        CommentEntry(
                            localDataProvider.convertLocalCommentToServer(localComment, commentId, attachments)
                        )
                    }
                }
                applyCommentUpdate(entry, CommentChangeType.Changed)
            }
        }

        abstract fun getAttachments(data: U): List<Attachment>?

        abstract fun getCommentId(data: U): Int

        abstract fun onSuccess(data: U)

    }

    private enum class CommentChangeType {
        Added,
        Changed,
        Cancelled
    }

    private enum class AttachmentChangeType {
        Added,
        Removed
    }

    private enum class CheckCommentError {
        CommentIsEmpty,
        FileSizeExceeded
    }
}
