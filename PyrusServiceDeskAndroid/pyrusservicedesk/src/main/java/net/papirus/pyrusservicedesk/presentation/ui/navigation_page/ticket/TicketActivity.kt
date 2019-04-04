package net.papirus.pyrusservicedesk.presentation.ui.navigation_page.ticket

import android.arch.lifecycle.Observer
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
import com.example.pyrusservicedesk.R
import kotlinx.android.synthetic.main.psd_activity_ticket.*
import net.papirus.pyrusservicedesk.PyrusServiceDesk
import net.papirus.pyrusservicedesk.ServiceDeskConfiguration
import net.papirus.pyrusservicedesk.presentation.ConnectionActivityBase
import net.papirus.pyrusservicedesk.presentation.ui.navigation.UiNavigator
import net.papirus.pyrusservicedesk.presentation.ui.view.NavigationCounterDrawable
import net.papirus.pyrusservicedesk.presentation.ui.view.recyclerview.item_animator.ItemAnimatorNoFade
import net.papirus.pyrusservicedesk.presentation.ui.view.recyclerview.item_decorators.SpaceItemDecoration
import net.papirus.pyrusservicedesk.sdk.data.Attachment
import net.papirus.pyrusservicedesk.sdk.data.EMPTY_TICKET_ID
import net.papirus.pyrusservicedesk.sdk.data.intermediate.FileData
import net.papirus.pyrusservicedesk.utils.ConfigUtils
import net.papirus.pyrusservicedesk.utils.RequestUtils.Companion.getFileUrl
import net.papirus.pyrusservicedesk.utils.getViewModel
import net.papirus.pyrusservicedesk.utils.isAtEnd
import net.papirus.pyrusservicedesk.utils.setCursorColor

/**
 * Activity for rendering ticket/feed comments.
 */
internal class TicketActivity : ConnectionActivityBase<TicketViewModel>(TicketViewModel::class.java) {

    companion object {
        private const val KEY_TICKET_ID = "KEY_TICKET_ID"
        private const val KEY_UNREAD_COUNT = "KEY_UNREAD_COUNT"

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
                    PyrusServiceDesk.getInstance().application,
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

    private val ticketSharedViewModel: TicketSharedViewModel by getViewModel(TicketSharedViewModel::class.java)
    private val navigationCounterIcon by lazy {
        NavigationCounterDrawable(
            this
        )
    }

    private val adapter = TicketAdapter().apply {
        setOnFileReadyForPreviewClickListener {
           UiNavigator.toFilePreview(this@TicketActivity, it.toFileData())
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
            itemAnimator = ItemAnimatorNoFade()
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
                net.papirus.pyrusservicedesk.utils.getColor(this, android.R.attr.textColorSecondary)
            )

        )
        send.setTextColor(stateList)
        attach.setOnClickListener { showAttachFileVariants() }
        attach.setColorFilter(accentColor)
        if(savedInstanceState == null)
            input.setText(viewModel.draft)
        input.highlightColor = accentColor
        input.setCursorColor(accentColor)
        input.addTextChangedListener(inputTextWatcher)
        send.isEnabled = !input.text.isNullOrBlank()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.let {
            ServiceDeskConfiguration.save(it)
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
                result?.let{
                    refresh.isRefreshing = false
                    adapter.setItems(it.newItems)
                    it.diffResult.dispatchUpdatesTo(adapter)
                }
                if (atEnd){
                    comments.scrollToPosition(adapter.itemCount - 1)
                    comments.post {
                        if(comments == null)
                            return@post
                        val offset = when {
                            comments.childCount > 0 -> comments.getChildAt(comments.childCount - 1).height
                            else -> 0
                        }
                        comments.scrollBy(0, offset)
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
        ticketSharedViewModel.getFilePickedLiveData().observe(
            this,
            Observer { fileUri ->
                fileUri?.let {
                    viewModel.onAttachmentSelected(it)
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
        AttachFileVariantsFragment().show(supportFragmentManager, "")
    }

    private fun copyToClipboard(text: String) {
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).apply {
            primaryClip = ClipData.newPlainText("Copied text", text)
        }
        Toast.makeText(applicationContext, R.string.psd_copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }
}

private fun Attachment.toFileData(): FileData {
    return FileData(
        name,
        bytesSize,
        uri ?: Uri.parse(getFileUrl(id))
    )
}
