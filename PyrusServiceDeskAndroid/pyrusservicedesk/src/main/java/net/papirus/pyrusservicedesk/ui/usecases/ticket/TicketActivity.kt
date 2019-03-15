package net.papirus.pyrusservicedesk.ui.usecases.ticket

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
import net.papirus.pyrusservicedesk.sdk.data.EMPTY_TICKET_ID
import net.papirus.pyrusservicedesk.ui.ConnectionActivityBase
import net.papirus.pyrusservicedesk.ui.navigation.UiNavigator
import net.papirus.pyrusservicedesk.ui.view.recyclerview.item_decorators.SpaceItemDecoration
import net.papirus.pyrusservicedesk.utils.getViewModel

internal class TicketActivity : ConnectionActivityBase<TicketViewModel>(TicketViewModel::class.java) {

    companion object {
        private const val KEY_TICKET_ID = "KEY_TICKET_ID"

        private const val REQUEST_PICK_IMAGE = 0

        fun getTicketId(arguments: Intent): Int {
            return arguments.getIntExtra(KEY_TICKET_ID, EMPTY_TICKET_ID)
        }

        fun getLaunchIntent(ticketId:Int? = null): Intent {
            return Intent(
                    PyrusServiceDesk.getInstance().application,
                    TicketActivity::class.java).also { intent ->

                ticketId?.let {intent.putExtra(KEY_TICKET_ID, it)}
            }
        }

        private fun isExpectedResult(requestCode: Int): Boolean {
            return requestCode == REQUEST_PICK_IMAGE
        }
    }

    override val layoutResId = R.layout.psd_activity_ticket
    override val toolbarViewId = R.id.ticket_toolbar

    private val ticketSharedViewModel: TicketSharedViewModel by getViewModel(TicketSharedViewModel::class.java)

    private var adapter = TicketAdapter().apply {
        setOnDownloadedFileClickListener {
           UiNavigator.toFilePreview(this@TicketActivity, it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.apply { title = getString(R.string.psd_organization_support, viewModel.organizationName) }
        ticket_toolbar.setNavigationIcon(R.drawable.psd_menu)
        ticket_toolbar.setNavigationOnClickListener { UiNavigator.toTickets(this@TicketActivity) }
        ticket_toolbar.setOnMenuItemClickListener{ onMenuItemClicked(it) }
        comments.apply {
            adapter = this@TicketActivity.adapter
            addItemDecoration(
                    SpaceItemDecoration(resources.getInteger(R.integer.psd_comments_item_space)))
            this@TicketActivity.adapter.itemTouchHelper?.attachToRecyclerView(this)
        }
        send.setOnClickListener { sendComment() }
        attach.setOnClickListener { showAttachFileVariants() }
    }

    private fun onMenuItemClicked(menuItem: MenuItem?): Boolean {
        return menuItem?.let {
            when (it.itemId) {
                R.id.psd_main_menu_close -> sharedViewModel.quitServiceDesk()
            }
            true
        } ?: false
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return menu?.let{
            MenuInflater(this).inflate(R.menu.psd_main_menu, menu)
            menu.findItem(R.id.psd_main_menu_close).setShowAsAction(SHOW_AS_ACTION_ALWAYS)
            true
        } ?: false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (!isExpectedResult(requestCode) || resultCode != RESULT_OK)
            return
        data?.data?.let {
            viewModel.addAttachment(it)
        }
    }

    override fun observeData() {
        super.observeData()
        viewModel.getCommentDiffLiveData().observe(
            this,
            Observer { result ->
                result?.let{
                    adapter.setItemsWithoutUpdate(it.newItems)
                    it.diffResult.dispatchUpdatesTo(adapter)
                }
                comments.scrollToPosition(adapter.itemCount - 1)
            }
        )
        ticketSharedViewModel.getFilePickedLiveData().observe(
            this,
            Observer { fileUri ->
                fileUri?.let {
                    viewModel.addAttachment(it)
                }
            }
        )
    }

    override fun onViewHeightChanged(changedBy: Int) {
        super.onViewHeightChanged(changedBy)
        comments.scrollBy(0, changedBy)
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
