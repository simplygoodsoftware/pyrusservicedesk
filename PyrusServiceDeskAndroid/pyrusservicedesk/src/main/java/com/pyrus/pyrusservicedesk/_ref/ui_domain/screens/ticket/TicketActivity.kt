package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.ServiceDeskConfiguration
import com.pyrus.pyrusservicedesk.databinding.PsdActivityTicketBinding
import com.pyrus.pyrusservicedesk.presentation.ConnectionActivityBase
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.TicketAdapter
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.TicketViewModel
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.dialogs.attach_files.AttachFileSharedViewModel
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.dialogs.comment_actions.PendingCommentActionSharedViewModel
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.item_decorators.SpaceItemDecoration
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk._ref.utils.getColorOnBackground
import com.pyrus.pyrusservicedesk._ref.utils.getSecondaryColorOnBackground
import com.pyrus.pyrusservicedesk._ref.utils.getViewModel
import com.pyrus.pyrusservicedesk._ref.utils.isAtEnd
import com.pyrus.pyrusservicedesk._ref.utils.setCursorColor
import com.pyrus.pyrusservicedesk._ref.utils.setupWindowInsets
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Activity for rendering ticket/feed comments.
 */
internal class TicketActivity : ConnectionActivityBase<TicketViewModel>(TicketViewModel::class.java) {

    override val layoutResId = R.layout.psd_activity_ticket
    override val toolbarViewId = R.id.ticket_toolbar
    override val refresherViewId = R.id.refresh
    override val progressBarViewId: Int = View.NO_ID
    
    private fun dispatch(event: TicketView.Event) {
        
    }

    private lateinit var binding: PsdActivityTicketBinding

    private val attachFileSharedViewModel: AttachFileSharedViewModel by getViewModel(
        AttachFileSharedViewModel::class.java)

    private val commentActionsSharedViewModel: PendingCommentActionSharedViewModel by getViewModel(
        PendingCommentActionSharedViewModel::class.java)

