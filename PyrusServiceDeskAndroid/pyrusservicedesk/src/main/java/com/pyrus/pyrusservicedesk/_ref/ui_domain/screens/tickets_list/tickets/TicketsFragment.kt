package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.injector
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.add_ticket.AddTicketBottomSheetFragment
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.filter_tickets_list.FilterTicketsBottomSheetFragment
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.filter_tickets_list.FilterTicketsBottomSheetFragment.Companion.KEY_SELECTED_USER_ID
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.adapters.TicketsPageAdapter
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.Effect
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.Message
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsView.Model
import com.pyrus.pyrusservicedesk._ref.utils.CIRCLE_TRANSFORMATION
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk._ref.utils.insets.RootViewDeferringInsetsCallback
import com.pyrus.pyrusservicedesk._ref.utils.text
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
    private var currentUserId = KEY_DEFAULT_USER_ID // TODO remove it
    private val adapter: TicketsPageAdapter by lazy { TicketsPageAdapter(::dispatch) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val feature = getStore { injector().ticketsFeatureFactory.create() }

        bind(BinderLifecycleMode.CREATE_DESTROY) {
            messages.map { it } bindTo feature
        }
        bind {
            feature.state.map { TicketsMapper.map(it.contentState) } bindTo this@TicketsFragment
            feature.effects bindTo this@TicketsFragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = PsdTicketsListBinding.inflate(inflater, container, false)

        setClickListeners()

        //get information about selected filter and process it
        parentFragmentManager.setFragmentResultListener(
            FilterTicketsBottomSheetFragment.KEY_FILTER_RESULT,
            this
        ) { _, bundle ->
            currentUserId = bundle.getString(KEY_SELECTED_USER_ID) ?: KEY_DEFAULT_USER_ID
            childFragmentManager.setFragmentResult(KEY_USER_ID, bundleOf(KEY_USER_ID to currentUserId))
            dispatch(Message.Outer.OnFilterSelected(currentUserId))
        }

        return binding.root
    }

    private fun setClickListeners() {
        binding.toolbarTicketsList.psdToolbarFilterIb.setOnClickListener {
            dispatch(Message.Outer.OnFilterClick(currentUserId))
        }

        binding.noConnection.reconnectButton.setOnClickListener {
            dispatch(Message.Outer.OnRetryClick)
        }

        binding.deleteFilterIv.setOnClickListener {
            childFragmentManager.setFragmentResult(KEY_USER_ID, bundleOf(KEY_USER_ID to KEY_DEFAULT_USER_ID))
            currentUserId = KEY_DEFAULT_USER_ID
            dispatch(Message.Outer.OnFilterSelected(KEY_DEFAULT_USER_ID))
        }

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
                if (adapter.currentList.isNotEmpty() && position < adapter.currentList.size)
                    dispatch(Message.Outer.OnChangePage(adapter.getAppId(position)))
            }
        })

        binding.viewPager.adapter = adapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = adapter.getTitle(position)
        }.attach()

        val multichatButtons = ConfigUtils.getMultichatButtons()
        if (multichatButtons?.rightButtonRes != null) {
            binding.toolbarTicketsList.psdToolbarRightIb
                .setBackgroundResource(multichatButtons.rightButtonRes)
        }
        if (multichatButtons?.rightButtonAction != null) {
            binding.toolbarTicketsList.psdToolbarRightIb.setOnClickListener {
                try {
                    startActivity(multichatButtons.rightButtonAction)
                }
                catch (e: Exception) {
                    // TODO show error ui
                }
            }
        }
        if (multichatButtons?.centerAction != null) {
            binding.toolbarTicketsList.ticketsTitleLl.setOnClickListener {
                try {
                    startActivity(multichatButtons.centerAction)
                }
                catch (e: Exception) {
                    // TODO show error ui
                }
            }
        }

    }


    private fun onCreateDialog(title: String, message: String, usersIsEmpty: Boolean): Dialog {
        val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton(resources.getString(R.string.ok)
            ) { dialog, id ->
                if (!usersIsEmpty)
                    dialog.cancel()
                else {
                    val multichatButtons = ConfigUtils.getMultichatButtons()
                    if (multichatButtons?.backButton != null) {
                        try {
                            startActivity(multichatButtons.backButton)
                        } catch (e: Exception) {
                            // TODO show error ui
                        }
                    }
                    dialog.cancel()
                }
            }
        return builder.create()
    }

    override fun createRenderer(): ViewRenderer<Model> = diff {
        diff(Model::titleText) { title -> binding.toolbarTicketsList.psdToolbarVendorNameTv.text = title }
        diff(Model::titleImageUrl) { url ->
            injector().picasso
                .load(url)
                .transform(CIRCLE_TRANSFORMATION)
                .into(binding.toolbarTicketsList.psdToolbarVendorIv)
        }
        diff(Model::ticketsIsEmpty) { isEmpty ->
            binding.toolbarTicketsList.psdToolbarRightIb.isVisible = !isEmpty && binding.toolbarTicketsList.psdToolbarRightIb.background != null
            binding.fabAddTicket.isVisible = !isEmpty

        }
        diff(Model::showFilter) { showFilter ->
            binding.toolbarTicketsList.psdToolbarFilterIb.isVisible = showFilter
        }
        diff(Model::filterEnabled) { filterEnabled ->
            binding.toolbarTicketsList.psdToolbarFilterIb.setBackgroundResource(
                if (!filterEnabled) R.drawable.ic_filter
                else R.drawable.ic_selected_filter
            )
            binding.filterFl.isVisible = filterEnabled
            if (!filterEnabled)
                currentUserId = KEY_DEFAULT_USER_ID
        }
        diff(Model::filterName) { filterName ->
            binding.filterContextTv.text = filterName
        }
        diff(Model::tabLayoutIsVisible) { tabLayoutVisibility ->
            binding.tabLayout.isVisible = tabLayoutVisibility
        }
        diff(Model::ticketSetInfo, { n, o -> n === o }) { ticketSetInfo ->
            adapter.submitList(ticketSetInfo?.ticketSets) {
                ticketSetInfo?.page?.let { page ->
                    if (binding.viewPager.currentItem != page) {
                        binding.viewPager.setCurrentItem(page, true)
                    }
                }
            }
        }
        diff(Model::showNoConnectionError) { showError ->
            binding.noConnection.root.isVisible = showError
        }
        diff(Model::isLoading) { isLoading ->
            binding.progressBar.isVisible = isLoading
        }
    }

    override fun handleEffect(effect: Effect.Outer) {
        when(effect) {

            is Effect.Outer.ShowFilterMenu -> {
                val bottomSheet = FilterTicketsBottomSheetFragment.newInstance(
                    effect.appId,
                    effect.selectedUserId,
                    effect.users
                )
                bottomSheet.show(parentFragmentManager, bottomSheet.tag)
            }

            is Effect.Outer.ShowAddTicketMenu -> {
                val bottomSheet = AddTicketBottomSheetFragment.newInstance(
                    effect.appId,
                    effect.users
                )
                bottomSheet.show(parentFragmentManager, bottomSheet.tag)
            }

            is Effect.Outer.ShowDialog -> {
                val dialog = onCreateDialog(resources.getString(R.string.psd_no_access), effect.message.text(requireActivity()), effect.usersIsEmpty)
                dialog.show()
            }

        }
    }

    internal companion object {

        const val KEY_DEFAULT_USER_ID = "0"
        const val KEY_USER_ID = "KEY_USER_ID"

        fun newInstance(): TicketsFragment = TicketsFragment()

    }

}