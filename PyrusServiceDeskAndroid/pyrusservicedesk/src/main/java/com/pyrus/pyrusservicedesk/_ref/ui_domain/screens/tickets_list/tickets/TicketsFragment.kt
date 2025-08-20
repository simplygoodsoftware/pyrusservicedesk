package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.terrakok.cicerone.Navigator
import com.pyrus.pyrusservicedesk.NoFullScreenFragment
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.injector
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.SdScreens
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.add_ticket.AddTicketBottomSheetFragment
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.adapters.app_tabs.AppTabFingerprint
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.adapters.ticket_list.ClosedTicketsTitleFingerprint
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.adapters.ticket_list.TicketHeadersListFingerprint
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.adapters.ticket_list.TicketsListViewHolder
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.Effect
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.Message
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsView.Model
import com.pyrus.pyrusservicedesk._ref.utils.CIRCLE_TRANSFORMATION
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils.Companion.getChatTitleTextColor
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils.Companion.getMainBackgroundColor
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils.Companion.getNoPreviewBackgroundColor
import com.pyrus.pyrusservicedesk._ref.utils.TopSmoothScroller
import com.pyrus.pyrusservicedesk._ref.utils.getSecondaryColorOnBackground
import com.pyrus.pyrusservicedesk._ref.utils.insets.RootViewDeferringInsetsCallback
import com.pyrus.pyrusservicedesk._ref.utils.navigation.PyrusNavigator
import com.pyrus.pyrusservicedesk._ref.utils.navigation.setSlideRightAnimation
import com.pyrus.pyrusservicedesk._ref.utils.text
import com.pyrus.pyrusservicedesk._ref.whitetea.android.TeaFragment
import com.pyrus.pyrusservicedesk._ref.whitetea.androidutils.bind
import com.pyrus.pyrusservicedesk._ref.whitetea.androidutils.getStore
import com.pyrus.pyrusservicedesk._ref.whitetea.bind.BinderLifecycleMode
import com.pyrus.pyrusservicedesk._ref.whitetea.core.ViewRenderer
import com.pyrus.pyrusservicedesk._ref.whitetea.utils.diff
import com.pyrus.pyrusservicedesk.databinding.PsdTicketsListBinding
import com.pyrus.pyrusservicedesk.payload_adapter.PayloadListAdapter
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.item_decorators.DividerVerticalItemDecoration
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.item_decorators.GroupVerticalItemDecoration
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.map

@OptIn(FlowPreview::class)
internal class TicketsFragment: TeaFragment<Model, Message, Effect.Outer>(), NoFullScreenFragment {

    private lateinit var binding: PsdTicketsListBinding
    private var needToScrollToClosedHeader = false
    private var needToScrollUp = false
    private var filterLlVisibility = false
    private val navigator: Navigator by lazy { PyrusNavigator(requireActivity(), R.id.fragment_container) }