    private val adapter: TicketAdapter by lazy {
        TicketAdapter(
            { dispatch(TicketView.Event.OnRetryClick) },
            { dispatch(TicketView.Event.OnPreviewClick) },
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = PsdActivityTicketBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        setupWindowInsets(binding.root)

        savedInstanceState?.let { ServiceDeskConfiguration.restore(it) }

        initUi()
        initListeners()

        // TODO Model.titleText
        binding.toolbarTitle.text = ConfigUtils.getTitle(this@TicketActivity)

        // TODO Model.sendEnabled
        binding.send.isEnabled = !binding.input.text.isNullOrBlank()
    }

    override fun onStart() {
        super.onStart()
//        viewModel.onStart()
    }

    override fun onStop() {
        super.onStop()
//        viewModel.onStop()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        savedInstanceState.let {
            if (it.getBoolean(STATE_KEYBOARD_SHOWN))
                showKeyboardOn(binding.input)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.let {
            ServiceDeskConfiguration.save(it)
            it.putBoolean(STATE_KEYBOARD_SHOWN, binding.input.hasFocus())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val menuDelegate = ConfigUtils.getMainMenuDelegate()
        if (menuDelegate != null && menu != null)
            return menuDelegate.onCreateOptionsMenu(menu, this)

        return menu?.let{
            MenuInflater(this).inflate(R.menu.psd_main_menu, menu)
            val closeItem = menu.findItem(R.id.psd_main_menu_close)
            closeItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            closeItem.icon?.setColorFilter(
                ConfigUtils.getToolbarButtonColor(this),
                PorterDuff.Mode.SRC_ATOP
            )
            true
        } ?: false
    }

    override fun startObserveData() {
        super.startObserveData()
        viewModel.getCommentDiffLiveData().observe(this) { result ->
            val atEnd = binding.comments.isAtEnd()
            val isEmpty = binding.comments.adapter?.itemCount == 0
            result?.let {
                binding.refresh.isRefreshing = false
                TODO()
//                adapter.setItems(it.newItems)
                it.diffResult.dispatchUpdatesTo(adapter)
            }
            if (adapter.itemCount > 0 && atEnd) {
                if (isEmpty)
                    binding.comments.scrollToPosition(adapter.itemCount - 1)
                else if (!binding.comments.isAtEnd()) {
                    binding.comments.smoothScrollToPosition(adapter.itemCount - 1)
                }
                launch {
                    while (!binding.comments.isAtEnd()) {
                        delay(CHECK_IS_AT_BOTTOM_DELAY_MS)
                    }
                    val offset = when {
                        binding.comments.childCount > 0 -> binding.comments.getChildAt(binding.comments.childCount - 1).height
                        else -> 0
                    }
                    binding.comments.smoothScrollBy(0, offset)
                }
            }
        }

        // TODO replace with result listener
        attachFileSharedViewModel.getFilePickedLiveData().observe(this) { fileUri ->
            dispatch(TicketView.Event.OnAttachmentClick(fileUri))
        }

        // TODO
        commentActionsSharedViewModel.getSelectedActionLiveData().observe(this) { action ->
            action ?: return@observe
            when {
                PendingCommentActionSharedViewModel.isRetryClicked(action) -> viewModel.onPendingCommentRetried()
                PendingCommentActionSharedViewModel.isDeleteClicked(action) -> viewModel.onPendingCommentDeleted()
                PendingCommentActionSharedViewModel.isCancelled(action) -> viewModel.onChoosingCommentActionCancelled()
            }
        }

    }

    override fun onViewHeightChanged(changedBy: Int) {
        super.onViewHeightChanged(changedBy)
        when {
            changedBy == 0 -> return
            changedBy > 0 -> binding.comments.scrollBy(0, changedBy)
            else -> {
                binding.input.clearFocus()
                if (!binding.comments.isAtEnd())
                    binding.comments.scrollBy(0, changedBy)
            }

        }
    }

    override fun finish() {
        super.finish()
        PyrusServiceDesk.onServiceDeskStop()
    }

    private fun onMenuItemClicked(menuItem: MenuItem?): Boolean {
        menuItem ?: return false
        if (menuItem.itemId == R.id.psd_main_menu_close) {
            dispatch(TicketView.Event.OnCloseClick)
        }
        return true
    }

    private fun initListeners() {
        binding.ticketToolbar.setOnMenuItemClickListener { onMenuItemClicked(it) }
        binding.send.setOnClickListener { dispatch(TicketView.Event.OnSendClick) }
        binding.attach.setOnClickListener { dispatch(TicketView.Event.OnShowAttachVariantsClick) }
        binding.input.addTextChangedListener(inputTextWatcher)
    }

    private fun initUi() {
        // if you don't set empty text, Android will set the app name
        supportActionBar?.apply { title = "" }

        binding.comments.apply {
            adapter = this@TicketActivity.adapter
            addItemDecoration(
                SpaceItemDecoration(
                    resources.getDimensionPixelSize(R.dimen.psd_comments_item_space),
                    this@TicketActivity.adapter.itemSpaceMultiplier
                )
            )
            itemAnimator = null
        }

        applyStyle()
    }
    
    private fun applyStyle() {
        val accentColor = ConfigUtils.getAccentColor(this)

        binding.root.setBackgroundColor(ConfigUtils.getMainBackgroundColor(this))

        val toolbarColor = ConfigUtils.getHeaderBackgroundColor(this)
        binding.toolbarTitle.setTextColor(
            ConfigUtils.getChatTitleTextColor(
                this
            )
        )
        binding.ticketToolbar.setBackgroundColor(toolbarColor)

        ConfigUtils.getMainFontTypeface()?.let {
            binding.send.typeface = it
            binding.input.typeface = it
            binding.noConnection.noConnectionTextView.typeface = it
            binding.noConnection.reconnectButton.typeface = it
        }

        val secondaryColor =
            getSecondaryColorOnBackground(ConfigUtils.getNoPreviewBackgroundColor(this))
        binding.noConnection.noConnectionImageView.setColorFilter(secondaryColor)
        binding.noConnection.noConnectionTextView.setTextColor(secondaryColor)

        binding.noConnection.reconnectButton.setTextColor(ConfigUtils.getAccentColor(this))

        binding.noConnection.root.setBackgroundColor(ConfigUtils.getNoConnectionBackgroundColor(this))

        ConfigUtils.getMainBoldFontTypeface()?.let {
            binding.toolbarTitle.typeface = it
        }

        val stateList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_enabled),
                intArrayOf(-android.R.attr.state_enabled)
            ),
            intArrayOf(
                ConfigUtils.getSendButtonColor(this),
                ConfigUtils.getSecondaryColorOnMainBackground(this)
            )
        )
        binding.send.setTextColor(stateList)

        binding.attach.setColorFilter(ConfigUtils.getFileMenuButtonColor(this))

        binding.input.highlightColor = accentColor
        binding.input.setCursorColor(accentColor)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            binding.input.textCursorDrawable = null
        }
        binding.input.setHintTextColor(ConfigUtils.getSecondaryColorOnMainBackground(this@TicketActivity))
        binding.input.setTextColor(ConfigUtils.getInputTextColor(this@TicketActivity))

        binding.divider.setBackgroundColor(
            getColorOnBackground(
                ConfigUtils.getMainBackgroundColor(this),
                30
            )
        )

        binding.refresh.setProgressBackgroundColor(ConfigUtils.getMainBackgroundColor(this))
        binding.refresh.setColorSchemeColors(ConfigUtils.getAccentColor(this))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = ConfigUtils.getStatusBarColor(this) ?: window.statusBarColor
        }
    }

    companion object {
        private const val KEY_TICKET_ID = "KEY_TICKET_ID"
        private const val KEY_UNREAD_COUNT = "KEY_UNREAD_COUNT"

        private const val STATE_KEYBOARD_SHOWN = "STATE_KEYBOARD_SHOWN"

        private const val CHECK_IS_AT_BOTTOM_DELAY_MS = 50L

        /**
         * Provides intent for launching the screen.
         *
         * @param ticketId id of ticket to be rendered.
         * When not, this should be omitted for the new ticket.
         * @param unreadCount current count of unread tickets.
         */
        fun getLaunchIntent(ticketId:Int? = null, unreadCount: Int? = 0): Intent {
            return Intent(
                PyrusServiceDesk.get().application,
                TicketActivity::class.java
            ).also { intent ->

                ticketId?.let { intent.putExtra(KEY_TICKET_ID, it) }
                intent.putExtra(KEY_UNREAD_COUNT, unreadCount)
            }
        }

    }

}