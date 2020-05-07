package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket

import androidx.lifecycle.Observer
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MenuItem.SHOW_AS_ACTION_ALWAYS
import android.view.View.NO_ID
import android.widget.Toast
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.ServiceDeskConfiguration
import com.pyrus.pyrusservicedesk.presentation.ConnectionActivityBase
import com.pyrus.pyrusservicedesk.presentation.ui.navigation.UiNavigator
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.dialogs.attach_files.AttachFileSharedViewModel
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.dialogs.attach_files.AttachFileVariantsFragment
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.dialogs.comment_actions.PendingCommentActionSharedViewModel
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.dialogs.comment_actions.PendingCommentActionsDialog
import com.pyrus.pyrusservicedesk.presentation.ui.view.NavigationCounterDrawable
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.item_decorators.SpaceItemDecoration
import com.pyrus.pyrusservicedesk.sdk.data.Attachment
import com.pyrus.pyrusservicedesk.sdk.data.EMPTY_TICKET_ID
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileData
import com.pyrus.pyrusservicedesk.utils.*
import com.pyrus.pyrusservicedesk.utils.RequestUtils.Companion.getFileUrl
import kotlinx.android.synthetic.main.psd_activity_ticket.*
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
         * When [PyrusServiceDesk.isSingleChat] is used this should be omitted.
         * When not, this should be omitted for the new ticket.
         * @param unreadCount current count of unread tickets.
         * Can be omitted in [PyrusServiceDesk.isSingleChat] mode
         */
        fun getLaunchIntent(ticketId:Int? = null, unreadCount: Int? = 0): Intent {
            return Intent(
                    PyrusServiceDesk.get().application,
                    TicketActivity::class.java).also { intent ->

                ticketId?.let { intent.putExtra(KEY_TICKET_ID, it) }
                intent.putExtra(KEY_UNREAD_COUNT, unreadCount)
            }
        }

        /**
         * Extracts ticket id from the given [arguments].
         * Expected that [arguments] are made by [getLaunchIntent].
         *
         * @return id of the ticket stored in [arguments] or [EMPTY_TICKET_ID] if
         * [arguments] doesn't contain it
         */
        fun getTicketId(arguments: Intent): Int {
            return arguments.getIntExtra(KEY_TICKET_ID, EMPTY_TICKET_ID)
        }

        /**
         * Extracts unread ticket count from the [arguments].
         * Expected that [arguments] are made by [getLaunchIntent].
         *
         * @return count of the unread tickets, or 0 if it was not specified.
         */
        fun getUnreadTicketsCount(arguments: Intent): Int {
            return arguments.getIntExtra(KEY_UNREAD_COUNT, 0)
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
    private val navigationCounterIcon by lazy {
        NavigationCounterDrawable(
            this
        )
    }

    private val adapter = TicketAdapter().apply {
        setOnFileReadyForPreviewClickListener {
           UiNavigator.toFilePreview(this@TicketActivity, it.toFileData())
        }
        setOnErrorCommentEntryClickListener {
            viewModel.onUserStartChoosingCommentAction(it)
            PendingCommentActionsDialog().show(supportFragmentManager, "")
        }
        setOnTextCommentLongClicked {
            copyToClipboard(it)
        }
    }

    private val inputTextWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            send.isEnabled = !s.isNullOrBlank()
            viewModel.onInputTextChanged(s.toString())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.let { ServiceDeskConfiguration.restore(it) }

        val accentColor = ConfigUtils.getAccentColor(this)

        supportActionBar?.apply { title = ConfigUtils.getTitle(this@TicketActivity) }

        if (!viewModel.isFeed) {
            ticket_toolbar.navigationIcon = navigationCounterIcon
            ticket_toolbar.setNavigationOnClickListener { UiNavigator.toTickets(this@TicketActivity) }
        }
        ticket_toolbar.setOnMenuItemClickListener{ onMenuItemClicked(it) }
        comments.apply {
            adapter = this@TicketActivity.adapter
            addItemDecoration(
                SpaceItemDecoration(
                    resources.getDimensionPixelSize(R.dimen.psd_comments_item_space),
                    this@TicketActivity.adapter.itemSpaceMultiplier)
            )
            itemAnimator = null
            this@TicketActivity.adapter.itemTouchHelper?.attachToRecyclerView(this)
        }
        send.setOnClickListener { sendComment() }
        val stateList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_enabled),
                intArrayOf(-android.R.attr.state_enabled)
            ),
            intArrayOf(
                accentColor,
                getColorByAttrId(this, android.R.attr.textColorSecondary)
            )

        )
        send.setTextColor(stateList)
        attach.setOnClickListener { showAttachFileVariants() }
        attach.setColorFilter(accentColor)
        if(savedInstanceState == null) {
            input.setText(viewModel.draft)
            showKeyboardOn(input){
                input.setSelection(input.length())
            }
        }
        input.apply {
            highlightColor = accentColor
            setCursorColor(accentColor)
            addTextChangedListener(inputTextWatcher)
        }
        send.isEnabled = !input.text.isNullOrBlank()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        savedInstanceState.let {
            if (it.getBoolean(STATE_KEYBOARD_SHOWN))
                showKeyboardOn(input)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.let {
            ServiceDeskConfiguration.save(it)
            it.putBoolean(STATE_KEYBOARD_SHOWN, input.hasFocus())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return menu?.let{
            MenuInflater(this).inflate(R.menu.psd_main_menu, menu)
            menu.findItem(R.id.psd_main_menu_close).setShowAsAction(SHOW_AS_ACTION_ALWAYS)
            true
        } ?: false
    }

    override fun startObserveData() {
        super.startObserveData()
        viewModel.getCommentDiffLiveData().observe(
            this,
            Observer { result ->
                val atEnd = comments.isAtEnd()
                val isEmpty = comments.adapter?.itemCount == 0
                result?.let{
                    refresh.isRefreshing = false
                    adapter.setItems(it.newItems)
                    it.diffResult.dispatchUpdatesTo(adapter)
                }
                if (adapter.itemCount > 0 && atEnd){
                    if (isEmpty)
                        comments.scrollToPosition(adapter.itemCount - 1)
                    else if (!comments.isAtEnd())
                        comments.smoothScrollToPosition(adapter.itemCount - 1)
                    launch {
                        while(!comments.isAtEnd())
                            delay(CHECK_IS_AT_BOTTOM_DELAY_MS)
                        val offset = when {
                            comments.childCount > 0 -> comments.getChildAt(comments.childCount - 1).height
                            else -> 0
                        }
                        comments.smoothScrollBy(0, offset)
                    }
                }
            }
        )
        viewModel.getUnreadCounterLiveData().observe(
            this,
            Observer { it?.let { count ->
                if (!viewModel.isFeed)
                    navigationCounterIcon.counter = count
            } }
        )
        attachFileSharedViewModel.getFilePickedLiveData().observe(
            this,
            Observer { fileUri ->
                fileUri?.let {
                    viewModel.onAttachmentSelected(it)
                }
            }
        )
        commentActionsSharedViewModel.getSelectedActionLiveData().observe(
            this,
            Observer { action ->
                action?.let {
                    when{
                        PendingCommentActionSharedViewModel.isRetryClicked(it) -> viewModel.onPendingCommentRetried()
                        PendingCommentActionSharedViewModel.isDeleteClicked(it) -> viewModel.onPendingCommentDeleted()
                        PendingCommentActionSharedViewModel.isCancelled(it) -> viewModel.onChoosingCommentActionCancelled()
                    }
                }
            }
        )
    }

    override fun onViewHeightChanged(changedBy: Int) {
        super.onViewHeightChanged(changedBy)
        when {
            changedBy == 0 -> return
            changedBy > 0 -> comments.scrollBy(0, changedBy)
            else -> {
                input.clearFocus()
                if (!comments.isAtEnd())
                    comments.scrollBy(0, changedBy)
            }

        }
    }

    override fun finish() {
        super.finish()
        if (PyrusServiceDesk.get().isSingleChat)
            PyrusServiceDesk.onServiceDeskStop()
    }

    private fun onMenuItemClicked(menuItem: MenuItem?): Boolean {
        return menuItem?.let {
            when (it.itemId) {
                R.id.psd_main_menu_close -> quitViewModel.quitServiceDesk()
            }
            true
        } ?: false
    }

    private fun sendComment() {
        viewModel.onSendClicked(input.text.toString())
        input.text = null
    }

    private fun showAttachFileVariants() {
        AttachFileVariantsFragment()
            .show(supportFragmentManager, "")
    }

    private fun copyToClipboard(text: String) {
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).apply {
            setPrimaryClip(ClipData.newPlainText("Copied text", text))
        }
        Toast.makeText(applicationContext, R.string.psd_copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }

}

private fun Attachment.toFileData(): FileData {
    return FileData(
        name,
        bytesSize,
        if (isLocal() && localUri != null) localUri else Uri.parse(getFileUrl(id)),
        isLocal()
    )
}
