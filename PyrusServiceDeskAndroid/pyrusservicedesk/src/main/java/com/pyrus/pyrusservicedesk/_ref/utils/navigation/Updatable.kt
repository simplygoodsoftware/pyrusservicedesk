package com.pyrus.pyrusservicedesk._ref.utils.navigation


internal interface Updatable {

    fun isTheSameScreen(screenKey: String): Boolean

    fun onScreenUpdate(payload: Any?)

}