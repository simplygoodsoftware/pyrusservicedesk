package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.tickets

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.utils.CIRCLE_TRANSFORMATION
import com.pyrus.pyrusservicedesk._ref.whitetea.android.TeaFragment
import com.pyrus.pyrusservicedesk._ref.whitetea.androidutils.bind
import com.pyrus.pyrusservicedesk._ref.whitetea.androidutils.getStore
import com.pyrus.pyrusservicedesk._ref.whitetea.bind.BinderLifecycleMode
import com.pyrus.pyrusservicedesk.databinding.PsdTicketsListBinding
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.addTicket.AddTicketFragment
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.filterTicketsList.FilterTicketsFragment
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.filterTicketsList.FilterTicketsFragment.Companion.KEY_SELECTED_USER_ID
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.TicketActivity
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.TicketListActivity
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.tickets.TicketsContract.Effect
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.tickets.TicketsContract.Message
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.tickets.TicketsView.TicketListModel
import com.pyrus.pyrusservicedesk.sdk.web.retrofit.SyncRepository
import com.pyrus.pyrusservicedesk.utils.RequestUtils.Companion.getOrganisationLogoUrl
import kotlinx.android.synthetic.main.psd_toolbar.view.psd_toolbar_filter_ib
import kotlinx.android.synthetic.main.psd_toolbar.view.psd_toolbar_qr_ib
import kotlinx.android.synthetic.main.psd_toolbar.view.psd_toolbar_settings_ib
import kotlinx.android.synthetic.main.psd_toolbar.view.psd_toolbar_vendor_iv
import kotlinx.android.synthetic.main.psd_toolbar.view.psd_toolbar_vendor_name_tv
import kotlinx.android.synthetic.main.psd_toolbar.view.ticketsTitleLl
import kotlinx.coroutines.flow.map

internal class TicketsFragment: TeaFragment<TicketListModel, Message, Effect>() {

    private lateinit var binding: PsdTicketsListBinding
    private var selectedUserIdFilter: String = KEY_DEFAULT_USER_ID
    private var currentVendor = ""
    private val hashMap: HashMap<String, String> = hashMapOf() //TODO

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = PsdTicketsListBinding.inflate(inflater, container, false)

        setClickListeners()

        //get information about selected filter and process it
        parentFragmentManager.setFragmentResultListener(
            FilterTicketsFragment.KEY_FILTER_RESULT,
            this
        ) { _, bundle ->
            val selectedUserId = bundle.getString(KEY_SELECTED_USER_ID) ?: KEY_DEFAULT_USER_ID
            dispatch(Message.Inner.UserIdSelected(selectedUserId, parentFragmentManager))
        }


        return binding.root
    }

    private fun setClickListeners() {
        binding.toolbarTicketsList.psd_toolbar_filter_ib.setOnClickListener {
            dispatch(Message.Outer.OnFilterClick(hashMap, ""))
        }

        binding.toolbarTicketsList.ticketsTitleLl.setOnClickListener { dispatch(Message.Outer.OnSettingsClick) }

        binding.toolbarTicketsList.psd_toolbar_settings_ib.setOnClickListener { dispatch(Message.Outer.OnSettingsClick) }

        binding.toolbarTicketsList.psd_toolbar_qr_ib.setOnClickListener {dispatch(Message.Outer.OnScanClick)}

        binding.deleteFilterIv.setOnClickListener { dispatch(Message.Inner.UserIdSelected(KEY_DEFAULT_USER_ID, parentFragmentManager)) }

        binding.fabAddTicket.setOnClickListener { dispatch(Message.Outer.OnFabItemClick(hashMap)) }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //TODO синк и  подписка на синк, будет в actor
        /*lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                syncRepository.ticketsListStateFlow.collect { value ->
                    viewModel.onNewData(value.tickets)
                }
            }
        }

        syncRepository.startSync()*/


        val feature = getStore { PyrusServiceDesk.injector().ticketsFeatureFactory().create() }

        bind(BinderLifecycleMode.CREATE_DESTROY) {
            messages bindTo feature
        }
        bind {
            feature.states().map { transform(it) }  bindTo this@TicketsFragment
            feature.effects() bindTo this@TicketsFragment
        }
    }

    private fun transform(state: TicketsContract.State): TicketListModel {
        return TicketListModel(
            titleText = state.titleText,
            titleImageUrl = state.titleImageUrl,
            filterName = state.filterName,
            ticketsIsEmpty = state.ticketsIsEmpty,
            filterEnabled = state.filterEnabled
        )
    }

    override fun render(model: TicketListModel) {
        //set title (vendor name) and vendor image
        binding.toolbarTicketsList.psd_toolbar_vendor_name_tv.text = model.titleText
        PyrusServiceDesk.injector().picasso
            .load(getOrganisationLogoUrl(model.titleImageUrl, PyrusServiceDesk.get().domain))
            .transform(CIRCLE_TRANSFORMATION)
            .into(binding.toolbarTicketsList.psd_toolbar_vendor_iv)

        //set visibility buttons in toolbar
        binding.toolbarTicketsList.psd_toolbar_filter_ib.visibility =
            if (!model.ticketsIsEmpty) View.VISIBLE else View.GONE

        binding.toolbarTicketsList.psd_toolbar_qr_ib.visibility =
            if (!model.ticketsIsEmpty) View.VISIBLE else View.GONE

        binding.toolbarTicketsList.psd_toolbar_settings_ib.visibility =
            if (model.ticketsIsEmpty) View.VISIBLE else View.GONE


        binding.fabAddTicket.visibility = if (!model.ticketsIsEmpty) View.VISIBLE else View.GONE
        binding.toolbarTicketsList.psd_toolbar_filter_ib.setBackgroundResource(if (!model.filterEnabled) R.drawable.ic_filter else R.drawable.ic_selected_filter)

        //set visibility and text filter ui
        binding.filterFl.visibility = if (model.filterEnabled) View.VISIBLE else View.GONE
        binding.filterContextTv.text = model.filterName
    }

    override fun handleEffect(effect: Effect) {
        when(effect) {

            is Effect.Outer.ShowFilterMenu -> {
                val bottomSheet = FilterTicketsFragment.newInstance(effect.appId, effect.selectedUserId)
                bottomSheet.show(parentFragmentManager, bottomSheet.tag)
            }

            is Effect.Outer.ShowAddTicketMenu -> {
                val bottomSheet = AddTicketFragment.newInstance(effect.appId)
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

        const val KEY_DEFAULT_USER_ID = "0"

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