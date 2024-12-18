package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.ticketsList

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.injector
import com.pyrus.pyrusservicedesk._ref.Screens
import com.pyrus.pyrusservicedesk._ref.whitetea.android.TeaFragment
import com.pyrus.pyrusservicedesk._ref.whitetea.androidutils.bind
import com.pyrus.pyrusservicedesk._ref.whitetea.androidutils.getStore
import com.pyrus.pyrusservicedesk._ref.whitetea.bind.BinderLifecycleMode
import com.pyrus.pyrusservicedesk.databinding.TicketsListFragmentBinding
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.addTicket.AddTicketFragment
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketMapper
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.TicketActivity
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.adapters.TicketsListAdapter
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.ticketsList.TicketsListContract.Effect
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.ticketsList.TicketsListContract.Message
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.ticketsList.TicketsListContract.State
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map

internal class TicketsListFragment: TeaFragment<State, Message, Effect>()  {

    private lateinit var binding: TicketsListFragmentBinding
    private lateinit var adapter: TicketsListAdapter
    private val hashMap: HashMap<String, String> = hashMapOf() //TODO sd start data
    private var appId: String = DEFAULT_APP_ID

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = TicketsListFragmentBinding.inflate(layoutInflater)

        binding.emptyTicketsListLl.createTicketTv.setOnClickListener {
            dispatch(Message.Outer.OnCreateTicketClick)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        adapter = TicketsListAdapter()
            .apply {
                setOnTicketItemClickListener {
                    dispatch(Message.Outer.OnTicketClick(it.ticketId))
                }
            }
        binding.ticketsRv.adapter = adapter
        binding.ticketsRv.layoutManager = LinearLayoutManager(context)
        arguments?.takeIf { it.containsKey(KEY_APP_ID) }?.apply {
            appId = getString(KEY_APP_ID) ?: DEFAULT_APP_ID
        }

        //get selected userId and process it
        parentFragmentManager.setFragmentResultListener(KEY_USER_ID, this) { _, bundle ->
            val result = bundle.getString(KEY_USER_ID) ?: DEFAULT_USER_ID
            dispatch(Message.Outer.OnUserIdSelect(result))
        }

        //TODO add feature
        val feature = getStore { injector().ticketsListFeatureFactory(appId).create() }

         bind(BinderLifecycleMode.CREATE_DESTROY) {
             this@TicketsListFragment.messages.map { it } bindTo feature
             //feature.messages bindTo this@TicketsListFragment
             //this@TicketsListFragment.messages() bindTo feature
             //messages bindTo feature
         }
         bind {
             feature.state.debounce(200) bindTo this@TicketsListFragment
             feature.effects bindTo this@TicketsListFragment
         }
    }

    override fun render(model: State) {
        binding.emptyTicketsListLl.psdEmptyTicketsList.visibility =
            if (model.tickets.isEmpty()) View.VISIBLE else View.GONE

        adapter.setItems(model.tickets)

    }

    override fun handleEffect(effect: Effect) {
        when(effect) {

            is Effect.Outer.ShowAddTicketMenu -> {
                val bottomSheet = AddTicketFragment.newInstance(effect.appId)
                bottomSheet.show(parentFragmentManager, bottomSheet.tag)
            }

            is Effect.Outer.ShowTicket -> injector().router.navigateTo(Screens.TicketScreen(effect.ticketId, effect.userId ?: DEFAULT_USER_ID))
                /*startActivity(
                TicketActivity.getLaunchIntent(
                    ticketId = effect.ticketId,
                    userId = effect.userId
                )*/

            Effect.Inner.TicketsAutoUpdate -> TODO()
            Effect.Inner.UpdateTickets -> TODO()
        }
    }


    //TODO
    /*private fun getSelectedUserIds(chosenUserId: String): List<Ticket> {
        val allUsersName = viewModel.getTicketsLiveData().value ?: emptyList()
        if (chosenUserId == KEY_DEFAULT_USER_ID)
            return allUsersName

        return allUsersName.filter { it.userId == chosenUserId }
    }*/




    companion object {

        private const val DEFAULT_APP_ID = ""

        private const val KEY_APP_ID = "KEY_APP_ID"

        const val KEY_USER_ID = "KEY_USER_ID"

        const val DEFAULT_USER_ID = "0"

    }
}