package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MenuItem.SHOW_AS_ACTION_ALWAYS
import android.view.View.NO_ID
import android.view.WindowManager
import android.widget.Toast
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.ServiceDeskConfiguration
import com.pyrus.pyrusservicedesk.presentation.ConnectionActivityBase
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.file_preview.FilePreviewActivity
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.TicketAdapter
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.dialogs.attach_files.AttachFileSharedViewModel
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.dialogs.attach_files.AttachFileVariantsFragment
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.dialogs.comment_actions.PendingCommentActionSharedViewModel
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.dialogs.comment_actions.PendingCommentActionsDialog
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.item_decorators.SpaceItemDecoration
import com.pyrus.pyrusservicedesk.sdk.data.Attachment
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileData
import com.pyrus.pyrusservicedesk._ref.utils.*
import com.pyrus.pyrusservicedesk._ref.utils.RequestUtils.Companion.getFileUrl
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Activity for rendering ticket/feed comments.
 */
internal class TicketActivity : ConnectionActivityBase<TicketViewModel>(TicketViewModel::class.java) {

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
                    TicketActivity::class.java).also { intent ->

                ticketId?.let { intent.putExtra(KEY_TICKET_ID, it) }
                intent.putExtra(KEY_UNREAD_COUNT, unreadCount)
            }
        }

    }

    override val layoutResId = R.layout.psd_activity_ticket
    override val toolbarViewId = R.id.ticket_toolbar
    override val refresherViewId = R.id.refresh
    override val progressBarViewId: Int = NO_ID

    private val attachFileSharedViewModel: AttachFileSharedViewModel by getViewModel(
        AttachFileSharedViewModel::class.java)
    private val commentActionsSharedViewModel: PendingCommentActionSharedViewModel by getViewModel(
        PendingCommentActionSharedViewModel::class.java)
    private val adapter: TicketAdapter = TODO()
//    private val adapter = TicketAdapter().apply {
//        setOnFileReadyForPreviewClickListener { attachment ->
//            val fileData = attachment.toFileData()
//            if (fileData.isLocal) {
//                return@setOnFileReadyForPreviewClickListener
//            }
//            startActivity(FilePreviewActivity.getLaunchIntent(fileData))
//        }
//        setOnErrorCommentEntryClickListener {
//            viewModel.onUserStartChoosingCommentAction(it)
//            PendingCommentActionsDialog().show(supportFragmentManager, "")
//        }
//        setOnTextCommentLongClicked {
//            copyToClipboard(it)
//        }
//        setOnRatingClickListener { rating ->
//            viewModel.onRatingClick(rating)
//        }
//    }

    private val inputTextWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//            send.isEnabled = !s.isNullOrBlank()
            viewModel.onInputTextChanged(s.toString())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.let { ServiceDeskConfiguration.restore(it) }

        val accentColor = ConfigUtils.getAccentColor(this)

        // if you don't set empty text, Android will set the app name
        supportActionBar?.apply { title = "" }
//        ticket_toolbar.toolbar_title.text = ConfigUtils.getTitle(this@TicketActivity)

//        root.setBackgroundColor(ConfigUtils.getMainBackgroundColor(this))

        val toolbarColor = ConfigUtils.getHeaderBackgroundColor(this)
//        ticket_toolbar.toolbar_title.setTextColor(
//            ConfigUtils.getChatTitleTextColor(
//                this
//            )
//        )
//        ticket_toolbar.setBackgroundColor(toolbarColor)

//        ConfigUtils.getMainFontTypeface()?.let {
//            send.typeface = it
//            input.typeface = it
//            noConnectionTextView.typeface = it
//            reconnectButton.typeface = it
//        }

        val secondaryColor = getSecondaryColorOnBackground(ConfigUtils.getNoPreviewBackgroundColor(this))
//        noConnectionImageView.setColorFilter(secondaryColor)
//        noConnectionTextView.setTextColor(secondaryColor)
//        reconnectButton.setTextColor(ConfigUtils.getAccentColor(this))
//        no_connection.setBackgroundColor(ConfigUtils.getNoConnectionBackgroundColor(this))

        ConfigUtils.getMainBoldFontTypeface()?.let {
//            ticket_toolbar.toolbar_title.typeface = it
        }

//        ticket_toolbar.setOnMenuItemClickListener{ onMenuItemClicked(it) }
//        comments.apply {
//            adapter = this@TicketActivity.adapter
//            addItemDecoration(
//                SpaceItemDecoration(
//                    resources.getDimensionPixelSize(R.dimen.psd_comments_item_space),
//                    this@TicketActivity.adapter.itemSpaceMultiplier)
//            )
//            itemAnimator = null
//        }
//        send.setOnClickListener { onSendCommentClick() }
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
//        send.setTextColor(stateList)
//        attach.setOnClickListener { showAttachFileVariants() }
//        attach.setColorFilter(ConfigUtils.getFileMenuButtonColor(this))
        if(savedInstanceState == null) {
//            input.setText(viewModel.draft)
//            showKeyboardOn(input){
//                input.setSelection(input.length())
//            }
        }
