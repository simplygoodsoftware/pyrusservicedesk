package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.rootFragment

import android.os.Build
import android.os.Build.VERSION
import android.os.Bundle
import android.util.Log
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
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.uiInjector
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.ServiceDeskConfiguration
import com.pyrus.pyrusservicedesk._ref.SdScreens
import com.pyrus.pyrusservicedesk._ref.ui_domain.access_denied.AccessFeatureContract.Effect
import com.pyrus.pyrusservicedesk._ref.ui_domain.access_denied.AccessFeatureContract.Message
import com.pyrus.pyrusservicedesk._ref.utils.insets.RootViewDeferringInsetsCallback
import com.pyrus.pyrusservicedesk._ref.utils.log.PLog
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
             val sendComment = arguments?.getString(KEY_SEND_COMMENT)
             val rootFragment = RootFragment.newInstance(action)
             fragmentManager?.beginTransaction()
                 ?.add(R.id.fragment_root_container, rootFragment)
                 ?.commit()
            val account = injector().accountStore.getAccount()

            // NB: do NOT read localTicketsStore here — onCreate is the main thread and Room throws
            // assertNotMainThread. Ticket counts are logged from the IO context below.
            PLog.d(RootFragment.TAG, "ET RouterFragment: isMultiChat=${account.isMultiChat()} userId=${account.getUserId()} appId=${account.getAppId()?.take(8)} openTicketId=${action?.ticketId}")
            Log.d(RootFragment.TAG, "ET RouterFragment: isMultiChat=${account.isMultiChat()} userId=${account.getUserId()} openTicketId=${action?.ticketId}")

            if (account.isMultiChat()) {
                if (action != null) {
                    uiInjector().router.newRootChain(
                        SdScreens.TicketsScreen(),
                        SdScreens.TicketScreen(
                            action.ticketId,
                            UserInternal(action.user.userId, action.user.appId),
                            null, //TODO sendComment for multichat
                        ).setSlideRightAnimation()
                    )
                }
                else {
                    uiInjector().router.newRootScreen(SdScreens.TicketsScreen())
                }
            }
            else {
                val userId = account.getUserId()
                val appId = account.getAppId()
                if (userId == null || appId == null) return

                val user = UserInternal(userId, appId)
                val router = uiInjector().router
                val localTicketsStore = injector().localTicketsStore
                lifecycleScope.launch(Dispatchers.IO) {
                    val tickets = localTicketsStore.getTickets()
                    val lastTicketId = tickets.lastOrNull()?.ticketId
                        ?: injector().localCommandsStore.getNextLocalId()
                    PLog.d(RootFragment.TAG, "ET RouterFragment single-chat: localTicketsCount=${tickets.size}, lastTicketId=${tickets.lastOrNull()?.ticketId}, lastServerTicketId=${tickets.maxBy { it.ticketId }.ticketId} chosenTicketId=$lastTicketId")
                    PLog.d(RootFragment.TAG, "ET RouterFragment single-chat: localTicketsCount=${tickets.size}, lastTicketId=${tickets.lastOrNull()?.ticketId}, lastServerTicketId=${tickets.maxBy { it.ticketId }.ticketId} chosenTicketId=$lastTicketId")
                    router.newRootScreen(SdScreens.TicketScreen(lastTicketId, user, sendComment).setSlideRightAnimation())
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
        uiInjector().navHolder.setNavigator(navigator)
    }

    override fun onPause() {
        uiInjector().navHolder.removeNavigator()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        ServiceDeskConfiguration.save(outState)
    }

    companion object {
        private const val KEY_OPEN_TICKET_ACTION = "KEY_OPEN_TICKET_ACTION"
        const val TAG = "RootFragment"
        private const val KEY_SEND_COMMENT = "KEY_SEND_COMMENT"

        fun newInstance(openTicketAction: OpenTicketAction?, sendComment: String?): RouterFragment {
            val fragment = RouterFragment()
            val args = Bundle().apply {
                putParcelable(KEY_OPEN_TICKET_ACTION, openTicketAction)
                putString(KEY_SEND_COMMENT, sendComment)
            }
            fragment.arguments = args
            return fragment
        }
    }

}