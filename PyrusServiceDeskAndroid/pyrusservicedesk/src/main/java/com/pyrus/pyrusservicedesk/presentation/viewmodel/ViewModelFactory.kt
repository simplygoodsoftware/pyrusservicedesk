package com.pyrus.pyrusservicedesk.presentation.viewmodel

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.content.Intent
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.file_preview.FilePreviewViewModel
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.TicketSharedViewModel
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.TicketViewModel
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
                TicketsViewModel(PyrusServiceDesk.getInstance()) as T
            TicketViewModel::class.java ->
                TicketViewModel(PyrusServiceDesk.getInstance(), arguments) as T
            FilePreviewViewModel::class.java ->
                FilePreviewViewModel(PyrusServiceDesk.getInstance(), arguments) as T
            QuitViewModel::class.java -> PyrusServiceDesk.getInstance().getSharedViewModel() as T
            TicketSharedViewModel::class.java -> TicketSharedViewModel() as T
            else -> throw IllegalStateException("View model for class $modelClass was not found")
        }
    }
}