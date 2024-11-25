package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.ServiceDeskProvider
import com.pyrus.pyrusservicedesk.log.PLog
import com.pyrus.pyrusservicedesk.presentation.call.*
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.CommentsDiffCallback
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.HtmlTagUtils
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries.*
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.DiffResultWithNewItems
import com.pyrus.pyrusservicedesk.presentation.viewmodel.ConnectionViewModelBase
import com.pyrus.pyrusservicedesk.sdk.data.Attachment
import com.pyrus.pyrusservicedesk.sdk.data.Author
import com.pyrus.pyrusservicedesk.sdk.data.Comment
import com.pyrus.pyrusservicedesk.sdk.data.FileManager
import com.pyrus.pyrusservicedesk.sdk.data.LocalDataProvider
import com.pyrus.pyrusservicedesk.sdk.data.TicketShortDescription
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.AddCommentResponseData
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.Comments
import com.pyrus.pyrusservicedesk.sdk.response.PendingDataError
import com.pyrus.pyrusservicedesk.sdk.updates.OnUnreadTicketCountChangedSubscriber
import com.pyrus.pyrusservicedesk.sdk.updates.PreferencesManager
import com.pyrus.pyrusservicedesk.sdk.verify.LocalDataVerifier
import com.pyrus.pyrusservicedesk.sdk.web.OnCancelListener
import com.pyrus.pyrusservicedesk.sdk.web.UploadFileHooks
import com.pyrus.pyrusservicedesk.utils.ConfigUtils
import com.pyrus.pyrusservicedesk.utils.MILLISECONDS_IN_MINUTE
import com.pyrus.pyrusservicedesk.utils.MILLISECONDS_IN_SECOND
import com.pyrus.pyrusservicedesk.utils.RequestUtils.Companion.MAX_FILE_SIZE_BYTES
import com.pyrus.pyrusservicedesk.utils.RequestUtils.Companion.MAX_FILE_SIZE_MEGABYTES
import com.pyrus.pyrusservicedesk.utils.getWhen
import kotlinx.coroutines.*
import java.lang.Exception
import java.lang.Runnable
import java.util.*
import kotlin.collections.ArrayList

/**
 * ViewModel for the tickets list screen.
 */
internal class TicketsListViewModel(
    serviceDeskProvider: ServiceDeskProvider,
    private val preferencesManager: PreferencesManager
) : ConnectionViewModelBase(serviceDeskProvider) {


    private val tickets = MediatorLiveData<List<TicketShortDescription>>()

    private var unreadCount = 0


    override fun onLoadData() {
        //update()
    }



    /**
     * Provides live data that delivers list of [TicketShortDescription] to be rendered.
     */
    fun getTicketsLiveData(): LiveData<List<TicketShortDescription>> = tickets


    /**
     * Provides live data that delivers count of currently unread tickets
     */
    fun getUnreadCount() = unreadCount


    /**
     * Callback to be invoked when user opens [ticket] in UI
     */
    fun onTicketOpened(ticket: TicketShortDescription) {
        if (!ticket.isRead) {
            unreadCount--
            val ticketsMutable = tickets.value!!.toMutableList()
            //ticketsMutable[ticketsMutable.indexOf(ticket)] = ticket.isRead
            tickets.value = ticketsMutable
        }
    }

}
