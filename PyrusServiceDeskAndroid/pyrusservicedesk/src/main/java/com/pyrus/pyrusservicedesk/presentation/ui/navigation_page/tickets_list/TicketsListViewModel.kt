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

    private companion object {

        private val TAG = TicketsListViewModel::class.java.simpleName

        private const val BUTTON_PATTERN = "<button>(.*?)</button>"

        fun Comment.hasAttachmentWithExceededSize(): Boolean =
            attachments?.let { it.any { attach -> attach.hasExceededFileSize() } } ?: false

        fun Attachment.hasExceededFileSize(): Boolean = bytesSize > MAX_FILE_SIZE_BYTES
    }

    /**
     * Drafted text. Assigned once when view model is created.
     */
    //val draft: String

    private val draftRepository = serviceDeskProvider.getDraftRepository()
    private val localDataProvider: LocalDataProvider = serviceDeskProvider.getLocalDataProvider()
    private val fileManager: FileManager = serviceDeskProvider.getFileManager()
    private val localDataVerifier: LocalDataVerifier = serviceDeskProvider.getLocalDataVerifier()

    private var isCreateTicketSent = false

    private val unreadCounter = MutableLiveData<Int>()
    private val commentDiff = MutableLiveData<DiffResultWithNewItems<TicketEntry>>()

    private var ticketEntries: List<TicketEntry> = emptyList()

    private val mainHandler = Handler(Looper.getMainLooper())


    private var pendingCommentUnderAction: CommentEntry? = null
    private var userId = PyrusServiceDesk.get().userId

    private var currentInterval: Long = 0

    init {

    }

    override fun onLoadData() {
        //update()
    }



    /**
     * Provides live data that delivers list of [TicketShortDescription] to be rendered.
     */
    fun getTicketsLiveData(): LiveData<List<TicketShortDescription>> = tickets

    //TODO delete
//    private fun provideTickets(): List<TicketShortDescription> {
//        val tasks = listOf(
//            TicketShortDescription(
//                0, "Ошибка в счете", false, Comment(
//                    0,
//                    "iiko: Мы рады, что смогли Вам помочь решить проблему ☺",
//                    creationDate = Date(1731074815),
//                    author = Author("Autor")
//                )
//            ),
//            TicketShortDescription(
//                0, "Проблемы с авторизацией в учетной зписи long", false, Comment(
//                    0,
//                    "Вы: После обновления страницы ничего не происходит. Как перевести в режим прос",
//                    creationDate = Date(1730815615000),
//                    author = Author("Autor")
//                )
//            ),
//            TicketShortDescription(
//                0, "Ошибка в счете", false, Comment(
//                    0, "печатает", creationDate = Date(1731074815), author = Author("Autor")
//                )
//            ),
//            TicketShortDescription(
//                0, "Ошибка в счете", false, Comment(
//                    0,
//                    "iiko: Мы рады, что смогли Вам помочь решить проблему ☺",
//                    creationDate = Date(1728137215000),
//                    author = Author("Autor")
//                )
//            ),
//        )
//        return tasks
//    }


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
