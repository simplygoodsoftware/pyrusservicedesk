package net.papirus.pyrusservicedesk.ui.usecases.ticket

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.content.Intent
import android.net.Uri
import net.papirus.pyrusservicedesk.PyrusServiceDesk
import net.papirus.pyrusservicedesk.repository.data.*
import net.papirus.pyrusservicedesk.repository.updates.*
import net.papirus.pyrusservicedesk.ui.viewmodel.ConnectionViewModelBase

internal class TicketViewModel(
    serviceDesk: PyrusServiceDesk,
    val arguments: Intent)
    : ConnectionViewModelBase(serviceDesk) {

    private var ticketId: Int = TicketActivity.getTicketId(arguments)

    private val commentsRequest = MutableLiveData<Boolean>()
    private val comments = MediatorLiveData<List<Comment>>()

    init {
        comments.apply{
            addSource(
                    Transformations.switchMap(commentsRequest){
                        repository.getTicket(ticketId)
                    }
            ){
                comments.value = it?.ticket?.comments
            }
        }

        if (!isNewTicket() && isNetworkConnected.value == true) {
            loadData()
        }
        onInitialized()
    }

    override fun <T : UpdateBase> onUpdateReceived(update: T) {
        when (update) {
            is TicketCreatedUpdate ->
                ticketId = update.ticketId ?: ticketId
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
