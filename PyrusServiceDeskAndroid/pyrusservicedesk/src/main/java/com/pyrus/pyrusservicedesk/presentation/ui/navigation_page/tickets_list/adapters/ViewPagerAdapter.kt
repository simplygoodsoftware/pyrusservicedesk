package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.adapters

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.ticketsList.TicketsListFragment

class ViewPagerAdapter(private val appCount: Int, fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = appCount

    override fun createFragment(position: Int): Fragment {
        val fragment = TicketsListFragment()
        fragment.arguments = Bundle().apply {
            putInt(KEY_APP_ID, position + 1)
        }
        return fragment
    }

    companion object {

        private const val KEY_APP_ID = "KEY_APP_ID"

    }
}