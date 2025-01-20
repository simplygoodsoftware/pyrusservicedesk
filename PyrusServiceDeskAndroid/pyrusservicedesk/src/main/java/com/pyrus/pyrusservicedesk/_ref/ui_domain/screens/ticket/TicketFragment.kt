package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket

import android.content.ClipData
import android.content.ClipboardManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.injector
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketView.Model
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.TicketAdapter
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.TicketAdapter.Companion.VIEW_TYPE_COMMENT_INBOUND
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.TicketAdapter.Companion.VIEW_TYPE_COMMENT_OUTBOUND
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.new_entries.CommentEntryV2
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk._ref.utils.getColorOnBackground
import com.pyrus.pyrusservicedesk._ref.utils.getSecondaryColorOnBackground
import com.pyrus.pyrusservicedesk._ref.utils.insets.RootViewDeferringInsetsCallback
import com.pyrus.pyrusservicedesk._ref.utils.setCursorColor
import com.pyrus.pyrusservicedesk._ref.utils.showKeyboardOn
import com.pyrus.pyrusservicedesk._ref.utils.text
import com.pyrus.pyrusservicedesk._ref.whitetea.android.TeaFragment
import com.pyrus.pyrusservicedesk._ref.whitetea.androidutils.bind
import com.pyrus.pyrusservicedesk._ref.whitetea.androidutils.getStore
import com.pyrus.pyrusservicedesk._ref.whitetea.bind.BinderLifecycleMode
import com.pyrus.pyrusservicedesk._ref.whitetea.core.ViewRenderer
import com.pyrus.pyrusservicedesk._ref.whitetea.utils.diff
import com.pyrus.pyrusservicedesk.databinding.PsdFragmentTicketBinding
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.item_decorators.GroupVerticalItemDecoration
import com.pyrus.pyrusservicedesk.sdk.repositories.UserInternal
import kotlinx.coroutines.flow.map


internal class TicketFragment: TeaFragment<Model, TicketView.Event, TicketView.Effect>() {

    private lateinit var binding: PsdFragmentTicketBinding

    private val adapter: TicketAdapter by lazy {
        TicketAdapter(
            { dispatch(TicketView.Event.OnRetryClick(it)) },
            ::dispatch,
            { text -> dispatch(TicketView.Event.OnCopyClick(text)) },
            { rating -> dispatch(TicketView.Event.OnRatingClick(rating)) }
        )
    }

