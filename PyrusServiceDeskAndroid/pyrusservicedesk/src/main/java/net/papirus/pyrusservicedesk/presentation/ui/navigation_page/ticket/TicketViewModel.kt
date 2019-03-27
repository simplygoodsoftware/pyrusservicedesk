package net.papirus.pyrusservicedesk.presentation.ui.navigation_page.ticket

import android.arch.lifecycle.*
import android.arch.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.support.v7.util.DiffUtil
import android.widget.Toast
import com.example.pyrusservicedesk.R
import net.papirus.pyrusservicedesk.PyrusServiceDesk
import net.papirus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries.*
import net.papirus.pyrusservicedesk.presentation.ui.view.recyclerview.DiffResultWithNewItems
import net.papirus.pyrusservicedesk.presentation.usecase.*
import net.papirus.pyrusservicedesk.presentation.viewmodel.ConnectionViewModelBase
import net.papirus.pyrusservicedesk.sdk.data.Attachment
import net.papirus.pyrusservicedesk.sdk.data.Comment
import net.papirus.pyrusservicedesk.sdk.data.EMPTY_TICKET_ID
import net.papirus.pyrusservicedesk.sdk.request.MAX_FILE_SIZE_BYTES
import net.papirus.pyrusservicedesk.sdk.request.MAX_FILE_SIZE_MEGABYTES
import net.papirus.pyrusservicedesk.sdk.updates.OnUnreadTicketCountChangedSubscriber
import net.papirus.pyrusservicedesk.sdk.web.UploadFileHooks
import net.papirus.pyrusservicedesk.utils.ConfigUtils
import net.papirus.pyrusservicedesk.utils.MILLISECONDS_IN_SECOND
import net.papirus.pyrusservicedesk.utils.getWhen
import java.util.*
import kotlin.collections.ArrayList

private const val TICKET_UPDATE_INTERVAL = 30L

