package net.papirus.pyrusservicedesk.ui.usecases.ticket

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.content.Intent
import android.net.Uri
import android.support.v7.util.DiffUtil
import net.papirus.pyrusservicedesk.PyrusServiceDesk
import net.papirus.pyrusservicedesk.sdk.data.Comment
import net.papirus.pyrusservicedesk.sdk.data.EMPTY_TICKET_ID
import net.papirus.pyrusservicedesk.sdk.data.TicketDescription
import net.papirus.pyrusservicedesk.sdk.updates.*
import net.papirus.pyrusservicedesk.ui.usecases.ticket.entries.*
import net.papirus.pyrusservicedesk.ui.view.recyclerview.DiffResultWithNewItems
import net.papirus.pyrusservicedesk.ui.viewmodel.ConnectionViewModelBase
import net.papirus.pyrusservicedesk.utils.getWhen
import java.util.*
import kotlin.collections.ArrayList

internal class TicketViewModel(
        serviceDesk: PyrusServiceDesk,
        arguments: Intent)
    : ConnectionViewModelBase(serviceDesk),
        OnClickedCallback<CommentEntry> {

    val isFeed = serviceDesk.enableFeedUi

    private var ticketId: Int = TicketActivity.getTicketId(arguments)

    private val unreadCounter = MutableLiveData<Int>()
    private val commentsRequest = MutableLiveData<Boolean>()
    private val entries = MediatorLiveData<List<Comment>>()
    private val commentDiff = MutableLiveData<DiffResultWithNewItems<TicketEntry>>()

    private var recentTicketEntries: List<TicketEntry> = emptyList()

    init {
        entries.apply{
            if (!isFeed) {
                addSource(
                    Transformations.switchMap(commentsRequest){
                        repository.getTicket(ticketId)
                    }
                ){
                    entries.value = it?.ticket?.comments
                    publishEntries(recentTicketEntries, entries.value?.toTicketEntries() ?: emptyList())
                    onDataLoaded()
                }
            }
            else {
                addSource(
                    Transformations.switchMap(commentsRequest){
                        repository.getTicketFeed()
                    }
                ){
                    entries.value = it?.comments
                    publishEntries(recentTicketEntries, entries.value?.toTicketEntries() ?: emptyList())
                    onDataLoaded()
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
        onInitialized()
    }

    override fun <T : UpdateBase> onUpdateReceived(update: T) {
        when (update.type) {
            UpdateType.TicketCreated -> {
                (update as TicketCreatedUpdate).let {
                    ticketId = update.ticketId ?: ticketId;
                    onLoadData()
                }
            }
            UpdateType.CommentAdded -> {
                (update as CommentAddedUpdate).let {
                    if (!isFeed && update.ticketId != ticketId)
                        return
                    applyUpdate(
                        CommentEntry(update.comment, update.fileUploadCallbacks, this, update.error),
                        if (update.isNew) ChangeType.Added else ChangeType.Changed)
                }
            }
            UpdateType.CommentCancelled -> {
                (update as CommentCancelledUpdate).let {
                    if (!isFeed && update.ticketId != ticketId)
                        return
                    applyUpdate(
                        CommentEntry(update.comment, onClickedCallback = this),
                        ChangeType.Cancelled)
                }
            }
        }
    }

    override fun getUpdateTypes(): Set<UpdateType> {
        return setOf(UpdateType.CommentAdded, UpdateType.CommentCancelled, UpdateType.TicketCreated)
    }

    override fun onLoadData() {
        commentsRequest.value = true
    }

    override fun onClicked(item: CommentEntry) {
        if (!item.hasError())
            return
        else {
            repository.retryComment(ticketId, item.comment)
        }
    }

    fun addComment(text: String) {
        if (isNewTicket())
            repository.createTicket("USER", TicketDescription(text, text))
        else
            repository.addComment(ticketId, text)
    }

    fun addAttachment(attachmentUri: Uri) = repository.uploadFile(ticketId, attachmentUri)

    fun getCommentDiffLiveData(): LiveData<DiffResultWithNewItems<TicketEntry>> = commentDiff

    fun getUnreadCounterLiveData(): LiveData<Int> = unreadCounter

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
