package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket

import android.app.ComponentCaller
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Build.VERSION
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import com.github.terrakok.cicerone.Navigator
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory
import com.pyrus.pyrusservicedesk.OpenTicketAction
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.injector
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.ServiceDeskConfiguration
import com.pyrus.pyrusservicedesk._ref.SdScreens
import com.pyrus.pyrusservicedesk._ref.utils.navigation.PyrusNavigator
import com.pyrus.pyrusservicedesk.core.Account
import com.pyrus.pyrusservicedesk.core.StaticRepository
import com.pyrus.pyrusservicedesk.databinding.PsdActivityMainBinding


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

//
//        val decorView = window.decorView
//        var flags: Int = decorView.getSystemUiVisibility()
//        flags = flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
//        decorView.setSystemUiVisibility(flags)

        binding = PsdActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        setupWindowInsets(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        savedInstanceState?.let { ServiceDeskConfiguration.restore(it) }
        super.overridePendingTransition(R.anim.fade_in, R.anim.no_animation)
        if (savedInstanceState == null) {
            val action = intent.getParcelableExtra<OpenTicketAction>(KEY_OPEN_TICKET_ACTION)
            injector().router.newRootScreen(SdScreens.RouterScreen(action))
        }
    }

    override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
        super.onNewIntent(intent, caller)

        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
//        val action = intent.getParcelableExtra<OpenTicketAction>(KEY_OPEN_TICKET_ACTION)
//        intent.removeExtra(KEY_OPEN_TICKET_ACTION)
//        if (action != null) {
//            Log.d("SDS_SD", "open TicketScreen")
//            injector().router.navigateTo(Screens.TicketScreen(
//                action.ticketId,
//                UserInternal(action.user.userId, action.user.appId)
//            ))
//        }
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
        val enter = R.anim.no_animation
        val exit = R.anim.fade_out
        if (VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            super.overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, enter, exit)
        }
        else {
            super.overridePendingTransition(enter, exit)
        }
    }

    private fun showRateUsDialog() {
        val manager = ReviewManagerFactory.create(this)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener(OnCompleteListener { task: Task<ReviewInfo> ->
            if (task.isSuccessful) {
                val reviewInfo: ReviewInfo = task.result
                val flow = manager.launchReviewFlow(this, reviewInfo)
                flow.addOnCompleteListener(OnCompleteListener { launchTask: Task<Void> ->
                    injector().rateTimeUseCase.onAppRated()
                })
            }
        })
    }

    companion object {

        private const val KEY_ACCOUNT = "KEY_ACCOUNT"
        private const val KEY_OPEN_TICKET_ACTION = "KEY_OPEN_TICKET_ACTION"

        fun createLaunchIntent(context: Context, account: Account, openTicketAction: OpenTicketAction?): Intent {
            return Intent(context, MainActivity::class.java)
                .putExtra(KEY_ACCOUNT, account)
                .putExtra(KEY_OPEN_TICKET_ACTION, openTicketAction)
        }
    }

}