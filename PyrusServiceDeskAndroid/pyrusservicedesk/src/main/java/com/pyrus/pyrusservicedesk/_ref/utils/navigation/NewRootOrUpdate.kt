package com.pyrus.pyrusservicedesk._ref.utils.navigation

import com.github.terrakok.cicerone.Command
import com.github.terrakok.cicerone.androidx.FragmentScreen


internal data class NewRootOrUpdate(val screen: FragmentScreen, val payload: Any?): Command