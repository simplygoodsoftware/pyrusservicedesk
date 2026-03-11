package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.rootFragment

import android.app.AlertDialog
import android.os.Build
import android.os.Build.VERSION
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.github.terrakok.cicerone.Navigator
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory
import com.pyrus.pyrusservicedesk.NoFullScreenFragment
import com.pyrus.pyrusservicedesk.OpenTicketAction
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.injector
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.ServiceDeskConfiguration
import com.pyrus.pyrusservicedesk._ref.ui_domain.access_denied.AccessDeniedFeature
import com.pyrus.pyrusservicedesk._ref.ui_domain.access_denied.AccessFeatureContract.Effect
import com.pyrus.pyrusservicedesk._ref.ui_domain.access_denied.AccessFeatureContract.Message
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsFragment
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk._ref.utils.insets.RootViewDeferringInsetsCallback
import com.pyrus.pyrusservicedesk._ref.utils.navigation.PyrusNavigator
import com.pyrus.pyrusservicedesk._ref.utils.text
import com.pyrus.pyrusservicedesk._ref.whitetea.android.TeaFragment
import com.pyrus.pyrusservicedesk._ref.whitetea.androidutils.getStore
import com.pyrus.pyrusservicedesk._ref.whitetea.bind.BinderLifecycleMode
import com.pyrus.pyrusservicedesk._ref.whitetea.bind.bind
import com.pyrus.pyrusservicedesk.core.refresh.AutoRefreshFeature
import com.pyrus.pyrusservicedesk.databinding.PsdRootFragmentBinding

internal class RootFragment: TeaFragment<Unit, Message.Outer, Effect.Outer>(),
    NoFullScreenFragment {

    private lateinit var binding: PsdRootFragmentBinding

    private lateinit var accessDeniedFeature: AccessDeniedFeature

    private val navigator: Navigator by lazy { PyrusNavigator(requireActivity(), R.id.fragment_container) }

    private var dialogIsOpen: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val window: Window = requireActivity().window
        accessDeniedFeature = getStore {
            injector().accessDeniedFeatureFactory.create()
        }
        bind(requireActivity().lifecycleScope, requireActivity().lifecycle, BinderLifecycleMode.CREATE_DESTROY) {
            messages bindTo accessDeniedFeature
        }
        bind(requireActivity().lifecycleScope, requireActivity().lifecycle) {
            accessDeniedFeature.effects bindTo this@RootFragment
        }


        if (VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }

        savedInstanceState?.let { ServiceDeskConfiguration.restore(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreate(savedInstanceState)
        binding = PsdRootFragmentBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val deferringInsetsListener = RootViewDeferringInsetsCallback(
            persistentInsetTypes = WindowInsetsCompat.Type.systemBars(),
            deferredInsetTypes = WindowInsetsCompat.Type.ime()
        )
        ViewCompat.setOnApplyWindowInsetsListener(binding.root, deferringInsetsListener)

        childFragmentManager.addOnBackStackChangedListener {
            val topFragment =
                childFragmentManager.fragments.findLast { it != null && it.isVisible }
            if (topFragment is TicketsFragment) {
                if (injector().rateTimeUseCase.isTimeToRate()) {
                    showRateUsDialog()
                }
                injector().audioWrapper.clearPositions()
            }
        }
    }
    override fun handleEffect(effect: Effect.Outer) {
        when(effect) {
            Effect.Outer.CloseServiceDesk -> {
                finish()
            }
            is Effect.Outer.OpenBackwardScreen -> {
                try { startActivity(effect.screen) } catch (e: Exception) {}
            }
            is Effect.Outer.ShowAccessDeniedDialog -> {
                if (!dialogIsOpen) {
                    val dialog = createDialog(
                        resources.getString(R.string.psd_no_access),
                        effect.message.text(requireContext()),
                        effect.usersIsEmpty
                    )
                    dialog.setOnShowListener {
                        val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        button.setTextColor(ConfigUtils.getAccentColor(requireContext()))
                    }
                    dialog.setCancelable(false)
                    dialog.show()
                    dialogIsOpen = true
                }
            }
        }
    }

    private fun createDialog(title: String, message: String, usersIsEmpty: Boolean): AlertDialog {
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireActivity(), R.style.CommonAlertDialog)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton(
                injector().resourceContextWrapper.createLocalizedContext(
                    requireActivity()
                ).resources.getString(R.string.ok)
            )
            { dialog, id ->
                dispatch(Message.Outer.OnDialogPositiveButtonClick(usersIsEmpty))
                dialogIsOpen = false
                dialog.cancel()
            }
        return builder.create()
    }

    override fun onResume() {
        super.onResume()
        injector().navHolder.setNavigator(navigator)
    }

    override fun onPause() {
        injector().navHolder.removeNavigator()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        ServiceDeskConfiguration.save(outState)
    }

    private fun showRateUsDialog() {
        val manager = ReviewManagerFactory.create(requireActivity())
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener(OnCompleteListener { task: Task<ReviewInfo> ->
            if (task.isSuccessful) {
                val reviewInfo: ReviewInfo = task.result
                val flow = manager.launchReviewFlow(requireActivity(), reviewInfo)
                flow.addOnCompleteListener(OnCompleteListener { launchTask: Task<Void> ->
                    injector().rateTimeUseCase.onAppRated()
                })
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        PyrusServiceDesk.onServiceDeskStop()
    }

    private fun finish() {
        requireActivity().finish()
    }

    companion object {
        private const val KEY_OPEN_TICKET_ACTION = "KEY_OPEN_TICKET_ACTION"
        const val TAG = "RootFragment"

        fun newInstance(openTicketAction: OpenTicketAction?): RootFragment {
            val fragment = RootFragment()
            val args = Bundle().apply {
                putParcelable(KEY_OPEN_TICKET_ACTION, openTicketAction)
            }
            fragment.arguments = args
            return fragment
        }
    }

}