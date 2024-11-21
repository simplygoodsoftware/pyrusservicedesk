package com.pyrus.pyrusservicedesk._ref.whitetea.instance_keeper

internal interface InstanceKeeper {

    fun get(key: Any): Instance?

    fun put(key: Any, instance: Instance)

    fun remove(key: Any): Instance?

    interface Instance {
        fun onDestroy()
    }
}