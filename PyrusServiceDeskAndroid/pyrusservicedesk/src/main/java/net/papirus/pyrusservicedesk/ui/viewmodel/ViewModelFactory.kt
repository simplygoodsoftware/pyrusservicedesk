package net.papirus.pyrusservicedesk.ui.viewmodel

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.content.Intent
import net.papirus.pyrusservicedesk.PyrusServiceDesk
import net.papirus.pyrusservicedesk.ui.usecases.ticket.TicketViewModel
import net.papirus.pyrusservicedesk.ui.usecases.tickets.TicketsViewModel
import java.lang.IllegalStateException

internal class ViewModelFactory(val arguments: Intent): ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return when (modelClass) {
            TicketsViewModel::class.java ->
                TicketsViewModel(PyrusServiceDesk.getInstance()) as T
            TicketViewModel::class.java ->
                TicketViewModel(PyrusServiceDesk.getInstance(), arguments) as T
            SharedViewModel::class.java -> PyrusServiceDesk.getInstance().getSharedViewModel() as T
            else -> throw IllegalStateException("View model for class $modelClass was not found")
        }
    }
}