package com.pyrus.pyrusservicedesk._ref.utils.navigation

import com.github.terrakok.cicerone.Router
import com.github.terrakok.cicerone.androidx.FragmentScreen

class PyrusRouterImpl: Router(), PyrusRouter {

    override fun newRootScreenOrUpdate(screen: FragmentScreen, payload: Any?) {
        executeCommands(NewRootOrUpdate(screen, payload))
    }

}