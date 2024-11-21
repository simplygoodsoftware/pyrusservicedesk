package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.addTicket

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.TicketActivity
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.TicketsListViewModel
import com.pyrus.pyrusservicedesk.utils.getViewModelWithActivityScope

class AddTicketFragment: BottomSheetDialogFragment() {

    private val viewModel: TicketsListViewModel by getViewModelWithActivityScope(TicketsListViewModel::class.java)

    override fun getTheme() = R.style.PsdAppBottomSheetDialogTheme

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.add_ticket_fragment, null, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerview: RecyclerView = view.findViewById(R.id.usersRv)
        recyclerview.layoutManager = LinearLayoutManager(view.context)

        val adapter = AddTicketAdapter(users = getUsersName(),
            onItemClick = { position ->
                startActivity(TicketActivity.getLaunchIntent(userId = PyrusServiceDesk.usersId[position]))
                dismiss()
            })
        recyclerview.adapter = adapter
    }

    private fun getUsersName(): List<String> {
        return viewModel.getUsersName() ?: emptyList()
    }
}