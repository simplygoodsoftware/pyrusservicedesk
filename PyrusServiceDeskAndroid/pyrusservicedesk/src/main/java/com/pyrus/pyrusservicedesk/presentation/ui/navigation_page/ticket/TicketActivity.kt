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
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.WindowManager
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.ServiceDeskConfiguration
import com.pyrus.pyrusservicedesk.databinding.PsdActivityTicketBinding
import com.pyrus.pyrusservicedesk.presentation.ConnectionActivityBase
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.file_preview.FilePreviewActivity
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.dialogs.attach_files.AttachFileSharedViewModel
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.dialogs.attach_files.AttachFileVariantsFragment
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.dialogs.comment_actions.PendingCommentActionSharedViewModel
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.dialogs.comment_actions.PendingCommentActionsDialog
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.dialogs.rating.RatingBottomSheetDialogFragment
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.dialogs.rating.RatingBottomSheetDialogFragment.Companion.RATING_COMMENT_KEY
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries.RatingEntry
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries.TicketEntry
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries.Type
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.item_decorators.SpaceItemDecoration
import com.pyrus.pyrusservicedesk.sdk.data.Attachment
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileData
import com.pyrus.pyrusservicedesk.utils.ConfigUtils
import com.pyrus.pyrusservicedesk.utils.RequestUtils.Companion.getFileUrl
import com.pyrus.pyrusservicedesk.utils.getColorOnBackground
import com.pyrus.pyrusservicedesk.utils.getSecondaryColorOnBackground
import com.pyrus.pyrusservicedesk.utils.getViewModel
import com.pyrus.pyrusservicedesk.utils.isAtEnd
import com.pyrus.pyrusservicedesk.utils.setCursorColor
import com.pyrus.pyrusservicedesk.utils.setupWindowInsets
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Activity for rendering ticket/feed comments.
 */
internal class TicketActivity : ConnectionActivityBase<TicketViewModel>(TicketViewModel::class.java) {

    private lateinit var binding: PsdActivityTicketBinding

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

    private var recentContentHeight = 0
    private val globalLayoutListener by lazy {
        OnGlobalLayoutListener {
            val changedHeight = recentContentHeight - binding.comments.height
            if (changedHeight != 0)
                onViewHeightChanged(changedHeight)
            recentContentHeight = binding.comments.height
        }
    }

    private val attachFileSharedViewModel: AttachFileSharedViewModel by getViewModel(
        AttachFileSharedViewModel::class.java)
    private val commentActionsSharedViewModel: PendingCommentActionSharedViewModel by getViewModel(
        PendingCommentActionSharedViewModel::class.java)

    private val ratingAdapter: RatingAdapter = RatingAdapter { rating ->
        onRatingClickListener(rating)
    }

    private val adapter = TicketAdapter().apply {
        setOnFileReadyForPreviewClickListener { attachment ->
            val fileData = attachment.toFileData()
            if (fileData.isLocal) {
                return@setOnFileReadyForPreviewClickListener
            }
            startActivity(FilePreviewActivity.getLaunchIntent(fileData))
        }
        setOnErrorCommentEntryClickListener {
            viewModel.onUserStartChoosingCommentAction(it)
            PendingCommentActionsDialog().show(supportFragmentManager, "")
        }
        setOnTextCommentLongClicked {
            copyToClipboard(it)
        }
        setOnRatingClickListener { rating ->
            onRatingClickListener(rating)
        }
    }

    private fun onRatingClickListener(rating: Int) {
        val bottomSheet = RatingBottomSheetDialogFragment.newInstance(viewModel.getRateUsText())
        bottomSheet.show(supportFragmentManager, bottomSheet.tag)
        viewModel.onRatingClick(rating)
    }

    private val inputTextWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            binding.send.isEnabled = !s.isNullOrBlank()
            viewModel.onInputTextChanged(s.toString())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = PsdActivityTicketBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupWindowInsets(binding.root)

        savedInstanceState?.let { ServiceDeskConfiguration.restore(it) }
        setNoConnectionView(binding.noConnection)
        setSupportActionBar(binding.ticketToolbar)
        setRefresher(binding.refresh)

        supportFragmentManager.setFragmentResultListener(RATING_COMMENT_KEY, this) { _, bundle ->
            val result = bundle.getString(RATING_COMMENT_KEY)
            viewModel.onRatingCommentSendClick(result)
        }

        val accentColor = ConfigUtils.getAccentColor(this)

        // if you don't set empty text, Android will set the app name
        supportActionBar?.apply { title = "" }
        binding.toolbarTitle.text = ConfigUtils.getTitle(this@TicketActivity)

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

        val secondaryColor = getSecondaryColorOnBackground(ConfigUtils.getNoPreviewBackgroundColor(this))
        binding.noConnection.noConnectionImageView.setColorFilter(secondaryColor)
        binding.noConnection.noConnectionTextView.setTextColor(secondaryColor)
        binding.noConnection.reconnectButton.setTextColor(ConfigUtils.getAccentColor(this))
        binding.noConnection.root.setBackgroundColor(ConfigUtils.getNoConnectionBackgroundColor(this))

        ConfigUtils.getMainBoldFontTypeface()?.let {
            binding.toolbarTitle.typeface = it
        }

        binding.ticketToolbar.setOnMenuItemClickListener{ onMenuItemClicked(it) }
        binding.comments.apply {
            adapter = this@TicketActivity.adapter
            addItemDecoration(
                SpaceItemDecoration(
                    resources.getDimensionPixelSize(R.dimen.psd_comments_item_space),
                    this@TicketActivity.adapter.itemSpaceMultiplier)
            )
            itemAnimator = null
        }

