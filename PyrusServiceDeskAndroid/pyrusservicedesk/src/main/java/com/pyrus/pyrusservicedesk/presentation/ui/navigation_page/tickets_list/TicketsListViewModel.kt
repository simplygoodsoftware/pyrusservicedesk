package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.ServiceDeskProvider
import com.pyrus.pyrusservicedesk.presentation.call.GetTicketsCall
import com.pyrus.pyrusservicedesk.presentation.viewmodel.ConnectionViewModelBase
import com.pyrus.pyrusservicedesk.sdk.data.Application
import com.pyrus.pyrusservicedesk.sdk.data.Attachment
import com.pyrus.pyrusservicedesk.sdk.data.Comment
import com.pyrus.pyrusservicedesk.sdk.data.Ticket
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.Tickets
import com.pyrus.pyrusservicedesk.sdk.updates.LiveUpdateSubscriber
import com.pyrus.pyrusservicedesk.sdk.updates.PreferencesManager
import com.pyrus.pyrusservicedesk.utils.RequestUtils.Companion.MAX_FILE_SIZE_BYTES

/**
 * ViewModel for the tickets list screen.
 */
internal class TicketsListViewModel(
    serviceDeskProvider: ServiceDeskProvider,
    private val preferencesManager: PreferencesManager
) : ConnectionViewModelBase(serviceDeskProvider), LiveUpdateSubscriber {


    private val tickets = MediatorLiveData<List<Ticket>>()
    private val applications = MediatorLiveData<List<Application>>()

    private var usersId: List<String>? = null
    private var usersName: List<String>? = null

    private var unreadCount = 0

    private companion object {

        private val TAG = TicketsListViewModel::class.java.simpleName

        private const val BUTTON_PATTERN = "<button>(.*?)</button>"

        fun Comment.hasAttachmentWithExceededSize(): Boolean =
            attachments?.let { it.any { attach -> attach.hasExceededFileSize() } } ?: false

        fun Attachment.hasExceededFileSize(): Boolean = bytesSize > MAX_FILE_SIZE_BYTES
    }

    init {
        if (isNetworkConnected.value == true) {
            loadData()
        }

        liveUpdates.subscribeOnData(this)
    }

    override fun onLoadData() {
        update()
    }

    override fun onNewData(tickets: Tickets) {
        this.tickets.value = tickets.tickets?.sortedWith(TicketComparator())
        this.applications.value = tickets.applications
        this.usersId = PyrusServiceDesk.usersId
        this.usersName = PyrusServiceDesk.usersName
        unreadCount = 1//tickets.tickets?.count{ description -> !description.isRead!! }//TODO we need unreadCount?
    }

    override fun onUnreadTicketCountChanged(unreadTicketCount: Int) {
        //TODO
    }

    fun getUsersId() : List<String>? = usersId

    fun getUsersName() : List<String>? = usersName

    /**
     * Provides live data that delivers list of [Ticket] to be rendered.
     */
    fun getTicketsLiveData(): LiveData<List<Ticket>> = tickets

    /**
     * Provides live data that delivers list of [Application] to be rendered.
     */
    fun getApplicationsLiveData(): LiveData<List<Application>> = applications

    private fun update() {
        GetTicketsCall(this@TicketsListViewModel, requests)
            .execute()
            .observeForever { result ->
                if (result == null)
                    return@observeForever
                when {
                    result.hasError() -> {  }
                    else ->{
                        onNewData(result.data!!)
                        onDataLoaded()
                    }
                }
            }
    }

    private class TicketComparator : Comparator<Ticket> {

        override fun compare(o1: Ticket, o2: Ticket): Int {
            return when {
                o1.lastComment == null -> return when {
                    o2.lastComment == null -> o1.ticketId - o2.ticketId
                    else -> 1
                }
                o2.lastComment == null -> -1
                o1.lastComment.creationDate.before(o2.lastComment.creationDate) -> 1
                o1.lastComment.creationDate.after(o2.lastComment.creationDate) -> -1
                else -> o1.ticketId - o2.ticketId
            }
        }
    }

//    private fun Ticket.read(): Ticket {
//        return Ticket(ticketId, userId, subject, author, true, lastComment, null, null, null)
//    }


    /**
     * Provides live data that delivers count of currently unread tickets
     */
    //TODO delete?
    fun getUnreadCount() = unreadCount


    /**
     * Callback to be invoked when user opens [ticket] in UI
     */
    //TODO delete?
    fun onTicketOpened(ticket: Ticket) {
        if (!ticket.isRead!!) {
            unreadCount--
            val ticketsMutable = tickets.value!!.toMutableList()
            //ticketsMutable[ticketsMutable.indexOf(ticket)].isRead = ticket.isRead
            tickets.value = ticketsMutable
        }
    }

}
