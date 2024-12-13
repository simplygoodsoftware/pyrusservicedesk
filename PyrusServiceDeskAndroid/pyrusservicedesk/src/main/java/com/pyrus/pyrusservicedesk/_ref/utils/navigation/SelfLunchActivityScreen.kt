package com.pyrus.pyrusservicedesk._ref.utils.navigation

import android.app.Activity
import com.github.terrakok.cicerone.Screen

interface SelfLunchActivityScreen: Screen {

    fun start(activity: Activity)

    companion object {
        operator fun invoke(
            key: String,
            start: (activity: Activity) -> Unit,
        ) = object : SelfLunchActivityScreen {

            override fun start(activity: Activity) {
                start(activity)
            }

            override val screenKey = key
        }
    }

}