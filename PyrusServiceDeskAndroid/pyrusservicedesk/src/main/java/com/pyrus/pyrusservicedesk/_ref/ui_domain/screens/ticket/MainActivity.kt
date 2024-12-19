package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import com.github.terrakok.cicerone.Navigator
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.injector
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.ServiceDeskConfiguration
import com.pyrus.pyrusservicedesk._ref.Screens
import com.pyrus.pyrusservicedesk._ref.utils.setupWindowInsets
import com.pyrus.pyrusservicedesk.core.StaticRepository
import com.pyrus.pyrusservicedesk.databinding.PsdActivityMainBinding
import com.pyrus.pyrusservicedesk._ref.utils.navigation.PyrusNavigator


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

        if (savedInstanceState == null) {
//            supportFragmentManager.beginTransaction()
//                .replace(R.id.fragment_container, TicketsFragment())
//                .commit()
            //injector().router.newRootScreen(Screens.TicketScreen())
            injector().router.newRootScreen(Screens.TicketsScreen())
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

    // TODO
//    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
//        val menuDelegate = ConfigUtils.getMainMenuDelegate()
//        if (menuDelegate != null && menu != null)
//            return menuDelegate.onCreateOptionsMenu(menu, this)
//
//        return menu?.let{
//            MenuInflater(this).inflate(R.menu.psd_main_menu, menu)
//            val closeItem = menu.findItem(R.id.psd_main_menu_close)
//            closeItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
//            closeItem.icon?.setColorFilter(
//                ConfigUtils.getToolbarButtonColor(this),
//                PorterDuff.Mode.SRC_ATOP
//            )
//            true
//        } ?: false
//    }

    override fun finish() {
        super.finish()
        // TODO
//        PyrusServiceDesk.onServiceDeskStop()
    }

    companion object {
        fun createLaunchIntent(context: Context): Intent {
            return Intent(context, MainActivity::class.java)
        }
    }

}