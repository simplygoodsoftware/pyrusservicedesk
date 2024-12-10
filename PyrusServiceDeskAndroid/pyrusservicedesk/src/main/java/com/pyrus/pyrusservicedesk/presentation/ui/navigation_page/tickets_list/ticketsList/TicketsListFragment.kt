package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.ticketsList

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.databinding.TicketsListFragmentBinding
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.TicketActivity
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.TicketListActivity
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.TicketsListViewModel
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.adapters.TicketsListAdapter
import com.pyrus.pyrusservicedesk.sdk.data.Ticket
import com.pyrus.pyrusservicedesk.utils.getViewModelWithActivityScope

class TicketsListFragment: Fragment() {

    private val viewModel: TicketsListViewModel by getViewModelWithActivityScope(
        TicketsListViewModel::class.java)

    private lateinit var binding: TicketsListFragmentBinding
    private lateinit var adapter: TicketsListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = TicketsListFragmentBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        adapter = TicketsListAdapter()
            .apply {
                setOnTicketItemClickListener {
                    it.ticketId.let { ticketId ->
                        this@TicketsListFragment.startActivity(
                            TicketActivity.getLaunchIntent(
                                ticketId = ticketId,
                                userId = viewModel.getCurrentUserId(ticketId)
                            )
                        )
                        viewModel.onTicketOpened(it)
                    }
                }
            }
        binding.ticketsRv.adapter = adapter
        binding.ticketsRv.layoutManager = LinearLayoutManager(context)
        arguments?.takeIf { it.containsKey(KEY_APP_ID) }?.apply {
            val appId = getString(KEY_APP_ID).toString() //TODO
            adapter.setItems(getSelectedUserIds(KEY_DEFAULT_USER_ID))
            viewModel.getTicketsLiveData()
        }
    }

    private fun getSelectedUserIds(chosenUserId: String): List<Ticket> {
        val allUsersName = viewModel.getTicketsLiveData().value ?: emptyList()
        if (chosenUserId == KEY_DEFAULT_USER_ID)
            return allUsersName

        return allUsersName.filter { it.userId == chosenUserId }
    }




    companion object {

        private const val KEY_DEFAULT_USER_ID = "0"

        private const val KEY_APP_ID = "KEY_APP_ID"

        /**
         * Provides intent for launching the screen.
         */
        fun getLaunchIntent(): Intent {
            return Intent(
                PyrusServiceDesk.get().application,
                TicketListActivity::class.java
            )
        }

    }
}