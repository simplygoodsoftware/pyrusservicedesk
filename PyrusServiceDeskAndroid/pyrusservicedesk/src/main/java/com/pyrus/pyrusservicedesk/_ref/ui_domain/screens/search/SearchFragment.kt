package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.search

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.pyrus.pyrusservicedesk.NoFullScreenFragment
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.injector
import com.pyrus.pyrusservicedesk._ref.SdScreens
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.search.SearchView.Effect
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.search.SearchView.Event
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.search.SearchView.Model
import com.pyrus.pyrusservicedesk._ref.utils.insets.RootViewDeferringInsetsCallback
import com.pyrus.pyrusservicedesk._ref.utils.showKeyboardOn
import com.pyrus.pyrusservicedesk._ref.utils.text
import com.pyrus.pyrusservicedesk._ref.whitetea.android.TeaFragment
import com.pyrus.pyrusservicedesk._ref.whitetea.androidutils.bind
import com.pyrus.pyrusservicedesk._ref.whitetea.androidutils.getStore
import com.pyrus.pyrusservicedesk._ref.whitetea.bind.BinderLifecycleMode
import com.pyrus.pyrusservicedesk._ref.whitetea.core.ViewRenderer
import com.pyrus.pyrusservicedesk._ref.whitetea.utils.diff
import com.pyrus.pyrusservicedesk.databinding.PsdSearchFragmentBinding
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull


internal class SearchFragment: TeaFragment<Model, Event, Effect>(), NoFullScreenFragment {

    lateinit var binding: PsdSearchFragmentBinding

    private val adapter: SearchAdapter by lazy {
        SearchAdapter { ticketId, commentId, userId ->
            dispatch(Event.OnTicketClick(ticketId, commentId, userId))
        }
    }

    private val inputTextWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            dispatch(Event.OnSearchChanged(s.toString()))
        }
    }

    @OptIn(FlowPreview::class)
    private fun bindFeature() {

        val feature = getStore { injector().searchFeatureFactory.create() }

        bind(BinderLifecycleMode.CREATE_DESTROY) {
            this@SearchFragment.messages.mapNotNull(SearchMapper::map) bindTo feature
            this@SearchFragment.messages.mapNotNull(SearchMapper::mapSearch).debounce(100) bindTo feature
        }
        bind {
            feature.state.map(SearchMapper::map) bindTo this@SearchFragment
            feature.effects.map(SearchMapper::map) bindTo this@SearchFragment
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = PsdSearchFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val deferringInsetsListener = RootViewDeferringInsetsCallback(
            persistentInsetTypes = WindowInsetsCompat.Type.systemBars(),
            deferredInsetTypes = WindowInsetsCompat.Type.ime()
        )
        ViewCompat.setOnApplyWindowInsetsListener(binding.root, deferringInsetsListener)

        binding.searchEditText.addTextChangedListener(inputTextWatcher)
        binding.toolbarBack.setOnClickListener {
            dispatch(Event.OnCloseClick)
        }

        binding.searchRv.layoutManager = LinearLayoutManager(requireContext())
        binding.searchRv.adapter = adapter
        binding.searchRv.itemAnimator = null

        binding.cleanButton.setOnClickListener {
            binding.searchEditText.text = null
            showKeyboardOn(binding.searchEditText)
        }

        if (savedInstanceState == null) {
            showKeyboardOn(binding.searchEditText)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindFeature()
    }

    override fun createRenderer(): ViewRenderer<Model> = diff {
        diff(Model::searchResults, { new, old -> new == old }) {
            adapter.submitList(it)
        }
        diff(Model::search) {
            if (!binding.searchEditText.hasFocus()) {
                binding.searchEditText.setText(it)
            }
            binding.cleanButton.isVisible = it.isNotEmpty()
        }
        diff(Model::showEmptyPage) { showEmptyPage ->
            binding.infoIcon.isVisible = showEmptyPage
            binding.infoText.isVisible = showEmptyPage
        }
        diff(Model::emptyPageText) { emptyPageText ->
            binding.infoText.text = emptyPageText?.text(requireContext())
        }
    }

    override fun handleEffect(effect: Effect) = when(effect){
        Effect.CloseKeyboard -> hideKeyboard(binding.searchEditText)
        Effect.Exit -> injector().router.exit()
        is Effect.OpenTicket -> injector().router.navigateTo(SdScreens.TicketScreen(effect.ticketId, effect.commentId, effect.user))
    }

    internal companion object {
        fun newInstance(): SearchFragment = SearchFragment()
    }

    private fun hideKeyboard(view: View) {
        val inputMethodManager = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) ?: null
        if (inputMethodManager != null)
            (inputMethodManager as InputMethodManager).hideSoftInputFromWindow(view.windowToken, 0)
    }
}