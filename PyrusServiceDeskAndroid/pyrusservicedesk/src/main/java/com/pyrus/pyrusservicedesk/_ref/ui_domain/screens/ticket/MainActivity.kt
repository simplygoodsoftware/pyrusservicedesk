package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket

import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.fragment.app.FragmentActivity
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.ServiceDeskConfiguration
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.TicketFragment
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk._ref.utils.setupWindowInsets
import com.pyrus.pyrusservicedesk.databinding.PsdActivityMainBinding

/**
 * Activity for rendering ticket/feed comments.
 */
internal class MainActivity : FragmentActivity() {

    private lateinit var binding: PsdActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = PsdActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        setupWindowInsets(binding.root)

        savedInstanceState?.let { ServiceDeskConfiguration.restore(it) }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, TicketFragment.newInstance())
                .commit()
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        ServiceDeskConfiguration.save(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val menuDelegate = ConfigUtils.getMainMenuDelegate()
        if (menuDelegate != null && menu != null)
            return menuDelegate.onCreateOptionsMenu(menu, this)

        return menu?.let{
            MenuInflater(this).inflate(R.menu.psd_main_menu, menu)
            val closeItem = menu.findItem(R.id.psd_main_menu_close)
            closeItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            closeItem.icon?.setColorFilter(
                ConfigUtils.getToolbarButtonColor(this),
                PorterDuff.Mode.SRC_ATOP
            )
            true
        } ?: false
    }

    override fun finish() {
        super.finish()
        PyrusServiceDesk.onServiceDeskStop()
    }

}