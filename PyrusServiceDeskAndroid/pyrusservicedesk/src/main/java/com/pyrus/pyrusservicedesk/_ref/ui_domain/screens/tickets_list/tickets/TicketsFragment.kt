package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.injector
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.add_ticket.AddTicketFragment
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.filter_tickets_list.FilterTicketsFragment
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.filter_tickets_list.FilterTicketsFragment.Companion.KEY_SELECTED_USER_ID
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.adapters.TicketsPageAdapter
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.Effect
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.Message
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsView.Model
import com.pyrus.pyrusservicedesk._ref.utils.CIRCLE_TRANSFORMATION
import com.pyrus.pyrusservicedesk._ref.utils.RequestUtils.Companion.getOrganisationLogoUrl
import com.pyrus.pyrusservicedesk._ref.utils.insets.RootViewDeferringInsetsCallback
import com.pyrus.pyrusservicedesk._ref.whitetea.android.TeaFragment
import com.pyrus.pyrusservicedesk._ref.whitetea.androidutils.bind
import com.pyrus.pyrusservicedesk._ref.whitetea.androidutils.getStore
import com.pyrus.pyrusservicedesk._ref.whitetea.bind.BinderLifecycleMode
import com.pyrus.pyrusservicedesk._ref.whitetea.core.ViewRenderer
import com.pyrus.pyrusservicedesk._ref.whitetea.utils.diff
import com.pyrus.pyrusservicedesk.databinding.PsdTicketsListBinding
import kotlinx.coroutines.flow.map

internal class TicketsFragment: TeaFragment<Model, Message, Effect.Outer>() {

