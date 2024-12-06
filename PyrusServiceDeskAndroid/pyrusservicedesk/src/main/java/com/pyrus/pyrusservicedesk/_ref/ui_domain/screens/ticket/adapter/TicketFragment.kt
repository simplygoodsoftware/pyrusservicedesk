package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter

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
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketView
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk._ref.utils.getColorOnBackground
import com.pyrus.pyrusservicedesk._ref.utils.getSecondaryColorOnBackground
import com.pyrus.pyrusservicedesk._ref.utils.setCursorColor
import com.pyrus.pyrusservicedesk._ref.utils.showKeyboardOn
import com.pyrus.pyrusservicedesk._ref.whitetea.android.TeaFragment
import com.pyrus.pyrusservicedesk.databinding.PsdFragmentTicketBinding
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.item_decorators.SpaceItemDecoration

internal class TicketFragment: TeaFragment<TicketView.TicketModel, TicketView.Event, TicketContract.Effect>() {

    private lateinit var binding: PsdFragmentTicketBinding

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = PsdFragmentTicketBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        initUi()
        initListeners()

        // TODO Model.titleText
        binding.toolbarTitle.text = ConfigUtils.getTitle(requireContext())

        // TODO Model.sendEnabled
        binding.send.isEnabled = !binding.input.text.isNullOrBlank()

        savedInstanceState?.let {
            if (it.getBoolean(STATE_KEYBOARD_SHOWN))
                showKeyboardOn(binding.input)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_KEYBOARD_SHOWN, binding.input.hasFocus())
    }

    override fun render(model: TicketView.TicketModel) {
        TODO("Not yet implemented")
    }

    private fun initListeners() {
        binding.ticketToolbar.setOnMenuItemClickListener { onMenuItemClicked(it) }
        binding.send.setOnClickListener { dispatch(TicketView.Event.OnSendClick) }
        binding.attach.setOnClickListener { dispatch(TicketView.Event.OnShowAttachVariantsClick) }
        binding.input.addTextChangedListener(inputTextWatcher)
    }

    private fun initUi() {
        // if you don't set empty text, Android will set the app name
        // TODO()
//        supportActionBar?.apply { title = "" }

        binding.comments.apply {
            adapter = this@TicketFragment.adapter
            addItemDecoration(
                SpaceItemDecoration(
                    resources.getDimensionPixelSize(R.dimen.psd_comments_item_space),
                    this@TicketFragment.adapter.itemSpaceMultiplier
                )
            )
            itemAnimator = null
        }

        applyStyle()
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

    companion object {
        private const val STATE_KEYBOARD_SHOWN = "STATE_KEYBOARD_SHOWN"

        fun newInstance(): TicketFragment {
            return TicketFragment()
        }
    }

}