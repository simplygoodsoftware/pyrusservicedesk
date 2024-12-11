package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket

import android.app.Application
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.entries.ButtonsEntry
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.entries.CommentEntry
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.entries.DateEntry
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.entries.RatingEntry
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.entries.TicketEntry
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.entries.Type
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.entries.WelcomeMessageEntry
import com.pyrus.pyrusservicedesk._ref.utils.log.PLog
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.DiffResultWithNewItems
import com.pyrus.pyrusservicedesk.presentation.viewmodel.ConnectionViewModelBase
import com.pyrus.pyrusservicedesk.sdk.data.Attachment
import com.pyrus.pyrusservicedesk.sdk.data.Author
import com.pyrus.pyrusservicedesk.sdk.data.Comment
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.Comments
import com.pyrus.pyrusservicedesk.sdk.response.PendingDataError
import com.pyrus.pyrusservicedesk.sdk.updates.OnUnreadTicketCountChangedSubscriber
import com.pyrus.pyrusservicedesk.sdk.updates.PreferencesManager
import com.pyrus.pyrusservicedesk.sdk.web.OnCancelListener
import com.pyrus.pyrusservicedesk.sdk.web.UploadFileHooks
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk._ref.utils.MILLISECONDS_IN_MINUTE
import com.pyrus.pyrusservicedesk._ref.utils.MILLISECONDS_IN_SECOND
import com.pyrus.pyrusservicedesk._ref.utils.RequestUtils.Companion.MAX_FILE_SIZE_BYTES
import com.pyrus.pyrusservicedesk._ref.utils.getWhen
import kotlinx.coroutines.*
import java.lang.Runnable
import java.util.*
import kotlin.collections.ArrayList

/**
 * ViewModel for the ticket screen.
 */
