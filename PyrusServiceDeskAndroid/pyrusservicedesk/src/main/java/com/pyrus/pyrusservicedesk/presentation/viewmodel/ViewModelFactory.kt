package com.pyrus.pyrusservicedesk.presentation.viewmodel

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.content.Intent
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.file_preview.FilePreviewViewModel
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.TicketViewModel
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.dialogs.attach_files.AttachFileSharedViewModel
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.dialogs.comment_actions.PendingCommentActionSharedViewModel
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets.TicketsViewModel

/**
 * Factory that provides view models.
 * @param arguments is optional arguments that are requested by some view models and should be provided by
 * the request side
 */
internal class ViewModelFactory(private val arguments: Intent): ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return when (modelClass) {
            TicketsViewModel::class.java ->
                TicketsViewModel(PyrusServiceDesk.get().serviceDeskProvider) as T
            TicketViewModel::class.java ->
                TicketViewModel(
                    PyrusServiceDesk.get().serviceDeskProvider,
                    arguments,
                    PyrusServiceDesk.get().isSingleChat) as T
            FilePreviewViewModel::class.java ->
                FilePreviewViewModel(
                    PyrusServiceDesk.get().serviceDeskProvider,
                    arguments
                ) as T
            QuitViewModel::class.java -> PyrusServiceDesk.get().getSharedViewModel() as T
            AttachFileSharedViewModel::class.java -> AttachFileSharedViewModel() as T
            PendingCommentActionSharedViewModel::class.java -> PendingCommentActionSharedViewModel() as T
            else -> throw IllegalStateException("View model for class $modelClass was not found")
        }
    }
}