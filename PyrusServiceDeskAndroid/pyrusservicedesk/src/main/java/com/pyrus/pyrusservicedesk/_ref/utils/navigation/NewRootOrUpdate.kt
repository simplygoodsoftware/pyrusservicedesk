package com.pyrus.pyrusservicedesk._ref.utils.navigation

import com.github.terrakok.cicerone.Command
import com.github.terrakok.cicerone.androidx.FragmentScreen


data class NewRootOrUpdate(val screen: FragmentScreen, val payload: Any?): Command