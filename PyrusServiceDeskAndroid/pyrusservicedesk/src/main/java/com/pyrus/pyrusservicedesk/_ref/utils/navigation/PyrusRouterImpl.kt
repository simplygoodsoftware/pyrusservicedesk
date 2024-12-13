package com.pyrus.pyrusservicedesk._ref.utils.navigation

import com.github.terrakok.cicerone.Router
import com.github.terrakok.cicerone.androidx.FragmentScreen
import com.pyrus.pyrusservicedesk._ref.utils.navigation.NewRootOrUpdate
import com.pyrus.pyrusservicedesk._ref.utils.navigation.PyrusRouter

class PyrusRouterImpl: Router(), PyrusRouter {

    override fun newRootScreenOrUpdate(screen: FragmentScreen, payload: Any?) {
        executeCommands(NewRootOrUpdate(screen, payload))
    }

}