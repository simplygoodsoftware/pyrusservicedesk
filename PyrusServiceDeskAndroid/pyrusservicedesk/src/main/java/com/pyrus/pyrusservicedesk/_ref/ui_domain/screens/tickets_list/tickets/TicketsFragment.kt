package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets

import android.os.Bundle
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
import com.pyrus.pyrusservicedesk._ref.Screens
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.add_ticket.AddTicketFragment
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.filter_tickets_list.FilterTicketsFragment
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.filter_tickets_list.FilterTicketsFragment.Companion.KEY_SELECTED_USER_ID
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.adapters.ViewPagerAdapter
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.Effect
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.Message
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsView.TicketListModel
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

internal class TicketsFragment: TeaFragment<TicketListModel, Message, Effect.Outer>() {

    private lateinit var binding: PsdTicketsListBinding
    private var selectedUserIdFilter: String = KEY_DEFAULT_USER_ID
    private var currentVendor = ""
    private var currentUserId = KEY_DEFAULT_USER_ID
    private lateinit var viewPagerAdapter: ViewPagerAdapter
    //private val hashMap: HashMap<String, String> = hashMapOf() //TODO

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

        viewPagerAdapter = ViewPagerAdapter(childFragmentManager, lifecycle)

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                dispatch(Message.Outer.OnChangeApp(viewPagerAdapter.getAppId(position)))
            }
        })

        binding.viewPager.adapter = viewPagerAdapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = viewPagerAdapter.getTitle(position)
        }.attach()


        val feature = getStore { injector().ticketsFeatureFactory().create() }

        bind(BinderLifecycleMode.CREATE_DESTROY) {
            this@TicketsFragment.messages.map { it } bindTo feature
        }
        bind {
            feature.state.map(TicketsMapper::map) bindTo this@TicketsFragment
            feature.effects bindTo this@TicketsFragment
        }
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

        binding.tabLayout.isVisible = !model.isLoading
        binding.progressBar.isVisible = model.isLoading

        binding.noConnection.root.isVisible = model.showNoConnectionError

    }*/

    override fun createRenderer(): ViewRenderer<TicketListModel> = diff {
        diff(TicketListModel::titleText) { title -> binding.toolbarTicketsList.psdToolbarVendorNameTv.text = title }
        diff(TicketListModel::titleImageUrl) { url ->
            injector().picasso
                .load(getOrganisationLogoUrl(url, null))//TODO
                .transform(CIRCLE_TRANSFORMATION)
                .into(binding.toolbarTicketsList.psdToolbarVendorIv)
        }
        diff(TicketListModel::ticketsIsEmpty) { isEmpty ->

            binding.toolbarTicketsList.psdToolbarFilterIb.isVisible = !isEmpty
            binding.toolbarTicketsList.psdToolbarQrIb.isVisible = !isEmpty
            binding.toolbarTicketsList.psdToolbarSettingsIb.isVisible = isEmpty
            binding.fabAddTicket.isVisible = !isEmpty

        }
        diff(TicketListModel::filterEnabled) { filterEnabled ->
            binding.toolbarTicketsList.psdToolbarFilterIb.setBackgroundResource(if (!filterEnabled) R.drawable.ic_filter else R.drawable.ic_selected_filter)
            binding.filterFl.isVisible = filterEnabled
        }
        diff(TicketListModel::filterName) { filterName ->
            binding.filterContextTv.text = filterName
        }
        diff(TicketListModel::tabLayoutVisibility) { tabLayoutVisibility ->
            binding.tabLayout.isVisible = tabLayoutVisibility
        }
        diff(TicketListModel::applications) { applications ->
            viewPagerAdapter.setItems(applications)
        }
        diff(TicketListModel::showNoConnectionError) { showError -> binding.noConnection.root.isVisible = showError }
        diff(TicketListModel::isLoading) { isLoading ->
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

            is Effect.Outer.ShowTicket -> {
                injector().router.navigateTo(Screens.TicketScreen(effect.ticketId, effect.userId))
            }
        }
    }

    companion object {

        private val TAG = TicketsFragment::class.java.simpleName

        const val KEY_DEFAULT_USER_ID = "0"
        const val KEY_APP_ID_RESULT = "KEY_APP_ID_RESULT"

        fun newInstance(): TicketsFragment {
            return TicketsFragment()
        }

    }

}