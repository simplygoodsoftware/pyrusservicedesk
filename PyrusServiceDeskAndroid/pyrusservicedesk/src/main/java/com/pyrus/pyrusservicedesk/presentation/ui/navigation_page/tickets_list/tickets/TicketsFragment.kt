package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.tickets

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.whitetea.android.TeaFragment
import com.pyrus.pyrusservicedesk.databinding.PsdTicketsListBinding
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.addTicket.AddTicketFragment
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.filterTicketsList.FilterTicketsFragment
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.TicketActivity
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.TicketListActivity
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.ticketsList.TicketsListContract.Effect
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.ticketsList.TicketsListContract.Message
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.tickets.TicketsView.TicketListModel
import com.pyrus.pyrusservicedesk.sdk.web.retrofit.SyncRepository
import com.pyrus.pyrusservicedesk.utils.CIRCLE_TRANSFORMATION
import com.pyrus.pyrusservicedesk.utils.RequestUtils.Companion.getOrganisationLogoUrl
import kotlinx.android.synthetic.main.psd_empty_tickets_list.view.createTicketTv
import kotlinx.android.synthetic.main.psd_toolbar.view.psd_toolbar_filter_ib
import kotlinx.android.synthetic.main.psd_toolbar.view.psd_toolbar_qr_ib
import kotlinx.android.synthetic.main.psd_toolbar.view.psd_toolbar_settings_ib
import kotlinx.android.synthetic.main.psd_toolbar.view.psd_toolbar_vendor_iv
import kotlinx.android.synthetic.main.psd_toolbar.view.psd_toolbar_vendor_name_tv

internal class TicketsFragment: TeaFragment<TicketListModel, Message, Effect>() {

    private lateinit var binding: PsdTicketsListBinding
    private var selectedUserIdFilter: String = KEY_DEFAULT_USER_ID
    private var currentVendor = ""
    private val hashMap: HashMap<String, String> = hashMapOf() //TODO

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = PsdTicketsListBinding.inflate(inflater, container, false)

        binding.toolbarTicketsList.psd_toolbar_filter_ib.setOnClickListener {
            dispatch(Message.Outer.OnFilterClick(hashMap, ""))
        }

        binding.deleteFilterIv.setOnClickListener { dispatch(Message.Inner.UserIdSelected(
            KEY_DEFAULT_USER_ID
        )) }

        binding.psdEmptyTicketsListLl.createTicketTv.setOnClickListener {
            dispatch(Message.Outer.OnFabItemClick(hashMap))
        }

        binding.fabAddTicket.setOnClickListener { dispatch(Message.Outer.OnFabItemClick(hashMap)) }

        parentFragmentManager.setFragmentResultListener(
            FilterTicketsFragment.KEY_CHOSEN_USER_ID,
            this
        ) { key, bundle ->
            val selectedUserId = bundle.getString(key) ?: KEY_DEFAULT_USER_ID
            dispatch(Message.Inner.UserIdSelected(selectedUserId))
        }


        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val syncRepository = SyncRepository()


        //TODO синк и  подписка на синк, будет в actor
        /*lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                syncRepository.ticketsListStateFlow.collect { value ->
                    viewModel.onNewData(value.tickets)
                }
            }
        }

        syncRepository.startSync()*/


        //TODO add feature
       /* val feature = getStore {}

        bind(BinderLifecycleMode.CREATE_DESTROY) {
            messages bindTo feature
        }
        bind {
            feature.states().debounce(200) bindTo this@TicketsListTeaFragment
            feature.effects() bindTo this@TicketsListTeaFragment
        }*/
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    override fun render(model: TicketListModel) {
        binding.toolbarTicketsList.psd_toolbar_vendor_name_tv.text = model.titleText
        PyrusServiceDesk.get().picasso
            .load(getOrganisationLogoUrl(model.titleImageUrl, PyrusServiceDesk.get().domain))
            .transform(CIRCLE_TRANSFORMATION)
            .into(binding.toolbarTicketsList.psd_toolbar_vendor_iv)

        binding.toolbarTicketsList.psd_toolbar_filter_ib.visibility =
            if (!model.ticketsIsEmpty) View.VISIBLE else View.GONE

        binding.toolbarTicketsList.psd_toolbar_qr_ib.visibility =
            if (!model.ticketsIsEmpty) View.VISIBLE else View.GONE

        binding.toolbarTicketsList.psd_toolbar_settings_ib.visibility =
            if (model.ticketsIsEmpty) View.VISIBLE else View.GONE

        binding.psdEmptyTicketsListLl.visibility =
            if (model.ticketsIsEmpty) View.VISIBLE else View.GONE

        binding.fabAddTicket.visibility = if (!model.ticketsIsEmpty) View.VISIBLE else View.GONE
        binding.toolbarTicketsList.psd_toolbar_filter_ib.setBackgroundResource(if (!model.filterEnabled) R.drawable.ic_filter else R.drawable.ic_selected_filter)
    }

    override fun handleEffect(effect: Effect) {
        when(effect) {

            is Effect.Outer.ShowFilterMenu -> {
                val bottomSheet = FilterTicketsFragment.newInstance(effect.users, effect.selectedUserId)
                bottomSheet.show(parentFragmentManager, bottomSheet.tag)
            }

            is Effect.Outer.ShowAddTicketMenu -> {
                val bottomSheet = AddTicketFragment.newInstance("")
                bottomSheet.show(parentFragmentManager, bottomSheet.tag)
            }

            is Effect.Outer.ShowTicket -> startActivity(
                TicketActivity.getLaunchIntent(
                    ticketId = effect.ticketId,
                    userId = effect.userId
                )
            )

            Effect.Inner.TicketsAutoUpdate -> TODO()
            Effect.Inner.UpdateTickets -> TODO()

        }
    }

    companion object {

        private val TAG = TicketsFragment::class.java.simpleName

        private const val KEY_DEFAULT_USER_ID = "0"

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