    private val inputTextWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            dispatch(TicketView.Event.OnMessageChanged(s.toString()))
        }
    }

    override fun createRenderer(): ViewRenderer<Model> = diff {
        diff(Model::inputText) { text -> if (!binding.input.hasFocus()) binding.input.setText(text) }
        diff(Model::sendEnabled) { sendEnabled -> binding.send.isEnabled = sendEnabled }
        diff(Model::comments, { new, old -> new === old }, ::updateComments)
        diff(Model::showNoConnectionError) { showError -> binding.noConnection.root.isVisible = showError }
        diff(Model::isLoading) { isLoading ->
            binding.ticketContent.isVisible = !isLoading
            binding.progressBar.isVisible = isLoading
        }
        diff(Model::toolbarTitleText) { text -> binding.toolbarTitle.text = ConfigUtils.getTitle(requireContext(), text?.text(requireContext())) }
        diff(Model::isRefreshing) { isRefreshing -> binding.refresh.isRefreshing = isRefreshing }
    }

    override fun handleEffect(effect: TicketView.Effect) {
        when(effect) {
            is TicketView.Effect.CopyToClipboard -> {
                val clipboard = getSystemService(requireContext(), ClipboardManager::class.java) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Copied text", effect.text))
            }

            is TicketView.Effect.MakeToast -> {
                Toast.makeText(
                    requireContext(),
                    effect.text.text(requireContext()),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindFeature()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = PsdFragmentTicketBinding.inflate(inflater, container, false)

        initUi()
        initListeners()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState?.getBoolean(STATE_KEYBOARD_SHOWN) == true) {
            showKeyboardOn(binding.input)
        }

        val deferringInsetsListener = RootViewDeferringInsetsCallback(
            persistentInsetTypes = WindowInsetsCompat.Type.systemBars(),
            deferredInsetTypes = WindowInsetsCompat.Type.ime()
        )
        ViewCompat.setOnApplyWindowInsetsListener(binding.root, deferringInsetsListener)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_KEYBOARD_SHOWN, binding.input.hasFocus())
    }

    private fun bindFeature() {
        val user = arguments?.getParcelable<UserInternal>(KEY_USER_INTERNAL)!!
        // TODO если открыть файл и вернуться в задачу скорее всего id обновится
        // TODO использовать nullable для нового тикета
        val ticketId = arguments?.getLong(KEY_TICKET_ID)!!

        val feature = getStore { injector().ticketFeatureFactory.create(
            user = user,
            initialTicketId = ticketId,
            welcomeMessage = ConfigUtils.getWelcomeMessage()
        ) }
        bind(BinderLifecycleMode.CREATE_DESTROY) {
            this@TicketFragment.messages.map(TicketMapper::map) bindTo feature
        }
        bind {
            feature.state.map(TicketMapper::map) bindTo this@TicketFragment
            feature.effects.map(TicketMapper::map) bindTo this@TicketFragment
        }

    }

    private fun initListeners() {
        binding.ticketToolbar.setOnMenuItemClickListener { onMenuItemClicked(it) }
        binding.send.setOnClickListener {
            dispatch(TicketView.Event.OnSendClick)
            binding.input.text = null
            binding.comments.scrollToPosition(0)
        }
        binding.attach.setOnClickListener { dispatch(TicketView.Event.OnShowAttachVariantsClick) }
        binding.input.addTextChangedListener(inputTextWatcher)
        binding.refresh.setOnRefreshListener { dispatch(TicketView.Event.OnRefresh) }
        binding.toolbarBack.setOnClickListener { dispatch(TicketView.Event.OnCloseClick) }
    }

    private fun initUi() {
        initCommentsRecyclerView()

        binding.toolbarTitle.text = ConfigUtils.getTitle(requireContext())

        applyStyle()
    }

    private fun initCommentsRecyclerView() {
        val recyclerView = binding.comments

        recyclerView.adapter = adapter

        recyclerView.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            true
        )

        val defaultDivider = resources.getDimensionPixelSize(R.dimen.psd_comments_item_space)
        recyclerView.addItemDecoration(
            GroupVerticalItemDecoration(
                viewType = VIEW_TYPE_COMMENT_INBOUND,
                innerDivider = defaultDivider,
                outerDivider = defaultDivider * 2,
                invert = true,
            )
        )
        recyclerView.addItemDecoration(
            GroupVerticalItemDecoration(
                viewType = VIEW_TYPE_COMMENT_OUTBOUND,
                innerDivider = defaultDivider,
                outerDivider = defaultDivider * 2,
                invert = true,
            )
        )
        recyclerView.addItemDecoration(
            GroupVerticalItemDecoration(
                viewType = GroupVerticalItemDecoration.TYPE_ANY,
                innerDivider = defaultDivider,
                outerDivider = defaultDivider,
                invert = true,
                excludeTypes = setOf(VIEW_TYPE_COMMENT_INBOUND, VIEW_TYPE_COMMENT_OUTBOUND)
            )
        )

        val pool = RecyclerView.RecycledViewPool()
        pool.setMaxRecycledViews(VIEW_TYPE_COMMENT_OUTBOUND, 10)
        pool.setMaxRecycledViews(VIEW_TYPE_COMMENT_INBOUND, 10)
        recyclerView.setRecycledViewPool(pool)

        recyclerView.itemAnimator = null
    }

    private fun applyStyle() {
        val accentColor = ConfigUtils.getAccentColor(requireContext())

        binding.root.setBackgroundColor(ConfigUtils.getMainBackgroundColor(requireContext()))

        val toolbarColor = ConfigUtils.getHeaderBackgroundColor(requireContext())
        binding.toolbarTitle.setTextColor(ConfigUtils.getChatTitleTextColor(requireContext()))
        binding.ticketToolbar.setBackgroundColor(toolbarColor)

        ConfigUtils.getMainFontTypeface()?.let {
            binding.send.typeface = it
            binding.input.typeface = it
            binding.noConnection.noConnectionTextView.typeface = it
            binding.noConnection.reconnectButton.typeface = it
        }

        val secondaryColor =
            getSecondaryColorOnBackground(ConfigUtils.getNoPreviewBackgroundColor(requireContext()))
        binding.noConnection.noConnectionImageView.setColorFilter(secondaryColor)
        binding.noConnection.noConnectionTextView.setTextColor(secondaryColor)

        binding.noConnection.reconnectButton.setTextColor(ConfigUtils.getAccentColor(requireContext()))

        binding.noConnection.root.setBackgroundColor(ConfigUtils.getNoConnectionBackgroundColor(requireContext()))

        ConfigUtils.getMainBoldFontTypeface()?.let {
            binding.toolbarTitle.typeface = it
        }

        val stateList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_enabled),
                intArrayOf(-android.R.attr.state_enabled)
            ),
            intArrayOf(
                ConfigUtils.getSendButtonColor(requireContext()),
                ConfigUtils.getSecondaryColorOnMainBackground(requireContext())
            )
        )
        binding.send.setTextColor(stateList)

        binding.attach.setColorFilter(ConfigUtils.getFileMenuButtonColor(requireContext()))

        binding.input.highlightColor = accentColor
        binding.input.setCursorColor(accentColor)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            binding.input.textCursorDrawable = null
        }
        binding.input.setHintTextColor(ConfigUtils.getSecondaryColorOnMainBackground(requireContext()))
        binding.input.setTextColor(ConfigUtils.getInputTextColor(requireContext()))

        binding.divider.setBackgroundColor(
            getColorOnBackground(
                ConfigUtils.getMainBackgroundColor(requireContext()),
                30
            )
        )

        binding.refresh.setProgressBackgroundColor(ConfigUtils.getMainBackgroundColor(requireContext()))
        binding.refresh.setColorSchemeColors(ConfigUtils.getAccentColor(requireContext()))

        with(requireActivity().window) {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = ConfigUtils.getStatusBarColor(requireContext()) ?: statusBarColor
        }

    }

    private fun onMenuItemClicked(menuItem: MenuItem?): Boolean {
        menuItem ?: return false
        if (menuItem.itemId == R.id.psd_main_menu_close) {
            dispatch(TicketView.Event.OnCloseClick)
        }
        return true
    }

    private fun updateComments(comments: List<CommentEntryV2>?) {
        adapter.submitList(comments?.reversed()) {
            val layoutManager = (binding.comments.layoutManager as? LinearLayoutManager) ?: return@submitList
            val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
            if (firstVisiblePosition == 0) {
                layoutManager.scrollToPosition(0)
            }
        }
    }

    companion object {
        private const val STATE_KEYBOARD_SHOWN = "STATE_KEYBOARD_SHOWN"
        private const val KEY_TICKET_ID = "KEY_TICKET_ID"
        private const val KEY_USER_INTERNAL = "KEY_USER_INTERNAL"

        fun newInstance(ticketId: Long, user: UserInternal): TicketFragment {
            val fragment = TicketFragment()
            val args = Bundle().apply {
                putParcelable(KEY_USER_INTERNAL, user)
                putLong(KEY_TICKET_ID, ticketId)
            }
            fragment.arguments = args
            return fragment
        }
    }

}