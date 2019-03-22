package net.papirus.pyrusservicedesk.presentation.ui.navigation_page.ticket

import android.arch.lifecycle.*
import android.arch.lifecycle.Observer
import android.content.Intent
import android.net.Uri
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.support.v7.util.DiffUtil
import net.papirus.pyrusservicedesk.PyrusServiceDesk
import net.papirus.pyrusservicedesk.UnreadCounterChangedSubscriber
import net.papirus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries.*
import net.papirus.pyrusservicedesk.presentation.ui.view.recyclerview.DiffResultWithNewItems
import net.papirus.pyrusservicedesk.presentation.usecase.*
import net.papirus.pyrusservicedesk.presentation.viewmodel.ConnectionViewModelBase
import net.papirus.pyrusservicedesk.sdk.data.Comment
import net.papirus.pyrusservicedesk.sdk.data.EMPTY_TICKET_ID
import net.papirus.pyrusservicedesk.sdk.web.UploadFileHooks
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
        UnreadCounterChangedSubscriber {

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
                    applyTicketListUpdate(it?.data!!)
                }
            }
            else {
                addSource(
                    Transformations.switchMap(commentsRequest){
                        GetFeedUseCase(this@TicketViewModel, requests).execute()
                    }
                ){
                    applyTicketListUpdate(it?.data!!)
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
    }

    override fun onLoadData() {
        commentsRequest.value = true
    }

    override fun onUnreadCounterChanged(unreadCounter: Int) {
        this.unreadCounter.value = unreadCounter
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
    }

    fun addComment(text: String) {
        val localComment = localDataProvider.newLocalComment(text)
        sendAddComment(localComment)
    }

    fun onAttachmentSelected(attachmentUri: Uri) {
        val localComment = localDataProvider.newLocalComment(fileUri = attachmentUri)
        val fileHooks = UploadFileHooks()
        val cancellationSignal = CancellationSignal()
        fileHooks.subscribeOnCancel {
            cancellationSignal.cancel()
            applyUpdate(CommentEntry(localComment, onClickedCallback = this), ChangeType.Cancelled)
        }
        sendAddComment(localComment, fileHooks, cancellationSignal)
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
            if (comment.hasAttachments() && comment.attachments?.size!! > 1)
                acc.addAll(comment.splitToEntriesByFiles())
            else
                acc.add(CommentEntry(comment, onClickedCallback = this@TicketViewModel))
            acc
        }
    }

    private fun sendAddComment(localComment: Comment,
                               uploadFileHooks: UploadFileHooks? = null,
                               cancellationSignal: CancellationSignal? = null) {

        val toNewTicket = !isFeed && isNewTicket() && !isCreateTicketSent

        applyUpdate(CommentEntry(localComment, uploadFileHooks = uploadFileHooks, onClickedCallback = this), ChangeType.Added)
        val call = when{
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
                    if (cancellationSignal?.isCanceled == true)
                        return@let
                    val entry = when {
                        result.hasError() -> CommentEntry(
                            localComment,
                            onClickedCallback = this@TicketViewModel,
                            error = result.error
                        )
                        else ->{
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
        for (i in recentTicketEntries.lastIndex downTo 0) {
            val entry = recentTicketEntries[i]
            if (entry.type == Type.Comment && !(entry as CommentEntry).comment.isLocal())
                break
            listOfLocalEntries.add(0, entry)
        }
        val toPublish = freshList.toTicketEntries().apply { addAll(listOfLocalEntries) }
        publishEntries(recentTicketEntries, toPublish)
        onDataLoaded()
    }

    private fun applyUpdate(commentEntry: CommentEntry, changeType: ChangeType) {
        val newEntries = recentTicketEntries.toMutableList()
        fun maybeAddDate(){
            commentEntry.comment.creationDate.getWhen(getApplication(), Calendar.getInstance()).let {
                if (newEntries.isEmpty()
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
        return this.attachments.fold(ArrayList()){
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