        binding.rating.ratingTextRv.apply {
            adapter = this@TicketActivity.ratingAdapter
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(
                SpaceItemDecoration(
                    resources.getDimensionPixelSize(R.dimen.psd_comments_item_space_double)
                )
            )
        }

        binding.send.setOnClickListener { onSendCommentClick() }
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
        binding.attach.setOnClickListener { showAttachFileVariants() }
        binding.attach.setColorFilter(ConfigUtils.getFileMenuButtonColor(this))
        if(savedInstanceState == null) {
            binding.input.setText(viewModel.draft)
            showKeyboardOn(binding.input){
                binding.input.setSelection(binding.input.length())
            }
        }
        binding.input.apply {
            highlightColor = accentColor
            setCursorColor(accentColor)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                textCursorDrawable = null
            }
            setHintTextColor(ConfigUtils.getSecondaryColorOnMainBackground(this@TicketActivity))
            setTextColor(ConfigUtils.getInputTextColor(this@TicketActivity))
            addTextChangedListener(inputTextWatcher)
        }
        binding.send.isEnabled = !binding.input.text.isNullOrBlank()
        binding.divider.setBackgroundColor(getColorOnBackground(ConfigUtils.getMainBackgroundColor(this), 30))
        binding.refresh.setProgressBackgroundColor(ConfigUtils.getMainBackgroundColor(this))
        binding.refresh.setColorSchemeColors(ConfigUtils.getAccentColor(this))

        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ConfigUtils.getStatusBarColor(this)?: window.statusBarColor
    }

    override fun onStart() {
        super.onStart()
        binding.comments.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
        viewModel.onStart()
    }

    override fun onStop() {
        super.onStop()
        if(binding.comments.viewTreeObserver.isAlive) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                binding.comments.viewTreeObserver.removeOnGlobalLayoutListener(globalLayoutListener)
            else binding.comments.viewTreeObserver.removeGlobalOnLayoutListener(globalLayoutListener)
        }
        viewModel.onStop()
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
            closeItem.setShowAsAction(SHOW_AS_ACTION_ALWAYS)
            closeItem.icon?.setColorFilter(ConfigUtils.getToolbarButtonColor(this), PorterDuff.Mode.SRC_ATOP)
            true
        } ?: false
    }

    private fun setRatingUi(ratingEntry: TicketEntry) {
        if (ratingEntry !is RatingEntry)
            return
        viewModel.setRateUsText(ratingEntry.ratingText)
        binding.rating.rateUsText.text = ratingEntry.ratingText
        binding.rating.ratingTextRv.isVisible = ratingEntry.ratingSettings?.type == 3 //TODO kate
        binding.rating.smileLl5.isVisible = ratingEntry.ratingSettings?.type == 1 && ratingEntry.ratingSettings.size == 5
        binding.rating.smileLl.isVisible = getSmileLlVisibility(ratingEntry)
        binding.rating.likeLl.isVisible = ratingEntry.ratingSettings?.type == 2

        if (getSmileLlVisibility(ratingEntry)) {
            binding.rating.rating2Mini.isVisible = ratingEntry.ratingSettings?.size == 3
        }

        setRatingClickListeners()

        if (ratingEntry.ratingSettings?.type == 3) {
            ratingAdapter.submitList(ratingEntry.ratingSettings.ratingTextValues)
        }
    }

    private fun setRatingClickListeners() {
        binding.rating.rating1.setOnClickListener { onRatingClickListener(1) }
        binding.rating.rating2.setOnClickListener { onRatingClickListener(2) }
        binding.rating.rating3.setOnClickListener { onRatingClickListener(3) }
        binding.rating.rating4.setOnClickListener { onRatingClickListener(4) }
        binding.rating.rating5.setOnClickListener { onRatingClickListener(5) }

        binding.rating.rating1Mini.setOnClickListener { onRatingClickListener(1) }
        binding.rating.rating2Mini.setOnClickListener { onRatingClickListener(3) }
        binding.rating.rating3Mini.setOnClickListener { onRatingClickListener(5) }

        binding.rating.like1.setOnClickListener { onRatingClickListener(1) }
        binding.rating.like2.setOnClickListener { onRatingClickListener(5) }
    }

    private fun getSmileLlVisibility(ratingEntry: RatingEntry): Boolean {
        return ratingEntry.ratingSettings?.type == 1
            && ratingEntry.ratingSettings.size != null
            && ratingEntry.ratingSettings.size < 5
    }

    override fun startObserveData() {
        super.startObserveData()
        viewModel.getCommentDiffLiveData().observe(this) { result ->
            val atEnd = binding.comments.isAtEnd()
            val isEmpty = binding.comments.adapter?.itemCount == 0
            result?.let {
                binding.refresh.isRefreshing = false
                adapter.setItems(it.newItems)
                it.diffResult.dispatchUpdatesTo(adapter)
            }

            val rating = result.newItems.find { it.type == Type.Rating }
            binding.rating.root.isVisible = rating != null
            if (rating != null) {
                setRatingUi(rating)
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
        viewModel.onSendClicked(binding.input.text.toString())
        binding.input.text = null
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
    return FileData(
        name,
        bytesSize,
        if (isLocal() && localUri != null) localUri else Uri.parse(getFileUrl(id, PyrusServiceDesk.get().domain)),
        isLocal()
    )
}