    private lateinit var binding: PsdTicketsListBinding
    private var currentUserId = KEY_DEFAULT_USER_ID
    private val adapter: TicketsPageAdapter by lazy {
        TicketsPageAdapter(::dispatch)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val feature = getStore { injector().ticketsFeatureFactory.create() }

        bind(BinderLifecycleMode.CREATE_DESTROY) {
            this@TicketsFragment.messages.map { it } bindTo feature
        }
        bind {
            feature.state.map(TicketsMapper::map) bindTo this@TicketsFragment
            feature.effects bindTo this@TicketsFragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = PsdTicketsListBinding.inflate(inflater, container, false)

        setClickListeners()

        //get information about selected filter and process it
        parentFragmentManager.setFragmentResultListener(
            FilterTicketsFragment.KEY_FILTER_RESULT,
            this
        ) { _, bundle ->
            currentUserId = bundle.getString(KEY_SELECTED_USER_ID) ?: KEY_DEFAULT_USER_ID
            dispatch(Message.Inner.UserIdSelected(currentUserId, childFragmentManager))
        }

        //todo
        /*//get information about selected vendor and process it
        parentFragmentManager.setFragmentResultListener(
            FilterTicketsFragment.KEY_FILTER_RESULT,
            this
        ) { _, bundle ->
            val selectedUserId = bundle.getString(KEY_SELECTED_USER_ID) ?: KEY_DEFAULT_USER_ID
            dispatch(Message.Inner.UserIdSelected(selectedUserId, parentFragmentManager))
        }*/

        return binding.root
    }

    private fun setClickListeners() {
        binding.toolbarTicketsList.psdToolbarFilterIb.setOnClickListener {
            dispatch(Message.Outer.OnFilterClick(currentUserId))
        }

        binding.toolbarTicketsList.ticketsTitleLl.setOnClickListener { dispatch(Message.Outer.OnSettingsClick) }

        binding.toolbarTicketsList.psdToolbarSettingsIb.setOnClickListener { dispatch(Message.Outer.OnSettingsClick) }

        binding.toolbarTicketsList.psdToolbarQrIb.setOnClickListener {dispatch(Message.Outer.OnScanClick)}

        binding.deleteFilterIv.setOnClickListener { dispatch(Message.Inner.UserIdSelected(
            KEY_DEFAULT_USER_ID, childFragmentManager)) }

        binding.fabAddTicket.setOnClickListener { dispatch(Message.Outer.OnFabItemClick) }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val deferringInsetsListener = RootViewDeferringInsetsCallback(
            persistentInsetTypes = WindowInsetsCompat.Type.systemBars(),
            deferredInsetTypes = WindowInsetsCompat.Type.ime()
        )
        ViewCompat.setOnApplyWindowInsetsListener(binding.root, deferringInsetsListener)

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                dispatch(Message.Outer.OnChangePage(adapter.getAppId(position)))
            }
        })

        binding.viewPager.adapter = adapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = adapter.getTitle(position)
        }.attach()

    }

    /*override fun render(model: TicketListModel) {
        //set title (vendor name) and vendor image
        binding.toolbarTicketsList.psdToolbarVendorNameTv.text =  model.titleText
        injector().picasso
            .load(getOrganisationLogoUrl(model.titleImageUrl, null)) // TODO domain from account
            .transform(CIRCLE_TRANSFORMATION)
            .into(binding.toolbarTicketsList.psdToolbarVendorIv)

        //set visibility buttons in toolbar
        binding.toolbarTicketsList.psdToolbarFilterIb.visibility =
            if (!model.ticketsIsEmpty) View.VISIBLE else View.GONE

        binding.toolbarTicketsList.psdToolbarQrIb.visibility =
            if (!model.ticketsIsEmpty) View.VISIBLE else View.GONE

        binding.toolbarTicketsList.psdToolbarSettingsIb.visibility =
            if (model.ticketsIsEmpty) View.VISIBLE else View.GONE


        binding.fabAddTicket.visibility = if (!model.ticketsIsEmpty) View.VISIBLE else View.GONE
        binding.toolbarTicketsList.psdToolbarFilterIb.setBackgroundResource(if (!model.filterEnabled) R.drawable.ic_filter else R.drawable.ic_selected_filter)

        //set visibility and text filter ui
        binding.filterFl.visibility = if (model.filterEnabled) View.VISIBLE else View.GONE
        binding.filterContextTv.text = model.filterName

        //tabLayout and viewPager
        binding.tabLayout.visibility = if (model.tabLayoutVisibility ) View.VISIBLE else View.GONE
        viewPagerAdapter.setItems(model.applications)
    }*/

    override fun createRenderer(): ViewRenderer<Model> = diff {
        diff(Model::titleText) { title -> binding.toolbarTicketsList.psdToolbarVendorNameTv.text = title }
        diff(Model::titleImageUrl) { url ->
            val logoUrl = url?.let { getOrganisationLogoUrl(url, null) } // TODO
            injector().picasso
                .load(logoUrl)
                .transform(CIRCLE_TRANSFORMATION)
                .into(binding.toolbarTicketsList.psdToolbarVendorIv)
        }
        diff(Model::ticketsIsEmpty) { isEmpty ->
            binding.toolbarTicketsList.psdToolbarFilterIb.isVisible = !isEmpty
            binding.toolbarTicketsList.psdToolbarQrIb.isVisible = !isEmpty
            binding.toolbarTicketsList.psdToolbarSettingsIb.isVisible = isEmpty
            binding.fabAddTicket.isVisible = !isEmpty

        }
        diff(Model::filterEnabled) { filterEnabled ->
            binding.toolbarTicketsList.psdToolbarFilterIb.setBackgroundResource(
                if (!filterEnabled) R.drawable.ic_filter
                else R.drawable.ic_selected_filter
            )
            binding.filterFl.isVisible = filterEnabled
        }
        diff(Model::filterName) { filterName ->
            binding.filterContextTv.text = filterName
        }
        diff(Model::tabLayoutVisibility) { tabLayoutVisibility ->
            binding.tabLayout.isVisible = tabLayoutVisibility
        }
        diff(Model::applications) { ticketSetInfoList ->
            adapter.submitList(ticketSetInfoList)
        }
        diff(Model::showNoConnectionError) { showError ->
            binding.noConnection.root.isVisible = showError
        }
        diff(Model::isLoading) { isLoading ->
            Log.d("SDS", "isLoading: ${isLoading}")
            binding.tabLayout.isVisible = !isLoading
            binding.progressBar.isVisible = isLoading
        }
    }

    override fun handleEffect(effect: Effect.Outer) {
        when(effect) {

            is Effect.Outer.ShowFilterMenu -> {
                val bottomSheet = FilterTicketsFragment.newInstance(effect.appId, effect.selectedUserId)
                bottomSheet.show(parentFragmentManager, bottomSheet.tag)
            }

            is Effect.Outer.ShowAddTicketMenu -> {
                val bottomSheet = AddTicketFragment.newInstance(effect.appId)
                bottomSheet.show(parentFragmentManager, bottomSheet.tag)
            }

        }
    }

    internal companion object {

        const val KEY_DEFAULT_USER_ID = "0"
        const val KEY_APP_ID_RESULT = "KEY_APP_ID_RESULT"
        const val KEY_USER_ID = "KEY_USER_ID"

        fun newInstance(): TicketsFragment {
            return TicketsFragment()
        }

    }

}