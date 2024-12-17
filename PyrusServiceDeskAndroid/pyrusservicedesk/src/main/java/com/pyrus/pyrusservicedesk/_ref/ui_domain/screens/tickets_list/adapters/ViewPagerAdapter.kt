package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.adapters

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.ticketsList.TicketsListFragment
import com.pyrus.pyrusservicedesk.sdk.data.Application

class ViewPagerAdapter(fm: FragmentManager, lf: Lifecycle) : FragmentStateAdapter(fm, lf) {

    private var appIds = listOf<String>()
    private var titleList = listOf<String>()

    override fun getItemCount() = appIds.size

    override fun createFragment(position: Int): Fragment {
        val fragment = TicketsListFragment()
        fragment.arguments = Bundle().apply {
            putString(KEY_APP_ID, appIds[position])
        }
        return fragment
    }

    fun setItems(applications: HashSet<Application>) {
        this.appIds = applications.map { it.appId ?: "" }
        this.titleList = applications.map { it.orgName ?: "" }
        notifyDataSetChanged()
    }

    fun getTitle(position: Int): String = titleList[position]

    fun getAppId(position: Int): String = appIds[position]

    companion object {

        private const val KEY_APP_ID = "KEY_APP_ID"

    }
}