internal class TicketViewModel(
    application: Application,
    private val preferencesManager: PreferencesManager
) : ConnectionViewModelBase(application), OnUnreadTicketCountChangedSubscriber {

    private companion object {

        private val TAG = TicketViewModel::class.java.simpleName

        private const val BUTTON_PATTERN = "<button>(.*?)</button>"

        fun Comment.hasAttachmentWithExceededSize(): Boolean =
            attachments?.let { it.any { attach -> attach.hasExceededFileSize() } } ?: false

        fun Attachment.hasExceededFileSize(): Boolean = bytesSize > MAX_FILE_SIZE_BYTES
    }

    /**
     * Drafted text. Assigned once when view model is created.
     */
//    val draft: String

//    private val draftRepository = serviceDeskProvider.getDraftRepository()
//    private val localDataProvider: LocalDataProvider = serviceDeskProvider.getLocalDataProvider()
//    private val fileManager: FileManager = serviceDeskProvider.getFileManager()
//    private val localDataVerifier: LocalDataVerifier = serviceDeskProvider.getLocalDataVerifier()

    private var isCreateTicketSent = false

    private val unreadCounter = MutableLiveData<Int>()
    private val commentDiff = MutableLiveData<DiffResultWithNewItems<TicketEntry>>()

    private var ticketEntries: List<TicketEntry> = emptyList()

    private val mainHandler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            update()
            val interval = getFeedUpdateInterval()
            currentInterval = interval
            mainHandler.postDelayed(this, interval)
        }
    }

    private var pendingCommentUnderAction: CommentEntry? = null
    private var userId = PyrusServiceDesk.get().userId

    private var currentInterval: Long = 0

    init {
//        draft = draftRepository.getDraft()

        runBlocking {
//            val response = requests.getPendingFeedCommentsRequest().execute()
//            if (!response.hasError() && !response.getData()?.comments.isNullOrEmpty()) {
//                applyTicketUpdate(response.getData()!!, true)
//            }
        }

        if (isNetworkConnected.value == true) {
            loadData()
        }
        maybeStartAutoRefresh()
//        liveUpdates.subscribeOnUnreadTicketCountChanged(this)
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
//        liveUpdates.unsubscribeFromTicketCountChanged(this)
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
        update()
//        val localComment = localDataProvider.createLocalComment(text.trim())
//        sendAddComment(localComment)
    }

    /**
     * Callback to be invoked when user picked file to send.
     *
     * @param attachmentUri URI of the file to be sent
     */
    fun onAttachmentSelected(attachmentUri: Uri) {
       launch {
//           val fileUri = try {
//               fileManager.copyFile(attachmentUri)
//           } catch (e: Exception) {
//               return@launch
//           }
//           withContext(Dispatchers.Main) {
//               val localComment = localDataProvider.createLocalComment(fileUri = fileUri)
//               sendAddComment(localComment)
//           }
       }
    }

    /**
     * Provides live data that delivers [DiffResultWithNewItems] which contains list of
     * current [TicketEntry]s and [DiffUtil.DiffResult] that is used for correctly apply
     * changes to UI.
     */
    fun getCommentDiffLiveData(): LiveData<DiffResultWithNewItems<TicketEntry>> = commentDiff

    /**
     * Callback to be invoked when user input changed.
     */
    fun onInputTextChanged(text: String) {
//        draftRepository.saveDraft(text)
    }

    /**
     * Callback to be invoked when user chooses to retry sending pending comment
     */
    fun onPendingCommentRetried() {
        pendingCommentUnderAction?.let { comment ->
            applyCommentUpdate(comment, ChangeType.Cancelled)
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
//                if (!requests.getRemovePendingCommentRequest(it.comment).execute().hasError()) {
//                    withContext(Dispatchers.Main) {
//                        applyCommentUpdate(it, ChangeType.Cancelled)
//                        pendingCommentUnderAction = null
//                    }
//                }
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
//        val call = GetFeedCall(this@TicketViewModel, requests).execute()
//        val observer = Observer<CallResult<Comments>> { result ->
//            if (result == null)
//                return@Observer
//            when {
//                result.hasError() -> {
//                }
//                else -> applyTicketUpdate(result.data!!, false)
//            }
//        }
//        call.observeForever(observer)
    }

    private fun maybeStartAutoRefresh() {
        mainHandler.post(updateRunnable)
    }

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
            if (comment.isLocal() || !comment.body.isNullOrEmpty() || !comment.attachments.isNullOrEmpty() || comment.rating != null) {
                acc.addAll(comment.splitToEntries())
            }
            acc
        }
    }

    private fun sendAddComment(
        localComment: Comment,
        uploadHooks: UploadFileHooks? = null
    ) {

        if (commentContainsError(localComment)) {
            return
        }

        val uploadFileHooks: UploadFileHooks?
        if (uploadHooks != null) {
            uploadFileHooks = uploadHooks
        }
        else if (!localComment.hasAttachments()) {
            uploadFileHooks = null
        }
        else {
            uploadFileHooks = UploadFileHooks()
            uploadFileHooks.subscribeOnCancel(object : OnCancelListener {
                override fun onCancel() {
                    return applyCommentUpdate(
                        CommentEntry(localComment), ChangeType.Cancelled
                    )
                }
            })
        }

        applyCommentUpdate(
            CommentEntry(localComment, uploadFileHooks = uploadFileHooks),
            ChangeType.Added
        )
//        AddFeedCommentCall(this, requests, localComment, uploadFileHooks)
//            .execute()
//            .observeForever(AddCommentObserver(uploadFileHooks, localComment))

        val lastActiveTime = System.currentTimeMillis()
        PLog.d(TAG, "sendAddComment, lastActiveTime: $lastActiveTime, commentLocalId: ${localComment.localId}")
        PyrusServiceDesk.startTicketsUpdatesIfNeeded(lastActiveTime)
        updateFeedIntervalIfNeeded()
    }

    private fun hasComment(commentId: Long): Boolean {
        return ticketEntries.findLast {
            it.type == Type.Comment && (it as CommentEntry).comment.commentId == commentId
        } != null
    }

    private fun commentContainsError(localComment: Comment): Boolean {
        val commentError = when{
//            localDataVerifier.isLocalCommentEmpty(localComment) -> CheckCommentError.CommentIsEmpty
            localComment.hasAttachmentWithExceededSize() -> CheckCommentError.FileSizeExceeded
            else -> null
        }
//        when (commentError) {
//            CheckCommentError.CommentIsEmpty -> return true
//            CheckCommentError.FileSizeExceeded -> {
//                (getApplication() as Context).run {
//                    Toast.makeText(
//                        this,
//                        this.getString(R.string.psd_file_size_exceeded_message, MAX_FILE_SIZE_MEGABYTES),
//                        Toast.LENGTH_SHORT
//                    )
//                        .show()
//                }
//                return true
//            }
//        }
        return false
    }

    private fun applyTicketUpdate(freshList: Comments, arePendingComments: Boolean) {
//        liveUpdates.onReadComments()
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
            val freshComments = ArrayList<Comment>()
            val welcomeMessage = ConfigUtils.getWelcomeMessage()
            if (welcomeMessage != null) {
                val firstComment = freshList.comments.firstOrNull()
                val welcomeCommentDate = firstComment?.creationDate ?: Date().apply { time = System.currentTimeMillis() }
                val welcomeComment = Comment(
                    body = welcomeMessage,
                    creationDate = welcomeCommentDate,
                    author = Author(""),
//                    isWelcomeMessage = true,
                )
                freshComments += welcomeComment
            }
            freshComments += freshList.comments

            addAll(freshComments.toTicketEntries())
            listOfLocalEntries.forEach {
                maybeAddDate(it as CommentEntry, this)
                add(it)
            }

            if (freshList.showRatingText.isNotBlank()) {
                add(WelcomeMessageEntry(freshList.showRatingText))
            }
            if (freshList.showRating) {
                add(RatingEntry())
            }
        }
        publishEntries(ticketEntries, toPublish)
        if (!arePendingComments)
            onDataLoaded()
    }

    private fun needUpdateCommentsList(freshList: List<Comment>): Boolean {
        if (userId != PyrusServiceDesk.get().userId) {
            userId = PyrusServiceDesk.get().userId
            return true
        }
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
                    when (it) {
                        -1 -> newEntries.add(commentEntry)
                        else -> newEntries[it] = commentEntry
                    }
                }
            }
            ChangeType.Cancelled -> {
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

    // buttons are displayed in other entries, so if comment contains nothing but buttons we don't need it
    private fun removeEmptyComments(entries: List<TicketEntry>): List<TicketEntry> {
        return entries.filterNot {
            it is CommentEntry
                    && it.comment.attachments.isNullOrEmpty()
                    && HtmlTagUtils.cleanTags(it.comment.body ?: "").isBlank()
        }
    }

    private fun addButtonsIfNeeded(newEntries: List<TicketEntry>): List<TicketEntry> {
        if (newEntries.isEmpty()) {
            return newEntries
        }

        val commentWithButtons = newEntries.last()

        if (commentWithButtons !is CommentEntry) {
            return newEntries
        }

        val buttons = HtmlTagUtils.extractButtons(commentWithButtons.comment)
        if (buttons.isEmpty()) {
            return newEntries
        }

        return newEntries + ButtonsEntry(buttons) {
            onSendClicked(it)
        }
    }

    private fun removeOldButtons(newEntries: List<TicketEntry>): List<TicketEntry> {
        return newEntries.filter { it.type != Type.Buttons }
    }

    private fun publishEntries(oldEntries: List<TicketEntry>, newEntries: List<TicketEntry>) {
        ticketEntries = removeEmptyComments(addButtonsIfNeeded(removeOldButtons(newEntries)))

        val lastActiveTime = (newEntries.findLast {
            it is CommentEntry && it.comment.isInbound
        } as CommentEntry?)?.comment?.creationDate?.time ?: -1
        PLog.d(TAG, "publishEntries, lastActiveTime: $lastActiveTime, newEntries count: ${newEntries.size}")
        PyrusServiceDesk.startTicketsUpdatesIfNeeded(lastActiveTime)
        updateFeedIntervalIfNeeded()

//        commentDiff.value = DiffResultWithNewItems(
//            DiffUtil.calculateDiff(
//                CommentsItemCallback(oldEntries, ticketEntries), false
//            ),
//            ticketEntries
//        )
    }

    private fun Comment.splitToEntries(): Collection<TicketEntry> {
        val pendingError = when{
            isLocal() -> PendingDataError()
            else -> null
        }
        if (!this.hasAttachments())
            return listOf(CommentEntry(this, error = pendingError))
        val result = mutableListOf<CommentEntry>()
        val commentBody = body ?: ""
        if (commentBody.isNotBlank()) {
            result.add(
                CommentEntry(
                    Comment(
                        this.commentId,
                        commentBody,
                        this.isInbound,
                        null,
                        this.creationDate,
                        this.author,
                        this.localId
                    ),
                    error = pendingError
                )
            )
        }
        return this.attachments!!.fold(result) { entriesList, attachment ->
            entriesList.add(
                CommentEntry(
                    Comment(
                        this.commentId,
                        this.body,
                        this.isInbound,
                        listOf(attachment),
                        this.creationDate,
                        this.author,
                        this.localId
                    ),
                    error = pendingError
                )
            )
            entriesList
        }
    }

//    fun onRatingClick(rating: Int) =
//        sendAddComment(localDataProvider.createLocalComment(rating = rating))

//    fun onStart() {
//        liveUpdates.increaseActiveScreenCount()
//    }

//    fun onStop() {
//        liveUpdates.decreaseActiveScreenCount()
//    }

//    private inner class AddCommentObserver(
//        uploadFileHooks: UploadFileHooks?,
//        localComment: Comment
//    ) : AddCommentObserverBase<CallResult<AddCommentResponseData>, AddCommentResponseData>(
//        uploadFileHooks,
//        localComment
//    ) {
//        override fun getAttachments(data: AddCommentResponseData) = data.sentAttachments
//
//        override fun getCommentId(data: AddCommentResponseData): Long = data.commentId
//
//        override fun onSuccess(data: AddCommentResponseData) {
////            liveUpdates.subscribeOnUnreadTicketCountChanged(this@TicketViewModel)
//        }
//
//    }


//    private abstract inner class AddCommentObserverBase<T : CallResult<U>, U>(
//        val uploadFileHooks: UploadFileHooks?,
//        val localComment: Comment
//    ) : Observer<T> {
//        override fun onChanged(t: T?) {
//            t?.let { result ->
//                isCreateTicketSent = false
//                if (uploadFileHooks?.isCancelled == true)
//                    return@let
//                val entry = when {
//                    result.hasError() -> CommentEntry(
//                        localComment,
//                        uploadFileHooks, // for retry purpose
//                        error = result.error
//                    )
//                    else -> {
//                        onSuccess(result.data!!)
//                        val commentId = getCommentId(result.data)
//                        val attachments = getAttachments(result.data)
//                        if (hasComment(commentId))
//                            return@let
//                        CommentEntry(
//                            localDataProvider.convertLocalCommentToServer(localComment, commentId, attachments)
//                        )
//                    }
//                }
//                applyCommentUpdate(entry, ChangeType.Changed)
//            }
//        }
//
//        abstract fun getAttachments(data: U): List<Attachment>?
//
//        abstract fun getCommentId(data: U): Long
//
//        abstract fun onSuccess(data: U)
//
//    }

    private fun updateFeedIntervalIfNeeded() {
        val interval = getFeedUpdateInterval()
        if (currentInterval == interval)
            return
        mainHandler.removeCallbacks(updateRunnable)
        mainHandler.post(updateRunnable)
    }

    private fun getFeedUpdateInterval(): Long {
        val diff = System.currentTimeMillis() - preferencesManager.getLastActiveTime()
        return when {
            diff < 1.5 * MILLISECONDS_IN_MINUTE -> 5L * MILLISECONDS_IN_SECOND
            diff < 5 * MILLISECONDS_IN_MINUTE -> 15L * MILLISECONDS_IN_SECOND
            else -> MILLISECONDS_IN_MINUTE.toLong()
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

}
