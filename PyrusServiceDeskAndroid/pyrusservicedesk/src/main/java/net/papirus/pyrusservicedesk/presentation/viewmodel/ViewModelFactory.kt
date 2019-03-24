package net.papirus.pyrusservicedesk.presentation.viewmodel

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.content.Intent
import net.papirus.pyrusservicedesk.PyrusServiceDesk
import net.papirus.pyrusservicedesk.presentation.ui.navigation_page.file_preview.FilePreviewViewModel
import net.papirus.pyrusservicedesk.presentation.ui.navigation_page.ticket.TicketSharedViewModel
import net.papirus.pyrusservicedesk.presentation.ui.navigation_page.ticket.TicketViewModel
import net.papirus.pyrusservicedesk.presentation.ui.navigation_page.tickets.TicketsViewModel

internal class ViewModelFactory(val arguments: Intent): ViewModelProvider.Factory {

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