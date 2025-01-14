package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import com.github.terrakok.cicerone.Navigator
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.injector
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.ServiceDeskConfiguration
import com.pyrus.pyrusservicedesk._ref.Screens
import com.pyrus.pyrusservicedesk._ref.utils.navigation.PyrusNavigator
import com.pyrus.pyrusservicedesk._ref.utils.navigation.setSlideRightAnimation
import com.pyrus.pyrusservicedesk.core.Account
import com.pyrus.pyrusservicedesk.core.StaticRepository
import com.pyrus.pyrusservicedesk.core.getAppId
import com.pyrus.pyrusservicedesk.core.getUserId
import com.pyrus.pyrusservicedesk.core.isMultiChat
import com.pyrus.pyrusservicedesk.databinding.PsdActivityMainBinding
import com.pyrus.pyrusservicedesk.sdk.repositories.UserInternal


internal class MainActivity : FragmentActivity() {

    private lateinit var binding: PsdActivityMainBinding

    private val navigator: Navigator = PyrusNavigator(this, R.id.fragment_container)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val theme = when{
            StaticRepository.getConfiguration().isDialogTheme -> R.style.PyrusServiceDesk_Dialog
            StaticRepository.getConfiguration().forceDarkAllowed -> R.style.PyrusServiceDesk
            else -> R.style.BasePyrusServiceDesk
        }
        setTheme(theme)

        binding = PsdActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        setupWindowInsets(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        savedInstanceState?.let { ServiceDeskConfiguration.restore(it) }

        // TODO check it
//        lifecycleScope.launch {
//            repeatOnLifecycle(Lifecycle.State.STARTED) {
//                PyrusServiceDesk.ticketsListStateFlow.collect {
//                    injector().router.newRootScreen(Screens.TicketsScreen())
//                }
//            }
//        }

        if (savedInstanceState == null) {
            val account = intent.getParcelableExtra<Account>(KEY_ACCOUNT)!!
            if (account.isMultiChat()) {
                injector().router.newRootScreen(Screens.TicketsScreen().setSlideRightAnimation())
            }
            else {
                val user = UserInternal(account.getUserId(), account.getAppId())
                injector().router.newRootScreen(Screens.TicketScreen(null, user).setSlideRightAnimation())
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // TODO sds
//        viewModel.onStart()
    }

    override fun onStop() {
        // TODO sds
//        viewModel.onStop()
        super.onStop()
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

    override fun finish() {
        super.finish()
        // TODO
//        PyrusServiceDesk.onServiceDeskStop()
    }

    companion object {

        private const val KEY_ACCOUNT = "KEY_ACCOUNT"

        fun createLaunchIntent(context: Context, account: Account): Intent {
            return Intent(context, MainActivity::class.java).putExtra(KEY_ACCOUNT, account)
        }
    }

}