//        input.apply {
//            highlightColor = accentColor
//            setCursorColor(accentColor)
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                textCursorDrawable = null
//            }
//            setHintTextColor(ConfigUtils.getSecondaryColorOnMainBackground(this@TicketActivity))
//            setTextColor(ConfigUtils.getInputTextColor(this@TicketActivity))
//            addTextChangedListener(inputTextWatcher)
//        }
//        send.isEnabled = !input.text.isNullOrBlank()
//        divider.setBackgroundColor(getColorOnBackground(ConfigUtils.getMainBackgroundColor(this), 30))
//        refresh.setProgressBackgroundColor(ConfigUtils.getMainBackgroundColor(this))
//        refresh.setColorSchemeColors(ConfigUtils.getAccentColor(this))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = ConfigUtils.getStatusBarColor(this)?: window.statusBarColor
        }
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
            if (it.getBoolean(STATE_KEYBOARD_SHOWN)) {
//                showKeyboardOn(input)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.let {
            ServiceDeskConfiguration.save(it)
//            it.putBoolean(STATE_KEYBOARD_SHOWN, input.hasFocus())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val menuDelegate = ConfigUtils.getMainMenuDelegate()
        if (menuDelegate != null && menu != null)
            return menuDelegate.onCreateOptionsMenu(menu, this)

        return menu?.let{
            MenuInflater(this).inflate(R.menu.psd_main_menu, menu)
            val closeItem = menu.findItem(R.id.psd_main_menu_close)
            closeItem.setShowAsAction(SHOW_AS_ACTION_ALWAYS)
            closeItem.icon?.setColorFilter(ConfigUtils.getToolbarButtonColor(this), PorterDuff.Mode.SRC_ATOP)
            true
        } ?: false
    }

    override fun startObserveData() {
        super.startObserveData()
        viewModel.getCommentDiffLiveData().observe(this) { result ->
//            val atEnd = comments.isAtEnd()
//            val isEmpty = comments.adapter?.itemCount == 0
            result?.let {
//                refresh.isRefreshing = false
                adapter.setItems(it.newItems)
                it.diffResult.dispatchUpdatesTo(adapter)
            }
//            if (adapter.itemCount > 0 && atEnd) {
//                if (isEmpty)
//                    comments.scrollToPosition(adapter.itemCount - 1)
//                else if (!comments.isAtEnd()) {
//                    comments.smoothScrollToPosition(adapter.itemCount - 1)
//                }
//                launch {
//                    while (!comments.isAtEnd()) {
//                        delay(CHECK_IS_AT_BOTTOM_DELAY_MS)
//                    }
//                    val offset = when {
//                        comments.childCount > 0 -> comments.getChildAt(comments.childCount - 1).height
//                        else -> 0
//                    }
//                    comments.smoothScrollBy(0, offset)
//                }
//            }
        }
        attachFileSharedViewModel.getFilePickedLiveData().observe(this) { fileUri ->
            fileUri?.let {
                viewModel.onAttachmentSelected(it)
            }
        }

        commentActionsSharedViewModel.getSelectedActionLiveData().observe(this) { action ->
            action?.let {
                when {
                    PendingCommentActionSharedViewModel.isRetryClicked(it) -> viewModel.onPendingCommentRetried()
                    PendingCommentActionSharedViewModel.isDeleteClicked(it) -> viewModel.onPendingCommentDeleted()
                    PendingCommentActionSharedViewModel.isCancelled(it) -> viewModel.onChoosingCommentActionCancelled()
                }
            }
        }

    }

    override fun onViewHeightChanged(changedBy: Int) {
        super.onViewHeightChanged(changedBy)
//        when {
//            changedBy == 0 -> return
////            changedBy > 0 -> comments.scrollBy(0, changedBy)
//            else -> {
//                input.clearFocus()
//                if (!comments.isAtEnd())
//                    comments.scrollBy(0, changedBy)
//            }
//
//        }
    }

    override fun finish() {
        super.finish()
        PyrusServiceDesk.onServiceDeskStop()
    }

    private fun onMenuItemClicked(menuItem: MenuItem?): Boolean {
        val menuDelegate = ConfigUtils.getMainMenuDelegate()
        if (menuDelegate != null && menuItem != null)
            return menuDelegate.onOptionsItemSelected(menuItem, this)

        return menuItem?.let {
            when (it.itemId) {
                R.id.psd_main_menu_close -> sharedViewModel.quitServiceDesk()
            }
            true
        } ?: false
    }

    private fun onSendCommentClick() {
//        viewModel.onSendClicked(input.text.toString())
//        input.text = null
    }

    private fun showAttachFileVariants() {
        AttachFileVariantsFragment().show(supportFragmentManager, "")
    }

    private fun copyToClipboard(text: String) {
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).apply {
            setPrimaryClip(ClipData.newPlainText("Copied text", text))
        }
        Toast.makeText(applicationContext, R.string.psd_copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }

}

private fun Attachment.toFileData(): FileData {
    TODO()
//    return FileData(
//        name,
//        bytesSize,
//        if (isLocal() && localUri != null) localUri else Uri.parse(getFileUrl(id, PyrusServiceDesk.get().domain)),
//        isLocal()
//    )
}
