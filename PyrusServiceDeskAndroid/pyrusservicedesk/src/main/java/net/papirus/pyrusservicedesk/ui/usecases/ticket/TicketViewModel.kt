package net.papirus.pyrusservicedesk.ui.usecases.ticket

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.content.Intent
import android.net.Uri
import net.papirus.pyrusservicedesk.PyrusServiceDesk
import net.papirus.pyrusservicedesk.sdk.data.Comment
import net.papirus.pyrusservicedesk.sdk.data.EMPTY_TICKET_ID
import net.papirus.pyrusservicedesk.sdk.data.TicketDescription
import net.papirus.pyrusservicedesk.sdk.updates.CommentAddedUpdate
import net.papirus.pyrusservicedesk.sdk.updates.TicketCreatedUpdate
import net.papirus.pyrusservicedesk.sdk.updates.UpdateBase
import net.papirus.pyrusservicedesk.sdk.updates.UpdateType
import net.papirus.pyrusservicedesk.ui.usecases.ticket.entries.CommentEntry
import net.papirus.pyrusservicedesk.ui.usecases.ticket.entries.DateEntry
import net.papirus.pyrusservicedesk.ui.usecases.ticket.entries.TicketEntry
import net.papirus.pyrusservicedesk.ui.viewmodel.ConnectionViewModelBase
import net.papirus.pyrusservicedesk.utils.getWhen
import java.util.*

internal class TicketViewModel(
    serviceDesk: PyrusServiceDesk,
    arguments: Intent)
    : ConnectionViewModelBase(serviceDesk) {

    private var ticketId: Int = TicketActivity.getTicketId(arguments)

    private val commentsRequest = MutableLiveData<Boolean>()
    private val entries = MediatorLiveData<MutableList<TicketEntry>>()
    private val commentChanges = MutableLiveData<CommentChangedUiUpdate>()

    init {
        entries.apply{
            if (serviceDesk.enableRichUi) {
                addSource(
                    Transformations.switchMap(commentsRequest){
                        repository.getTicket(ticketId)
                    }
                ){
                    entries.value = it?.ticket?.comments?.toTicketEntries()
                    onDataLoaded()
                }
            }
            else {
                addSource(
                    Transformations.switchMap(commentsRequest){
                        repository.getConversation()
                    }
                ){
                    entries.value = it?.comments?.toTicketEntries()
                    onDataLoaded()
                }
            }
        }

        if (!isNewTicket() && isNetworkConnected.value == true) {
            loadData()
            replayProgress()
        }
        onInitialized()
    }

    override fun <T : UpdateBase> onUpdateReceived(update: T) {
        when (update) {
            is TicketCreatedUpdate -> {
                ticketId = update.ticketId ?: ticketId
                commentsRequest.value = true
            }
            is CommentAddedUpdate -> {
                if (update.ticketId != ticketId)
                    return
                applyUpdate(
                    CommentEntry(update.comment, update.error),
                    if (update.isNew) ChangeType.Added else ChangeType.Changed)
            }
        }
    }

    override fun getUpdateTypes(): Set<UpdateType> {
        return setOf(UpdateType.TicketUpdated, UpdateType.TicketCreated)
    }

    override fun loadData() {
        commentsRequest.value = true
    }

    fun addComment(text: String) {
        if (isNewTicket())
            repository.createTicket("USER", TicketDescription(text, text))
        else
            repository.addComment(ticketId, text)
    }

    fun addAttachment(attachmentUri: Uri) {
        repository.uploadFile(ticketId, attachmentUri)
    }

    fun getTicketEntriesLiveData(): LiveData<List<TicketEntry>> = entries as LiveData<List<TicketEntry>>

    fun getCommentChangesLiveData(): LiveData<CommentChangedUiUpdate> = commentChanges

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
            acc.add(CommentEntry(comment))
            acc
        }
    }

    private fun applyUpdate(commentEntry: CommentEntry, changeType: ChangeType) {
        if (entries.value == null) {
            entries.value = mutableListOf()
        }
        val list = entries.value!!
        commentEntry.comment.creationDate.getWhen(getApplication(), Calendar.getInstance()).let {
            if (list.isEmpty()
                || list.findLast { entry -> entry is DateEntry }
                    ?.let { dateEntry ->  (dateEntry as DateEntry).date == it } == false) {

                list.add(DateEntry(commentEntry.comment.creationDate.getWhen(getApplication(), Calendar.getInstance())))
            }
        }
        list.add(commentEntry)
        commentChanges.value = CommentChangedUiUpdate(
            changeType,
            commentEntry
        )
    }
}
