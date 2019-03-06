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
import net.papirus.pyrusservicedesk.repository.data.EMPTY_TICKET_ID
import net.papirus.pyrusservicedesk.ui.ActivityBase
import net.papirus.pyrusservicedesk.ui.ConnectionActivityBase
import net.papirus.pyrusservicedesk.ui.view.recyclerview.item_decorators.SpaceItemDecoration
import net.papirus.pyrusservicedesk.utils.INTENT_IMAGE_TYPE

internal class TicketActivity : ConnectionActivityBase<TicketViewModel>(TicketViewModel::class.java) {

    companion object {
        private const val KEY_TICKET_ID = "KEY_TICKET_ID"

        private const val REQUEST_PICK_IMAGE = 0

        fun startNewTicket(source: ActivityBase) = source.startActivity(getIntent())

        fun startTicket(source: ActivityBase, ticketId: Int) {
            source.startActivity(getIntent().putExtra(KEY_TICKET_ID, ticketId))
        }

        fun getTicketId(arguments: Intent): Int {
            return arguments.getIntExtra(KEY_TICKET_ID, EMPTY_TICKET_ID)
        }

        private fun getIntent(): Intent {
            return Intent(
                    PyrusServiceDesk.getInstance().application,
                    TicketActivity::class.java)
        }

        private fun isExpectedResult(requestCode: Int): Boolean {
            return requestCode == REQUEST_PICK_IMAGE
        }
    }

    override val layoutResId = R.layout.psd_activity_ticket
    override val toolbarViewId = R.id.ticket_toolbar

    private var adapter = TicketAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.apply { title = "Pyrus Support" }
        ticket_toolbar.setNavigationIcon(R.drawable.psd_menu)
        ticket_toolbar.setNavigationOnClickListener { finish() }
        ticket_toolbar.setOnMenuItemClickListener{ onMenuItemClicked(it) }
        comments.apply {
            adapter = this@TicketActivity.adapter
            addItemDecoration(
                    SpaceItemDecoration(resources.getInteger(R.integer.psd_comments_item_space)))
            this@TicketActivity.adapter.itemTouchHelper?.attachToRecyclerView(this)
        }
        send.setOnClickListener { sendComment() }
        attach.setOnClickListener { openFileChooser() }
    }

    private fun onMenuItemClicked(menuItem: MenuItem?): Boolean {
        return menuItem?.let {
            when (it.itemId) {
                R.id.psd_ticket_menu_close -> sharedViewModel.quitServiceDesk()
            }
            true
        } ?: false
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        observeData()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return menu?.let{
            MenuInflater(this).inflate(R.menu.activity_ticket, menu)
            menu.findItem(R.id.psd_ticket_menu_close).setShowAsAction(SHOW_AS_ACTION_ALWAYS)
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
        viewModel.getCommentsViewModel().observe(
                this,
                Observer {comments -> comments?.let { adapter.setItems(it) } }
        )
        sharedViewModel.getQuitServiceDeskLiveData().observe(
                this,
                Observer { quit -> quit?.let { if(it) finish() } }
        )
    }

    private fun sendComment() {
        if (!input.text.isNullOrEmpty()) {
            viewModel.addComment(input.text.toString())
            input.text = null
        }
    }

    private fun openFileChooser() {
        Intent(Intent.ACTION_GET_CONTENT).also{
            it.type = INTENT_IMAGE_TYPE
            it.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            startActivityForResult(it, REQUEST_PICK_IMAGE)
        }
    }
}
