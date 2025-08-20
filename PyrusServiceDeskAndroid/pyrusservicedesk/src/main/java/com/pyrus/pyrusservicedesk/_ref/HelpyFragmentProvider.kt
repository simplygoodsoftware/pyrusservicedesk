package com.pyrus.pyrusservicedesk._ref

import androidx.fragment.app.Fragment
import com.pyrus.pyrusservicedesk.OpenTicketAction
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.rootFragment.RouterFragment

class HelpyFragmentProvider: FragmentProvider {
    override fun provideFragment(openTicketAction: OpenTicketAction?): Fragment {
        return RouterFragment.newInstance(openTicketAction)
    }
}

interface FragmentProvider {
    fun provideFragment(openTicketAction: OpenTicketAction?): Fragment
}