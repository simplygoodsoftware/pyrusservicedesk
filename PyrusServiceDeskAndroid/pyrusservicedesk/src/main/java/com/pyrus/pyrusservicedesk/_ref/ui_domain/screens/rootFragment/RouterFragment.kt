package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.rootFragment

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
import com.pyrus.pyrusservicedesk.NoFullScreenFragment
import com.pyrus.pyrusservicedesk.OpenTicketAction
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.injector
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.ServiceDeskConfiguration
import com.pyrus.pyrusservicedesk._ref.SdScreens
import com.pyrus.pyrusservicedesk._ref.ui_domain.access_denied.AccessFeatureContract.Effect
import com.pyrus.pyrusservicedesk._ref.ui_domain.access_denied.AccessFeatureContract.Message
import com.pyrus.pyrusservicedesk._ref.utils.insets.RootViewDeferringInsetsCallback
import com.pyrus.pyrusservicedesk._ref.utils.navigation.PyrusNavigator
import com.pyrus.pyrusservicedesk._ref.utils.navigation.setSlideRightAnimation
import com.pyrus.pyrusservicedesk._ref.whitetea.android.TeaFragment
import com.pyrus.pyrusservicedesk.core.getAppId
import com.pyrus.pyrusservicedesk.core.getUserId
import com.pyrus.pyrusservicedesk.core.isMultiChat
import com.pyrus.pyrusservicedesk.databinding.PsdRootFragmentBinding
import com.pyrus.pyrusservicedesk.sdk.repositories.UserInternal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class RouterFragment: TeaFragment<Unit, Message.Outer, Effect.Outer>(),
    NoFullScreenFragment {

    private lateinit var binding: PsdRootFragmentBinding

    private val navigator: Navigator by lazy { PyrusNavigator(requireActivity(), R.id.fragment_container) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val window: Window = requireActivity().window

        if (VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }

        savedInstanceState?.let { ServiceDeskConfiguration.restore(it) }
         if (savedInstanceState == null) {
             val action = arguments?.getParcelable<OpenTicketAction>(KEY_OPEN_TICKET_ACTION)
             val rootFragment = RootFragment.newInstance(action)
             fragmentManager?.beginTransaction()
                 ?.add(R.id.fragment_root_container, rootFragment)
                 ?.commit()
            val account = injector().accountStore.getAccount()

            if (account.isMultiChat()) {
                if (action != null) {
                    injector().router.newRootChain(
                        SdScreens.TicketsScreen(),
                        SdScreens.TicketScreen(
                            action.ticketId,
                            UserInternal(action.user.userId, action.user.appId)
                        ).setSlideRightAnimation()
                    )
                }
                else {
                    injector().router.newRootScreen(SdScreens.TicketsScreen())
                }
            }
            else {
                val userId = account.getUserId()
                val appId = account.getAppId()
                if (userId == null || appId == null) return

                val user = UserInternal(userId, appId)
                lifecycleScope.launch(Dispatchers.IO) {
                    //val localId = injector().localCommandsStore.getNextLocalId()
                    val lastTicketId = injector().localCommandsStore.getLastTicketId()
                    injector().router.newRootScreen(SdScreens.TicketScreen(lastTicketId, user).setSlideRightAnimation())
                }
            }
        }
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

    companion object {
        private const val KEY_OPEN_TICKET_ACTION = "KEY_OPEN_TICKET_ACTION"
        const val TAG = "RootFragment"

        fun newInstance(openTicketAction: OpenTicketAction?): RouterFragment {
            val fragment = RouterFragment()
            val args = Bundle().apply {
                putParcelable(KEY_OPEN_TICKET_ACTION, openTicketAction)
            }
            fragment.arguments = args
            return fragment
        }
    }

}