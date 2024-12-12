package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket

import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.isVisible
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.injector
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketView.Model
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.TicketAdapter
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk._ref.utils.getColorOnBackground
import com.pyrus.pyrusservicedesk._ref.utils.getSecondaryColorOnBackground
import com.pyrus.pyrusservicedesk._ref.utils.setCursorColor
import com.pyrus.pyrusservicedesk._ref.utils.showKeyboardOn
import com.pyrus.pyrusservicedesk._ref.whitetea.android.TeaFragment
import com.pyrus.pyrusservicedesk._ref.whitetea.androidutils.bind
import com.pyrus.pyrusservicedesk._ref.whitetea.androidutils.getStore
import com.pyrus.pyrusservicedesk._ref.whitetea.bind.BinderLifecycleMode
import com.pyrus.pyrusservicedesk._ref.whitetea.core.ViewRenderer
import com.pyrus.pyrusservicedesk._ref.whitetea.utils.diff
import com.pyrus.pyrusservicedesk.databinding.PsdFragmentTicketBinding
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.item_decorators.SpaceItemDecoration
import kotlinx.coroutines.flow.map

internal class TicketFragment: TeaFragment<Model, TicketView.Event, TicketContract.Effect>() {

    private lateinit var binding: PsdFragmentTicketBinding

    private val adapter: TicketAdapter by lazy {
        TicketAdapter(
            { dispatch(TicketView.Event.OnRetryClick(0)) }, // TODO
            { dispatch(TicketView.Event.OnPreviewClick(it)) },
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

    override val renderer: ViewRenderer<Model> = diff {
        diff(Model::titleText) { title -> binding.toolbarTitle.text = title }
        diff(Model::inputText) { text -> if (!binding.input.hasFocus()) binding.input.setText(text) }
        diff(Model::sendEnabled) { sendEnabled -> binding.send.isEnabled = sendEnabled }
        diff(Model::comments, { new, old -> new === old }, adapter::submitList)
        diff(Model::showNoConnectionError) { showError -> binding.noConnection.root.isVisible = showError }
        diff(Model::isLoading) { isLoading ->
            binding.refresh.isVisible = !isLoading
            binding.progressBar.isVisible = isLoading
        }
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

        bindFeature()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_KEYBOARD_SHOWN, binding.input.hasFocus())
    }

    private fun bindFeature() {
        val feature = getStore { injector().ticketFeatureFactory("welcome").create() }
        bind {
            feature.states().map(TicketMapper::map) bindTo this@TicketFragment
        }
        bind(BinderLifecycleMode.START_STOP) {
            this@TicketFragment.messages.map(TicketMapper::map) bindTo feature
        }
    }

    private fun initListeners() {
        binding.ticketToolbar.setOnMenuItemClickListener { onMenuItemClicked(it) }
        binding.send.setOnClickListener { dispatch(TicketView.Event.OnSendClick) }
        binding.attach.setOnClickListener { dispatch(TicketView.Event.OnShowAttachVariantsClick) }
        binding.input.addTextChangedListener(inputTextWatcher)
    }

    private fun initUi() {
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