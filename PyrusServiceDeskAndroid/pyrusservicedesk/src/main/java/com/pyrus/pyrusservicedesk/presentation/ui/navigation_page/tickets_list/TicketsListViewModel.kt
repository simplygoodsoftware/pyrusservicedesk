package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.pyrus.pyrusservicedesk.ServiceDeskProvider
import com.pyrus.pyrusservicedesk.presentation.call.GetTicketsCall
import com.pyrus.pyrusservicedesk.presentation.viewmodel.ConnectionViewModelBase
import com.pyrus.pyrusservicedesk.sdk.data.Attachment
import com.pyrus.pyrusservicedesk.sdk.data.Comment
import com.pyrus.pyrusservicedesk.sdk.data.TicketShortDescription
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


    private val tickets = MediatorLiveData<List<TicketShortDescription>>()

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

    override fun onNewData(tickets: List<TicketShortDescription>) {
        this.tickets.value = tickets.sortedWith(TicketShortDescriptionComparator())
        unreadCount = tickets.count{ description -> !description.isRead }
    }

    override fun onUnreadTicketCountChanged(unreadTicketCount: Int) {
        //TODO
    }

    /**
     * Provides live data that delivers list of [TicketShortDescription] to be rendered.
     */
    fun getTicketsLiveData(): LiveData<List<TicketShortDescription>> = tickets

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

    private class TicketShortDescriptionComparator : Comparator<TicketShortDescription> {

        override fun compare(o1: TicketShortDescription, o2: TicketShortDescription): Int {
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

    private fun TicketShortDescription.read(): TicketShortDescription {
        return TicketShortDescription(ticketId, subject, true, lastComment)
    }


    /**
     * Provides live data that delivers count of currently unread tickets
     */
    //TODO delete?
    fun getUnreadCount() = unreadCount


    /**
     * Callback to be invoked when user opens [ticket] in UI
     */
    //TODO delete?
    fun onTicketOpened(ticket: TicketShortDescription) {
        if (!ticket.isRead) {
            unreadCount--
            val ticketsMutable = tickets.value!!.toMutableList()
            //ticketsMutable[ticketsMutable.indexOf(ticket)].isRead = ticket.isRead
            tickets.value = ticketsMutable
        }
    }

}
