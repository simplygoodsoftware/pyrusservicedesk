package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket

import android.Manifest.permission.RECORD_AUDIO
import android.content.ClipData
import android.content.ClipboardManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.content.res.ColorStateList
import android.graphics.PorterDuff
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
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.injector
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.SdScreens
import com.pyrus.pyrusservicedesk._ref.data.AudioData
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketView.Effect
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketView.Event
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketView.Model
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.entries.CommentEntry
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.fingerprints.AudioStatus
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.fingerprints.ButtonsFingerprint
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.fingerprints.CommentAttachmentFingerprint
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.fingerprints.CommentAudioFingerprint
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.fingerprints.CommentPreviewableAttachmentFingerprint
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.fingerprints.CommentTextFingerprint
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.fingerprints.DateFingerprint
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.fingerprints.RatingCommentFingerprint
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.fingerprints.RatingTextFingerprint
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.fingerprints.SimpleTextFingerprint
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.record.AudioRecordView
import com.pyrus.pyrusservicedesk._ref.utils.AudioWrapper
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils.Companion.getMainBackgroundColor
import com.pyrus.pyrusservicedesk._ref.utils.TextProvider
import com.pyrus.pyrusservicedesk._ref.utils.animateVisibility
import com.pyrus.pyrusservicedesk._ref.utils.getColorOnBackground
import com.pyrus.pyrusservicedesk._ref.utils.getSecondaryColorOnBackground
import com.pyrus.pyrusservicedesk._ref.utils.getTimeString
import com.pyrus.pyrusservicedesk._ref.utils.insets.RootViewDeferringInsetsCallback
import com.pyrus.pyrusservicedesk._ref.utils.setCursorColor
import com.pyrus.pyrusservicedesk._ref.utils.showKeyboardOn
import com.pyrus.pyrusservicedesk._ref.utils.text
import com.pyrus.pyrusservicedesk._ref.whitetea.android.TeaFragment
import com.pyrus.pyrusservicedesk._ref.whitetea.androidutils.bind
import com.pyrus.pyrusservicedesk._ref.whitetea.androidutils.getStore
import com.pyrus.pyrusservicedesk._ref.whitetea.bind.BinderLifecycleMode
import com.pyrus.pyrusservicedesk._ref.whitetea.core.ViewRenderer
import com.pyrus.pyrusservicedesk._ref.whitetea.utils.diff
import com.pyrus.pyrusservicedesk.databinding.PsdFragmentTicketBinding
import com.pyrus.pyrusservicedesk.payload_adapter.PayloadListAdapter
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.dialogs.attach_files.AttachFileVariantsFragment
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.dialogs.comment_actions.ErrorCommentActionsDialog
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.dialogs.rating.RatingBottomSheetDialogFragment
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.dialogs.rating.RatingBottomSheetDialogFragment.Companion.RATING_COMMENT_KEY
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.item_decorators.CommentVerticalItemDecoration
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.item_decorators.GroupVerticalItemDecoration
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.item_decorators.SpaceItemDecoration
import com.pyrus.pyrusservicedesk.sdk.repositories.UserInternal
import com.pyrus.pyrusservicedesk.utils.UiUtils.hideKeyboard
import com.pyrus.pyrusservicedesk.utils.hapticFeedback
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal class TicketFragment: TeaFragment<Model, Event, Effect>() {

    private lateinit var binding: PsdFragmentTicketBinding
    private lateinit var audioRecordView: AudioRecordView

    private var actionButtonIsSend = false

    private val adapter: PayloadListAdapter<CommentEntry> by lazy { PayloadListAdapter(
        ButtonsFingerprint(::dispatch),
        CommentTextFingerprint(::dispatch),
        CommentAttachmentFingerprint(::dispatch, viewLifecycleOwner),
        CommentPreviewableAttachmentFingerprint(::dispatch, viewLifecycleOwner),
        DateFingerprint(),
        RatingCommentFingerprint(::dispatch),
        SimpleTextFingerprint(),
        CommentAudioFingerprint(audioWrapper, lifecycleScope, ::dispatch),
    ) }

    private val ratingAdapter: PayloadListAdapter<CommentEntry.RatingTextValues> by lazy { PayloadListAdapter(
        RatingTextFingerprint(::dispatch)
    ) }

    private val inputTextWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            dispatch(Event.OnMessageChanged(s.toString()))
        }
    }

    private var pendingAudioJob: Job? = null
    private var pendingAudioUri: String? = null
    private var userIsSeeking = false

    private val requestAudioPermissionLauncher = registerForActivityResult(RequestPermission()) { }

    private lateinit var audioWrapper: AudioWrapper

    override fun createRenderer(): ViewRenderer<Model> = diff {
        diff(Model::inputText) { text -> if (!binding.inputEditText.hasFocus()) binding.inputEditText.setText(text) }
        diff(Model::sendEnabled) { sendEnabled -> binding.sendButton.isEnabled = sendEnabled }
        diff(Model::comments, { new, old -> new === old }, ::updateComments)
        diff(Model::showNoConnectionError) { showError -> binding.noConnection.root.isVisible = showError }
        diff(Model::isLoading) { isLoading ->
            binding.ticketContent.isVisible = !isLoading
            binding.progressBar.isVisible = isLoading
        }
        diff(Model::toolbarTitleText) { text -> binding.toolbarTitle.text = ConfigUtils.getTitle(requireContext(), text?.text(requireContext())) }
        diff(Model::isRefreshing) { isRefreshing -> binding.refresh.isRefreshing = isRefreshing }
        diff(Model::showInputPanel) { showInput ->
            binding.inputLayout.isVisible = showInput
            binding.divider.isVisible = showInput

            binding.refresh.setBottomSpinnerGap(
                if (showInput) 0f
                else resources.getDimension(R.dimen.psd_blur_height)
            )

            val bottomPadding =
                when {
                    showInput -> resources.getDimension(R.dimen.psd_offset_small)
                    else -> resources.getDimension(R.dimen.psd_blur_height) + resources.getDimension(R.dimen.psd_offset_small)
                }

            binding.comments.apply {
                setPadding(paddingLeft, paddingTop, paddingRight, bottomPadding.toInt())
            }

            if (!showInput) hideKeyboard(requireView())
        }
        diff(Model::wavesIsVisible) { wavesIsVisible ->
            audioRecordView.setRecordingIndicationVisibility(wavesIsVisible)
        }
        diff(Model::canDragRecordMic) { canDragRecordMic ->
            audioRecordView.canInteract(canDragRecordMic)
        }
        diff(Model::recordState) { recordState ->
            audioRecordView.applyRecordState(recordState)
        }
        diff(Model::scrollDownIsVisible) { scrollIsVisible ->
            binding.scrollDownButtonLayout.animateVisibility(
                isVisible = scrollIsVisible,
                fadeInStartDelay = 200L
            )
        }
        diff(Model::actionButtonIsSend) { actionButtonIsSend ->
            this@TicketFragment.actionButtonIsSend = actionButtonIsSend
            binding.sendButton.isVisible = actionButtonIsSend
            binding.recordButton.isVisible = !actionButtonIsSend

            val actionIconRes = when {
                actionButtonIsSend -> R.drawable.psd_ic_send_filled
                else -> R.drawable.psd_ic_mic_filled
            }
            binding.actionCircleIcon.setImageResource(actionIconRes)
        }
        diff(Model::pendingAudio) { pendingAudio ->
            pendingAudioJob?.cancel()
            pendingAudioUri = pendingAudio
            if (pendingAudio != null) {
                Log.d("DFD", "pending audio: $pendingAudio")
                pendingAudioJob = lifecycleScope.launch {
                    audioWrapper.getAudioDataFlow(pendingAudio).collect(::applyAudioData)
                }
            }
        }
        diff(Model::ratingTextRvVisibility) { ratingTextRvVisibility ->
            binding.rating.ratingTextRv.isVisible = ratingTextRvVisibility
        }

        diff(Model::smileLl5Visibility) { smileLl5Visibility ->
            binding.rating.smileLl5.isVisible = smileLl5Visibility
        }

        diff(Model::smileLlVisibility) { smileLlVisibility ->
            binding.rating.smileLl.isVisible = smileLlVisibility
        }

        diff(Model::likeLlVisibility) { likeLlVisibility ->
            binding.rating.likeLl.isVisible = likeLlVisibility
        }

        diff(Model::ratingText) { ratingText ->
            binding.rating.rateUsText.text = ratingText
        }

        diff(Model::rating2MiniVisibility) { rating2MiniVisibility ->
            binding.rating.rating2Mini.isVisible = rating2MiniVisibility
        }

        diff(Model::ratingTextValues) { ratingTextValues ->
            val list = ratingTextValues?.sortedByDescending  { it.rating }
            list?.let { ratingAdapter.submitList(it) }
        }

        diff(Model::showRating) { showRating ->
            binding.rating.root.isVisible = showRating
            binding.gradient.isVisible = showRating
            val params = binding.refresh.layoutParams as ViewGroup.MarginLayoutParams
            params.bottomMargin = if (showRating) -resources.getDimension(R.dimen.psd_offset_default).toInt() else 0
            binding.refresh.layoutParams = params
        }
    }

    override fun handleEffect(effect: Effect) = when(effect) {
        is Effect.PlayAudio -> {}

        is Effect.CopyToClipboard -> {
            val clipboard = getSystemService(requireContext(), ClipboardManager::class.java) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText(TextProvider.Res(R.string.copied_text).text(requireContext()), effect.text))
        }

        is Effect.MakeToast -> {
            Toast.makeText(
                requireContext(),
                effect.text.text(requireContext()),
                Toast.LENGTH_SHORT
            ).show()
        }

        is Effect.ShowAttachVariants -> {
            injector().router.setResultListener(effect.key) {
                dispatch(Event.SetAttachVariant(effect.key, it))
            }
            AttachFileVariantsFragment.newInstance(effect.key).show(parentFragmentManager, null)
        }

        is Effect.ShowErrorCommentDialog -> {
            ErrorCommentActionsDialog
                .newInstance(effect.key)
                .show(parentFragmentManager, "")
        }

        is Effect.ShowInfoBottomSheetFragment -> {}
        is Effect.UpdateRecordWave -> audioRecordView.updateFrequency(effect.recordedSegmentValues)
        is Effect.ShowAudioRecordTooltip -> audioRecordView.showAudioRecordTooltip()
        is Effect.Exit -> injector().router.exit()
        is Effect.OpenPreview -> injector().router.navigateTo(SdScreens.ImageScreen(effect.fileData))
        is Effect.OpenRatingComment -> {
            val bottomSheet = RatingBottomSheetDialogFragment.newInstance(effect.rateUsText)
            bottomSheet.show(parentFragmentManager, bottomSheet.tag)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindFeature()
        parentFragmentManager.setFragmentResultListener(RATING_COMMENT_KEY, this) { _, bundle ->
            val result = bundle.getString(RATING_COMMENT_KEY)
            dispatch(Event.OnRatingClick(null, result))
        }
        audioWrapper = injector().audioWrapper
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = PsdFragmentTicketBinding.inflate(inflater, container, false)

        audioRecordView = AudioRecordView(
            recordWaves = binding.recordWaves,
            recordButton = binding.recordButton,
            sendButton = binding.sendButton,
            onEvent = ::dispatch,
            requestPermission = ::requestAudioPermission,
            parentView = binding.inputLayout,
            actionCircle = binding.actionCircle,
            actionCircleHolder = binding.actionCircleHolder,
            lockStopView = binding.lockStopView,
            lockStopLayout = binding.lockStopLayout,
            cancelRecordHint = binding.cancelRecordHint,
            cancelHoldingRecordButton = binding.cancelHoldingRecordButton,
            removePendingAudioButton = binding.removePendingAudioButton,
            pendingPlayerLayout = binding.pendingPlayerLayout,
            attachButton = binding.attachButton,
            inputEditText = binding.inputEditText,
            timerView = binding.timerView,
            recordDot = binding.recordDot,
        )

        initUi()
        initListeners()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState == null && requireArguments().getLong(KEY_TICKET_ID, 0) < 0) {
            showKeyboardOn(binding.inputEditText)
        }

        if (savedInstanceState?.getBoolean(STATE_KEYBOARD_SHOWN) == true) {
            showKeyboardOn(binding.inputEditText)
        }

        val rootInsetsListener = RootViewDeferringInsetsCallback(
            persistentInsetTypes = WindowInsetsCompat.Type.captionBar() or WindowInsetsCompat.Type.statusBars(),
            deferredInsetTypes = WindowInsetsCompat.Type.ime(),
        )
        ViewCompat.setOnApplyWindowInsetsListener(binding.contentRoot, rootInsetsListener)

        val insetListener = object : OnApplyWindowInsetsListener {
            override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
                val typeInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
                val hasImeInsets = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom > 0
                if (hasImeInsets) v.setPadding(0,0,0,0)
                else v.setPadding(typeInsets.left, typeInsets.top, typeInsets.right, typeInsets.bottom)

                return WindowInsetsCompat.CONSUMED
            }
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.inputLayout, insetListener)
        ViewCompat.setOnApplyWindowInsetsListener(binding.actionCircleHolder, insetListener)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_KEYBOARD_SHOWN, binding.inputEditText.hasFocus())
    }

    private fun bindFeature() {
        val user = arguments?.getParcelable<UserInternal>(KEY_USER_INTERNAL)!!
        val ticketId = arguments?.getLong(KEY_TICKET_ID)!!
        val sendComment = arguments?.getString(KEY_SEND_COMMENT)

        val feature = getStore { injector().ticketFeatureFactory.create(
            user = user,
            initialTicketId = ticketId,
            welcomeMessage = ConfigUtils.getWelcomeMessage(),
            sendComment = sendComment,
        ) }
        bind(BinderLifecycleMode.CREATE_DESTROY) {
            this@TicketFragment.messages.map(TicketMapper::map) bindTo feature
        }
        bind {
            feature.state.map(TicketMapper::map) bindTo this@TicketFragment
            feature.effects.map(TicketMapper::map) bindTo this@TicketFragment
        }

    }

    private fun initListeners() {
        binding.ticketToolbar.setOnMenuItemClickListener { onMenuItemClicked(it) }
        binding.sendButton.setOnClickListener {
            dispatch(Event.OnSendClick)
            binding.inputEditText.text = null
            binding.comments.scrollToPosition(0)
        }
        binding.attachButton.setOnClickListener {
            binding.attachButton.hapticFeedback()
            dispatch(Event.OnShowAttachVariantsClick)
        }
        binding.inputEditText.addTextChangedListener(inputTextWatcher)
        binding.refresh.setOnRefreshListener { dispatch(Event.OnRefresh) }
        binding.toolbarBack.setOnClickListener { dispatch(Event.OnCloseClick) }

        binding.comments.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                if (newState == RecyclerView.SCROLL_STATE_IDLE && layoutManager.findFirstVisibleItemPosition() == 0)
                    binding.scrollDownButton.hide()
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy == 0) return
                binding.scrollDownButton.show()
            }

        })

        binding.scrollDownButton.setOnClickListener {
            binding.comments.scrollToPosition(0)
            binding.scrollDownButton.hide()
        }

        binding.lockStopLayout.setOnClickListener {
            binding.lockStopLayout.hapticFeedback()
            dispatch(Event.OnStopRecord)
        }

        binding.playerProgressBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {}

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                userIsSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                userIsSeeking = false
                val url = pendingAudioUri
                if (url != null) {
                    audioWrapper.waitForSeek(binding.playerProgressBar.progress.toLong(), url)
                }
            }

        })

        binding.playButton.setOnClickListener {
            Log.d("DFD", "playAudio: $pendingAudioUri")
            pendingAudioUri?.let(audioWrapper::playAudio)
        }

        binding.removePendingAudioButton.setOnClickListener {
            binding.removePendingAudioButton.hapticFeedback()
            dispatch(Event.OnRemovePendingAudioClick)
        }

        binding.cancelHoldingRecordButton.setOnClickListener {
            binding.cancelHoldingRecordButton.hapticFeedback()
            dispatch(Event.OnCancelRecord)
        }

        binding.actionCircle.setOnClickListener {
            if (actionButtonIsSend) {
                binding.actionCircle.hapticFeedback()
                dispatch(Event.OnStopEndSendRecord)
            }
        }

        binding.rating.rating1.setOnClickListener { dispatch(Event.OnRatingClick(1, null)) }
        binding.rating.rating2.setOnClickListener { dispatch(Event.OnRatingClick(2, null)) }
        binding.rating.rating3.setOnClickListener { dispatch(Event.OnRatingClick(3, null)) }
        binding.rating.rating4.setOnClickListener { dispatch(Event.OnRatingClick(4, null)) }
        binding.rating.rating5.setOnClickListener { dispatch(Event.OnRatingClick(5, null)) }

        binding.rating.rating1Mini.setOnClickListener { dispatch(Event.OnRatingClick(1, null)) }
        binding.rating.rating2Mini.setOnClickListener { dispatch(Event.OnRatingClick(3, null)) }
        binding.rating.rating3Mini.setOnClickListener { dispatch(Event.OnRatingClick(5, null)) }

        binding.rating.like1.setOnClickListener { dispatch(Event.OnRatingClick(1, null)) }
        binding.rating.like2.setOnClickListener { dispatch(Event.OnRatingClick(5, null)) }

    }

    private fun requestAudioPermission(): Boolean {

        val context = context ?: return false
        val activity = activity ?: return false

        return when {
            ContextCompat.checkSelfPermission(context, RECORD_AUDIO) == PERMISSION_GRANTED -> {
                Log.d("DFD", "Permission: GRANTED")
                true
            }
            ActivityCompat.shouldShowRequestPermissionRationale(activity, RECORD_AUDIO) -> {
                Log.d("DFD", "Permission: show rationale")
                // TODO DFD show rationale
                false
            }
            else -> {
                Log.d("DFD", "Permission: request")
                requestAudioPermissionLauncher.launch(RECORD_AUDIO)
                false
            }
        }
    }

    private fun initUi() {
        initCommentsRecyclerView()

        binding.rating.ratingTextRv.adapter = ratingAdapter
        binding.rating.ratingTextRv.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            true
        )
        binding.rating.ratingTextRv.addItemDecoration(
            SpaceItemDecoration(
                resources.getDimensionPixelSize(R.dimen.psd_comments_item_space_double)
            )
        )

        binding.progressBar.indeterminateDrawable.setColorFilter(
            ConfigUtils.getAccentColor(requireContext()),
            PorterDuff.Mode.SRC_IN
        )
        binding.toolbarTitle.text = ConfigUtils.getTitle(requireContext())

        applyStyle()
    }

    override fun onStop() {
        audioWrapper.stop()
        super.onStop()
    }

    private fun initCommentsRecyclerView() {
        val recyclerView = binding.comments

        recyclerView.adapter = adapter
        recyclerView.itemAnimator = null

        recyclerView.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            true
        )

        val defaultDivider = resources.getDimensionPixelSize(R.dimen.psd_comments_item_space)
        recyclerView.addItemDecoration(
            CommentVerticalItemDecoration(
                innerDivider = defaultDivider,
                outerDivider = defaultDivider * 4,
                invert = true,
            )  { current, next ->
                when {
                    (current as? CommentEntry.Comment)?.isInbound == (next as? CommentEntry.Comment)?.isInbound -> false
                    else -> true
                }
            }
        )
        recyclerView.addItemDecoration(
            GroupVerticalItemDecoration(
                viewType = GroupVerticalItemDecoration.TYPE_ANY,
                innerDivider = defaultDivider,
                outerDivider = defaultDivider,
                invert = true,
                excludeTypes = setOf(R.layout.psd_view_holder_comment_text, R.layout.psd_view_holder_comment_attachment, R.layout.psd_view_holder_comment_previewable_attachment)
            )
        )

        val pool = RecyclerView.RecycledViewPool()
        pool.setMaxRecycledViews(R.layout.psd_view_holder_comment_text, 10)
        pool.setMaxRecycledViews(R.layout.psd_view_holder_comment_attachment, 10)
        pool.setMaxRecycledViews(R.layout.psd_view_holder_comment_previewable_attachment, 10)
        recyclerView.setRecycledViewPool(pool)
    }

    private fun applyStyle() {
        val accentColor = ConfigUtils.getAccentColor(requireContext())
        binding.refresh.setBackgroundColor(getMainBackgroundColor(requireContext()))

        binding.contentRoot.setBackgroundColor(ConfigUtils.getMainBackgroundColor(requireContext()))
        binding.scrollDownButton.imageTintList = ColorStateList.valueOf(accentColor)
        binding.inputLayout.setBackgroundColor(ConfigUtils.getMainBackgroundColor(requireContext()))

        binding.cancelRecordHint.setTextColor(ConfigUtils.getSecondaryColorOnMainBackground(requireContext()))
        binding.cancelHoldingRecordButton.setTextColor(accentColor)

        val toolbarColor = ConfigUtils.getHeaderBackgroundColor(requireContext())
        binding.toolbarTitle.setTextColor(ConfigUtils.getChatTitleTextColor(requireContext()))
        binding.ticketToolbar.setBackgroundColor(toolbarColor)

        ConfigUtils.getMainFontTypeface()?.let {
            binding.inputEditText.typeface = it
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
        binding.sendButton.imageTintList = stateList

        binding.toolbarBack.setColorFilter(ConfigUtils.getAccentColor(requireContext()))

        binding.inputEditText.highlightColor = accentColor
        binding.inputEditText.setCursorColor(accentColor)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            binding.inputEditText.textCursorDrawable = null
        }
        binding.inputEditText.setHintTextColor(resources.getColor(R.color.psd_hint_color))
        binding.inputEditText.setTextColor(ConfigUtils.getInputTextColor(requireContext()))

        binding.view.setBackgroundColor(
            getColorOnBackground(
                ConfigUtils.getMainBackgroundColor(requireContext()),
                30
            )
        )
        binding.divider.setBackgroundColor(
            getColorOnBackground(
                ConfigUtils.getMainBackgroundColor(requireContext()),
                30
            )
        )

        binding.refresh.setProgressBackgroundColor(resources.getColor(R.color.psd_color_fab_scroll_down))
        binding.refresh.setColorSchemeColors(ConfigUtils.getAccentColor(requireContext()))

        with(requireActivity().window) {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = ConfigUtils.getStatusBarColor(requireContext()) ?: statusBarColor
        }

        binding.playButton.backgroundTintList = ColorStateList.valueOf(ConfigUtils.getAccentColor(requireContext()))
        binding.playerProgressBar.progressTintList = ColorStateList.valueOf(accentColor)
        binding.playerProgressBar.thumbTintList = ColorStateList.valueOf(accentColor)
        binding.playerProgressBar.progressBackgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.psd_color_black_01))
        binding.trackCurrentTime.setTextColor(ConfigUtils.getSecondaryColorOnMainBackground(requireContext()))

        val ratingBackgroundColor = ColorStateList.valueOf(ConfigUtils.getSupportMessageTextBackgroundColor(binding.root.context))
        binding.rating.rating1.backgroundTintList = ratingBackgroundColor
        binding.rating.rating2.backgroundTintList = ratingBackgroundColor
        binding.rating.rating3.backgroundTintList = ratingBackgroundColor
        binding.rating.rating4.backgroundTintList = ratingBackgroundColor
        binding.rating.rating5.backgroundTintList = ratingBackgroundColor

        binding.rating.rating1Mini.backgroundTintList = ratingBackgroundColor
        binding.rating.rating2Mini.backgroundTintList = ratingBackgroundColor
        binding.rating.rating3Mini.backgroundTintList = ratingBackgroundColor

        binding.rating.like1.backgroundTintList = ratingBackgroundColor
        binding.rating.like2.backgroundTintList = ratingBackgroundColor
    }

    private fun onMenuItemClicked(menuItem: MenuItem?): Boolean {
        menuItem ?: return false
        if (menuItem.itemId == R.id.psd_main_menu_close) {
            dispatch(Event.OnCloseClick)
        }
        return true
    }

    private fun updateComments(comments: List<CommentEntry>?) {
        val adapterIsEmpty = adapter.itemCount == 0
        val commentsToSubmit = comments?.reversed()
        adapter.submitList(commentsToSubmit) {
            val layoutManager = (binding.comments.layoutManager as? LinearLayoutManager) ?: return@submitList
            val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
            if (firstVisiblePosition == 0) {
                layoutManager.scrollToPosition(0)
            }
            val commentId = arguments?.getLong(KEY_COMMENT_ID)
            if (commentId != null && adapterIsEmpty) {
                val scrollPosition = commentsToSubmit?.indexOfFirst {
                    it is CommentEntry.Comment && it.id == commentId
                }
                if (scrollPosition != null && scrollPosition != -1) {
                    scrollToPosition(scrollPosition)
                }
            }
        }
    }

    private fun scrollToPosition(scrollPosition: Int) {
        val layoutManager = binding.comments.layoutManager as? LinearLayoutManager ?: return
        layoutManager.scrollToPosition(scrollPosition)
        binding.comments.post {
            val targetView = layoutManager.findViewByPosition(scrollPosition) ?: return@post

            val itemTop = targetView.top
            val scrollY = binding.comments.scrollY
            val padding = binding.comments.resources.getDimension(R.dimen.dp_4).toInt()
            val additionalPadding = padding * padding * 2
            val offset = itemTop + scrollY - (padding + additionalPadding)

            binding.comments.scrollBy(0, offset)
        }
    }

    private fun applyAudioData(data: AudioData) {
        updateSeekBar(data.position)
        binding.playerProgressBar.max = data.audioFullTime?.toInt() ?: 0
        updateAudioStatus(data.status)
        binding.trackCurrentTime.text = getTimeString(binding.root.context, data.audioCurrentTime?.toDouble() ?: 0.0)
    }

    private fun updateSeekBar(position: Int) {
        if (userIsSeeking) return
        binding.playerProgressBar.progress = position
    }

    private fun updateAudioStatus(status: AudioStatus) {
        when (status) {
            AudioStatus.None,
            AudioStatus.Error,
            AudioStatus.Processing,
            AudioStatus.Paused,
                -> {
                binding.playButton.setImageResource(R.drawable.psd_ic_play_16)
            }
            AudioStatus.Playing -> {
                binding.playButton.setImageResource(R.drawable.ic_audio_pause_16)
            }
        }
    }

    companion object {
        private const val STATE_KEYBOARD_SHOWN = "STATE_KEYBOARD_SHOWN"
        private const val KEY_TICKET_ID = "KEY_TICKET_ID"
        private const val KEY_COMMENT_ID = "KEY_COMMENT_ID"
        private const val KEY_USER_INTERNAL = "KEY_USER_INTERNAL"
        private const val KEY_SEND_COMMENT = "KEY_SEND_COMMENT"

        fun newInstance(
            ticketId: Long,
            commentId: Long?,
            user: UserInternal,
            sendComment: String?,
        ): TicketFragment {
            val fragment = TicketFragment()
            val args = Bundle().apply {
                putParcelable(KEY_USER_INTERNAL, user)
                putLong(KEY_TICKET_ID, ticketId)
                commentId?.let { putLong(KEY_COMMENT_ID, commentId) }
                putString(KEY_SEND_COMMENT, sendComment)
            }
            fragment.arguments = args
            return fragment
        }
    }

}