internal class TicketViewModel(
        serviceDesk: PyrusServiceDesk,
        arguments: Intent)
    : ConnectionViewModelBase(serviceDesk),
        OnClickedCallback<CommentEntry>,
        OnUnreadTicketCountChangedSubscriber {

    private companion object {

        fun checkComment(comment: Comment): CheckCommentError? {
            return when{
                comment.isEmpty() -> CheckCommentError.CommentIsEmpty
                comment.hasAttachmentWithExceededSize() -> CheckCommentError.FileSizeExceeded
                else -> null
            }
        }

        fun Comment.isEmpty(): Boolean = body.isBlank() && attachments.isNullOrEmpty()

        private fun Comment.hasAttachmentWithExceededSize(): Boolean =
            attachments?.let { it.any { attach -> attach.hasExceededFileSize()} } ?: false

        private fun Attachment.hasExceededFileSize(): Boolean = bytesSize > MAX_FILE_SIZE_BYTES
    }

    val isFeed = serviceDesk.isSingleChat

    private var isCreateTicketSent = false
    private var ticketId: Int = TicketActivity.getTicketId(arguments)

    private val unreadCounter = MutableLiveData<Int>()
    private val commentsRequest = MutableLiveData<Boolean>()
    private val entries = MediatorLiveData<List<Comment>>()
    private val commentDiff = MutableLiveData<DiffResultWithNewItems<TicketEntry>>()

    private var recentTicketEntries: List<TicketEntry> = emptyList()

    private val mainHandler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            mainHandler.postDelayed(this, TICKET_UPDATE_INTERVAL * MILLISECONDS_IN_SECOND)
            commentsRequest.value = true
        }
    }

    init {
        entries.apply{
            if (!isFeed) {
                addSource(
                    Transformations.switchMap(commentsRequest){
                        GetTicketUseCase(this@TicketViewModel, requests, ticketId).execute()
                    }
                ){
                    it?.let { result ->
                        when {
                            result.hasError() -> {  }
                            else -> applyTicketListUpdate(result.data!!)
                        }
                    }
                }
            }
            else {
                addSource(
                    Transformations.switchMap(commentsRequest){
                        GetFeedUseCase(this@TicketViewModel, requests).execute()
                    }
                ){
                    it?.let { result ->
                        when {
                            result.hasError() -> {}
                            else -> applyTicketListUpdate(result.data!!)
                        }
                    }
                }
            }
        }. also { it.observeForever {  } /*should be observed to trigger transformations*/}

        if (!isFeed) {
            unreadCounter.value = TicketActivity.getUnreadTicketsCount(arguments)
        }

        val wantLoadDataOnStart = isFeed || !isNewTicket()
        if (wantLoadDataOnStart && isNetworkConnected.value == true) {
            loadData()
        }
        maybeStartAutoRefresh()
        liveUpdates.subscribeOnUnreadTicketCountChanged(this)
    }

    override fun onLoadData() {
        commentsRequest.value = true
    }

    override fun onUnreadTicketCountChanged(unreadTicketCount: Int) {
        this.unreadCounter.value = unreadTicketCount
    }

    override fun onClicked(item: CommentEntry) {
        if (!item.hasError())
            return
        else {
            applyUpdate(item, ChangeType.Cancelled)
            sendAddComment(item.comment, item.uploadFileHooks)
        }
    }

    override fun onCleared() {
        super.onCleared()
        mainHandler.removeCallbacks(updateRunnable)
        liveUpdates.unsubscribeFromTicketCountChanged(this)
    }

    fun addComment(text: String) {
        val localComment = localDataProvider.newLocalComment(text.trim())
        sendAddComment(localComment)
    }

    fun onAttachmentSelected(attachmentUri: Uri) {
        val localComment = localDataProvider.newLocalComment(fileUri = attachmentUri)
        val fileHooks = UploadFileHooks()
        fileHooks.subscribeOnCancel(CancellationSignal.OnCancelListener {
            applyUpdate(CommentEntry(localComment, onClickedCallback = this), ChangeType.Cancelled)
        })
        sendAddComment(localComment, fileHooks)
    }

    fun getCommentDiffLiveData(): LiveData<DiffResultWithNewItems<TicketEntry>> = commentDiff

    fun getUnreadCounterLiveData(): LiveData<Int> = unreadCounter

    private fun maybeStartAutoRefresh() {
        if (isFeed || !isNewTicket()) {
            mainHandler.post(updateRunnable)
        }
    }

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
            if (comment.hasAttachments())
                acc.addAll(comment.splitToEntriesByFiles())
            else
                acc.add(CommentEntry(comment, onClickedCallback = this@TicketViewModel))
            acc
        }
    }

    private fun sendAddComment(localComment: Comment,
                               uploadFileHooks: UploadFileHooks? = null) {

        when (checkComment(localComment)) {
            CheckCommentError.CommentIsEmpty -> return
            CheckCommentError.FileSizeExceeded -> {
                (getApplication() as Context).run {
                    Toast.makeText(
                        this,
                        this.getString(R.string.psd_file_size_exceeded_message, MAX_FILE_SIZE_MEGABYTES),
                        Toast.LENGTH_SHORT)
                        .show()
                }
                return
            }
        }

        val toNewTicket = !isFeed && isNewTicket() && !isCreateTicketSent

        applyUpdate(
            CommentEntry(localComment, uploadFileHooks = uploadFileHooks, onClickedCallback = this),
            ChangeType.Added
        )
        val call = when {
            toNewTicket -> {
                isCreateTicketSent = true
                CreateTicketUseCase(this, requests, localComment, uploadFileHooks).execute()
            }
            isFeed -> AddFeedCommentUseCase(this, requests, localComment, uploadFileHooks).execute()
            else -> AddCommentUseCase(this, requests, ticketId, localComment, uploadFileHooks).execute()
        }
        val observer = object : Observer<UseCaseResult<Int>> {
            override fun onChanged(res: UseCaseResult<Int>?) {
                res?.let { result ->
                    isCreateTicketSent = false
                    if (uploadFileHooks?.isCancelled == true)
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
                            CommentEntry(
                                localDataProvider.localToServerComment(localComment, commentId),
                                onClickedCallback = this@TicketViewModel
                            )
                        }
                    }
                    applyUpdate(entry, ChangeType.Changed)
                }
                call.removeObserver(this)
            }
        }
        call.observeForever(observer)
    }

    private fun applyTicketListUpdate(freshList: List<Comment>) {
        val listOfLocalEntries = mutableListOf<TicketEntry>()
        if (hasRealComments()) {
            for (i in recentTicketEntries.lastIndex downTo 0) {
                val entry = recentTicketEntries[i]
                if (entry.type == Type.Comment && !(entry as CommentEntry).comment.isLocal())
                    break
                listOfLocalEntries.add(0, entry)
            }
        }
        val toPublish = mutableListOf<TicketEntry>().apply {
            ConfigUtils.getWelcomeMessage()?.let { add(0, WelcomeMessageEntry(it)) }
            addAll(freshList.toTicketEntries())
            addAll(listOfLocalEntries)

        }
        publishEntries(recentTicketEntries, toPublish)
        onDataLoaded()
    }

    private fun hasRealComments(): Boolean = recentTicketEntries.any { it.type == Type.Comment }

    private fun applyUpdate(commentEntry: CommentEntry, changeType: ChangeType) {
        val newEntries = recentTicketEntries.toMutableList()
        fun maybeAddDate(){
            commentEntry.comment.creationDate.getWhen(getApplication(), Calendar.getInstance()).let {
                if (!hasRealComments()
                    || newEntries.findLast { entry -> entry.type == Type.Date}
                        ?.let { dateEntry ->  (dateEntry as DateEntry).date == it } == false) {

                    newEntries.add(DateEntry(commentEntry.comment.creationDate.getWhen(getApplication(), Calendar.getInstance())))
                }
            }
        }
        fun findIndex(): Int{
            return newEntries.findLast {
                (it.type == Type.Comment) && (it as CommentEntry).comment.localId == commentEntry.comment.localId
            }?.let{
                newEntries.indexOf(it)
            }?: -1
        }
        when (changeType) {
            ChangeType.Added -> {
                maybeAddDate()
                newEntries.add(commentEntry)
            }
            ChangeType.Changed -> {
                maybeAddDate()
                findIndex().let {
                    if (it != -1)
                        newEntries[it] = commentEntry
                    else
                        newEntries.add(commentEntry)
                }
            }
            ChangeType.Cancelled -> {
                findIndex().let {
                    when (it){
                        -1 -> return@let
                        0 -> newEntries.removeAt(0)
                        newEntries.lastIndex -> {
                            newEntries.removeAt(newEntries.lastIndex)
                            if (newEntries.last() is DateEntry)
                                newEntries.removeAt(newEntries.lastIndex)
                        }
                        else -> {
                            newEntries.removeAt(it)
                            if (newEntries[it].type == Type.Date && newEntries[it - 1].type == Type.Date)
                                newEntries.removeAt(it - 1)
                        }
                    }

                }
            }
        }
        publishEntries(recentTicketEntries, newEntries)
    }

    private fun publishEntries(oldEntries: List<TicketEntry>, newEntries: List<TicketEntry>) {
        recentTicketEntries = newEntries
        commentDiff.value = DiffResultWithNewItems(
            DiffUtil.calculateDiff(
                CommentsDiffCallback(oldEntries, recentTicketEntries), false),
            recentTicketEntries)
    }

    private fun Comment.splitToEntriesByFiles(): Collection<TicketEntry> {
        if (this.attachments.isNullOrEmpty())
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
        return this.attachments.fold(result){
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

private enum class ChangeType {
    Added,
    Changed,
    Cancelled
}

private enum class CheckCommentError {
    CommentIsEmpty,
    FileSizeExceeded
}
