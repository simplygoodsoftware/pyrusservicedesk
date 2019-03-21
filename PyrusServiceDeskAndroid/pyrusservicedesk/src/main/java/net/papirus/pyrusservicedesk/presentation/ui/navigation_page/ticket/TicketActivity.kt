package net.papirus.pyrusservicedesk.presentation.ui.navigation_page.ticket

import android.arch.lifecycle.Observer
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MenuItem.SHOW_AS_ACTION_ALWAYS
import com.example.pyrusservicedesk.R
import kotlinx.android.synthetic.main.psd_activity_ticket.*
import net.papirus.pyrusservicedesk.PyrusServiceDesk
import net.papirus.pyrusservicedesk.presentation.ConnectionActivityBase
import net.papirus.pyrusservicedesk.presentation.ui.navigation.UiNavigator
import net.papirus.pyrusservicedesk.presentation.ui.view.NavigationCounterDrawable
import net.papirus.pyrusservicedesk.presentation.ui.view.recyclerview.item_decorators.SpaceItemDecoration
import net.papirus.pyrusservicedesk.sdk.data.EMPTY_TICKET_ID
import net.papirus.pyrusservicedesk.utils.ThemeUtils
import net.papirus.pyrusservicedesk.utils.getViewModel
import net.papirus.pyrusservicedesk.utils.isAtEnd
import net.papirus.pyrusservicedesk.utils.setCursorColor

internal class TicketActivity : ConnectionActivityBase<TicketViewModel>(TicketViewModel::class.java) {

    companion object {
        private const val KEY_TICKET_ID = "KEY_TICKET_ID"
        private const val KEY_UNREAD_COUNT = "KEY_UNREAD_COUNT"

        fun getLaunchIntent(ticketId:Int? = null, unreadCount: Int? = 0): Intent {
            return Intent(
                    PyrusServiceDesk.getInstance().application,
                    TicketActivity::class.java).also { intent ->

                ticketId?.let {intent.putExtra(KEY_TICKET_ID, it)}
                intent.putExtra(KEY_UNREAD_COUNT, unreadCount)
            }
        }

        fun getTicketId(arguments: Intent): Int {
            return arguments.getIntExtra(KEY_TICKET_ID, EMPTY_TICKET_ID)
        }

        fun getUnreadTicketsCount(arguments: Intent): Int {
            return arguments.getIntExtra(KEY_UNREAD_COUNT, 0)
        }
    }

    override val layoutResId = R.layout.psd_activity_ticket
    override val toolbarViewId = R.id.ticket_toolbar
    override val refresherViewId = R.id.refresh

    private val ticketSharedViewModel: TicketSharedViewModel by getViewModel(TicketSharedViewModel::class.java)
    private val navigationCounterIcon by lazy {
        NavigationCounterDrawable(
            this
        )
    }

    private var adapter = TicketAdapter().apply {
        setOnDownloadedFileClickListener {
           UiNavigator.toFilePreview(this@TicketActivity, it.id, it.name)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val accentColor = ThemeUtils.getAccentColor(this)

        supportActionBar?.apply { title = ThemeUtils.getTitle(this@TicketActivity) }

        if (!viewModel.isFeed) {
            ticket_toolbar.navigationIcon = navigationCounterIcon
            ticket_toolbar.setNavigationOnClickListener { UiNavigator.toTickets(this@TicketActivity) }
        }
        ticket_toolbar.setOnMenuItemClickListener{ onMenuItemClicked(it) }
        comments.apply {
            adapter = this@TicketActivity.adapter
            addItemDecoration(
                    SpaceItemDecoration(resources.getInteger(R.integer.psd_comments_item_space)))
            this@TicketActivity.adapter.itemTouchHelper?.attachToRecyclerView(this)
        }
        send.setOnClickListener { sendComment() }
        send.setTextColor(accentColor)
        attach.setOnClickListener { showAttachFileVariants() }
        attach.setColorFilter(accentColor)
        input.highlightColor = accentColor
        input.setCursorColor(accentColor)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return menu?.let{
            MenuInflater(this).inflate(R.menu.psd_main_menu, menu)
            menu.findItem(R.id.psd_main_menu_close).setShowAsAction(SHOW_AS_ACTION_ALWAYS)
            true
        } ?: false
    }

    override fun observeData() {
        super.observeData()
        viewModel.getCommentDiffLiveData().observe(
            this,
            Observer { result ->
                val atEnd = comments.isAtEnd()
                result?.let{
                    refresh.isRefreshing = false
                    adapter.setItemsWithoutUpdate(it.newItems)
                    it.diffResult.dispatchUpdatesTo(adapter)
                }
                if (adapter.itemCount == 0 || atEnd)
                    comments.scrollToPosition(adapter.itemCount - 1)
            }
        )
        viewModel.getUnreadCounterLiveData().observe(
            this,
            Observer { it?.let { count -> navigationCounterIcon.counter = count } }
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
                R.id.psd_main_menu_close -> sharedViewModel.quitServiceDesk()
            }
            true
        } ?: false
    }

    private fun sendComment() {
        if (!input.text.isNullOrBlank()) {
            viewModel.addComment(input.text.toString())
            input.text = null
        }
    }

    private fun showAttachFileVariants() {
        AttachFileVariantsFragment().show(supportFragmentManager, "")
    }
}