    private val headersAdapter: PayloadListAdapter<Model.TicketsEntry> by lazy {
        PayloadListAdapter(
            TicketHeadersListFingerprint(::dispatch),
            ClosedTicketsTitleFingerprint(::dispatch)
        )
    }
    private val tabsAdapter: PayloadListAdapter<Model.TabEntry> by lazy {
        PayloadListAdapter(AppTabFingerprint(::dispatch))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val feature = getStore { injector().ticketsFeatureFactory.create() }

        bind(BinderLifecycleMode.CREATE_DESTROY) {
            messages.map { it } bindTo feature
        }
        bind {
            feature.state.debounceSkipFirst(70).map { TicketsMapper.map(it.contentState) } bindTo this@TicketsFragment
            feature.effects bindTo this@TicketsFragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = PsdTicketsListBinding.inflate(inflater, container, false)

        setClickListeners()

        applyStyle()

        return binding.root
    }

    private fun setClickListeners() {

        binding.noConnection.reconnectButton.setOnClickListener {
            dispatch(Message.Outer.OnRetryClick)
        }

        binding.fabAddTicket.setOnClickListener { dispatch(Message.Outer.OnFabItemClick) }
        binding.createTicketTv.setOnClickListener { dispatch(Message.Outer.OnFabItemClick) }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val deferringInsetsListener = RootViewDeferringInsetsCallback(
            persistentInsetTypes = WindowInsetsCompat.Type.systemBars(),
            deferredInsetTypes = WindowInsetsCompat.Type.ime()
        )
        ViewCompat.setOnApplyWindowInsetsListener(binding.root, deferringInsetsListener)

        binding.ticketsRv.adapter = headersAdapter
        binding.ticketsRv.itemAnimator = null
        binding.ticketsRv.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.ticketsRv.addItemDecoration(
            DividerVerticalItemDecoration(
                dividerHeight = resources.getDimension(R.dimen.dp_05).toInt(),
                dividerLeftMargin = resources.getDimension(R.dimen.psd_offset_default).toInt(),
                dividerColor = resources.getColor(R.color.psd_color_divider_250),
            ) { current, next ->
                when {
                    current is TicketsListViewHolder && next is TicketsListViewHolder -> true
                    current is TicketsListViewHolder && next == null -> true
                    else -> false
                }
            }
        )
        binding.tabRv.adapter = tabsAdapter
        binding.tabRv.addItemDecoration(GroupVerticalItemDecoration(
            viewType = GroupVerticalItemDecoration.TYPE_ANY,
            innerDivider = resources.getDimension(R.dimen.psd_tab_item_space).toInt(),
            outerDivider = 0,
            invert = false,
            orientation = RecyclerView.HORIZONTAL
        ))

        binding.refresh.setOnRefreshListener { dispatch(Message.Outer.OnRefresh) }

        binding.tabRv.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.tabRv.itemAnimator = null

        binding.progressBar.indeterminateDrawable.setColorFilter(
            ConfigUtils.getAccentColor(requireContext()),
            android.graphics.PorterDuff.Mode.SRC_IN
        )
        val multichatButtons = ConfigUtils.getMultichatButtons()
        binding.toolbarTicketsList.psdToolbarRightIb.isVisible =  multichatButtons?.rightButtonRes != null
        if (multichatButtons?.rightButtonRes != null) {
            binding.toolbarTicketsList.psdToolbarRightIb
                .setImageResource(multichatButtons.rightButtonRes)
        }
        if (multichatButtons?.rightButtonAction != null) {
            binding.toolbarTicketsList.psdToolbarRightIb.setOnClickListener {
                dispatch(Message.Outer.OnRightButtonClick)
            }
        }
        binding.toolbarTicketsList.psdSearchIb.setOnClickListener {
            dispatch(Message.Outer.OnSearchClick)
        }
    }

    override fun onResume() {
        super.onResume()
        injector().audioWrapper.updateMainActivityIsActive(true)
        injector().navHolder.setNavigator(navigator)
    }

    override fun onPause() {
        injector().navHolder.removeNavigator()
        injector().audioWrapper.updateMainActivityIsActive(false)
        injector().stopSession()
        super.onPause()
    }

    private fun applyStyle() {
        val accentColor = ConfigUtils.getAccentColor(requireContext())

        binding.fabAddTicket.backgroundTintList = ColorStateList.valueOf(accentColor)

        binding.root.setBackgroundColor(getMainBackgroundColor(requireContext()))

        val toolbarColor = ConfigUtils.getHeaderBackgroundColor(requireContext())
        binding.toolbarTicketsList.psdToolbarVendorNameTv.setTextColor(getChatTitleTextColor(requireContext()))
        binding.toolbarTicketsList.ticketToolbar.setBackgroundColor(toolbarColor)

        ConfigUtils.getMainFontTypeface()?.let {
            binding.noConnection.noConnectionTextView.typeface = it
            binding.noConnection.reconnectButton.typeface = it
        }

        val secondaryColor = getSecondaryColorOnBackground(getNoPreviewBackgroundColor(requireContext()))
        binding.noConnection.noConnectionImageView.setColorFilter(secondaryColor)
        binding.noConnection.noConnectionTextView.setTextColor(secondaryColor)

        binding.noConnection.reconnectButton.setTextColor(accentColor)

        binding.noConnection.root.setBackgroundColor(ConfigUtils.getNoConnectionBackgroundColor(requireContext()))

        ConfigUtils.getMainBoldFontTypeface()?.let {
            binding.toolbarTicketsList.psdToolbarVendorNameTv.typeface = it
        }

        binding.refresh.setProgressBackgroundColor(R.color.psd_color_fab_scroll_down)
        binding.refresh.setColorSchemeColors(accentColor)
        binding.toolbarTicketsList.psdSearchIb.imageTintList = ColorStateList.valueOf(accentColor)

        with(requireActivity().window) {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = ConfigUtils.getStatusBarColor(requireContext()) ?: statusBarColor
        }

    }

    fun scrollToTop() {
        binding.ticketsRv.smoothScrollToPosition(0)
    }

    override fun createRenderer(): ViewRenderer<Model> = diff {
        diff(Model::titleText) { title ->
            binding.toolbarTicketsList.psdToolbarVendorNameTv.text = title?.text(requireContext())
        }
        diff(Model::titleImageUrl) { url ->
            var drawable = ConfigUtils.getSupportAvatar(requireContext())
            if (!url.isNullOrBlank())
                drawable = resources.getDrawable(R.drawable.transparent_bg)
            injector().picasso
                .load(url)
                .placeholder(drawable)
                .error(ConfigUtils.getSupportAvatar(requireContext()))
                .transform(CIRCLE_TRANSFORMATION)
                .into(binding.toolbarTicketsList.psdToolbarVendorIv)
        }
        diff(Model::tabLayoutIsVisible) { tabLayoutVisibility ->
            binding.tabRv.isVisible = tabLayoutVisibility
        }
        diff(Model::tickets, { n, o -> n === o }) { tickets ->
            headersAdapter.submitList(tickets) {
                if (needToScrollToClosedHeader) {
                    needToScrollToClosedHeader = false
                    needToScrollUp = false
                    val smoothScroller = TopSmoothScroller(requireContext())
                    val closedEntry = headersAdapter.currentList.find {
                        it is Model.TicketsEntry.ClosedTicketTitleEntry
                    }
                    smoothScroller.targetPosition = headersAdapter.currentList.indexOf(closedEntry)
                    binding.ticketsRv.layoutManager?.startSmoothScroll(smoothScroller)
                }
                else if (needToScrollUp) {
                    needToScrollUp = false
                    binding.ticketsRv.smoothScrollToPosition(0)
                }
                else if (needToScrollTop()) {
                    binding.ticketsRv.scrollToPosition(0)
                }
            }
        }
        diff(Model::appTabs, { n, o -> n === o }) { appTabs ->
            tabsAdapter.submitList(appTabs) {
                binding.tabRv.post {
                    val index = appTabs.indexOfFirst { it.isSelected }
                    if (index >= 0) {
                        (binding.tabRv.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(index, 0)
                    }
                }
            }
        }
        diff(Model::showNoConnectionError) { showError ->
            binding.noConnection.root.isVisible = showError
        }
        diff(Model::isLoading) { isLoading ->
            binding.progressBar.isVisible = isLoading
            binding.toolbarTicketsList.psdToolbarVendorIv.visibility =
                if (!isLoading) View.VISIBLE else View.INVISIBLE
        }
        diff(Model::isUserTriggerLoading, compare = { _, _ -> false }) { isLoading ->
            if (binding.refresh.isRefreshing != isLoading) {
                binding.refresh.isRefreshing = isLoading
            }
        }
        diff(Model::showCreateTicketPicture) { show ->
            binding.psdEmptyListIb.isVisible = show
            binding.createTicketTv.isVisible = show
        }
    }

    private fun changeVisibilityViewWithAnimation(view: View, show: Boolean) {

        val params = view.layoutParams as? ConstraintLayout.LayoutParams ?: return

        view.post {
            view.measure(
                View.MeasureSpec.makeMeasureSpec(view.width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val viewSize = view.height.let { if (it == 0) view.measuredHeight else it }

            val marginProperty = params::topMargin

            val (start, end) = if (show) Pair(-viewSize, 0) else Pair(0, -viewSize)

            if (show) {
                marginProperty.set(start)
                view.visibility = View.VISIBLE
                view.requestLayout()
            }

            ValueAnimator.ofInt(start, end).apply {
                duration = 300
                interpolator = FastOutSlowInInterpolator()

                addUpdateListener {
                    marginProperty.set(it.animatedValue as Int)
                    view.requestLayout()
                }

                if (!show) {
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            view.visibility = View.GONE
                        }
                    })
                }
            }.start()
        }
    }

    override fun handleEffect(effect: Effect.Outer) {
        when(effect) {

            is Effect.Outer.ShowFilterMenu -> {}

            is Effect.Outer.ShowAddTicketMenu -> {
                val bottomSheet = AddTicketBottomSheetFragment.newInstance(
                    effect.appId,
                    effect.users
                )
                bottomSheet.show(parentFragmentManager, bottomSheet.tag)
            }

            is Effect.Outer.ScrollToClosedHeader -> {
                needToScrollToClosedHeader = true
            }

            is Effect.Outer.ScrollUp -> {
                needToScrollUp = true
            }

            is Effect.Outer.OpenTicket -> {
                injector().router.navigateTo(SdScreens.TicketScreen(effect.ticketId, effect.user).setSlideRightAnimation())
            }
        }
    }

    private fun needToScrollTop(): Boolean {
        val layoutManager = binding.ticketsRv.layoutManager as LinearLayoutManager
        val isAtTop =
            layoutManager.findFirstVisibleItemPosition() == 0 && layoutManager.findViewByPosition(0)?.top == binding.ticketsRv.paddingTop
        return isAtTop
    }

    internal companion object {

        const val KEY_DEFAULT_USER_ID = "0"
        const val KEY_USER_ID = "KEY_USER_ID"

        fun newInstance(): TicketsFragment = TicketsFragment()

    }

}