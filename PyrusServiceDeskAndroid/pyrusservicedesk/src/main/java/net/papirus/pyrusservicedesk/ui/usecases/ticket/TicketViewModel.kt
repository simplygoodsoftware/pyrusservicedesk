package net.papirus.pyrusservicedesk.ui.usecases.ticket

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.content.Intent
import android.net.Uri
import net.papirus.pyrusservicedesk.PyrusServiceDesk
import net.papirus.pyrusservicedesk.repository.data.Comment
import net.papirus.pyrusservicedesk.repository.data.EMPTY_TICKET_ID
import net.papirus.pyrusservicedesk.repository.data.TicketDescription
import net.papirus.pyrusservicedesk.repository.updates.CommentAddedUpdate
import net.papirus.pyrusservicedesk.repository.updates.TicketCreatedUpdate
import net.papirus.pyrusservicedesk.repository.updates.UpdateBase
import net.papirus.pyrusservicedesk.repository.updates.UpdateType
import net.papirus.pyrusservicedesk.ui.viewmodel.ConnectionViewModelBase

internal class TicketViewModel(
    serviceDesk: PyrusServiceDesk,
    arguments: Intent)
    : ConnectionViewModelBase(serviceDesk) {

    private var ticketId: Int = TicketActivity.getTicketId(arguments)

    private val commentsRequest = MutableLiveData<Boolean>()
    private val comments = MediatorLiveData<List<Comment>>()

    init {
        comments.apply{
            if (serviceDesk.enableRichUi) {
                addSource(
                    Transformations.switchMap(commentsRequest){
                        repository.getTicket(ticketId)
                    }
                ){
                    comments.value = it?.ticket?.comments
                    onDataLoaded()
                }
            }
            else {
                addSource(
                    Transformations.switchMap(commentsRequest){
                        repository.getConversation()
                    }
                ){
                    comments.value = it?.comments
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
                commentsRequest.value = true
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

    fun getCommentsViewModel(): LiveData<List<Comment>> = comments

    private fun isNewTicket() = ticketId == EMPTY_TICKET_ID
}
