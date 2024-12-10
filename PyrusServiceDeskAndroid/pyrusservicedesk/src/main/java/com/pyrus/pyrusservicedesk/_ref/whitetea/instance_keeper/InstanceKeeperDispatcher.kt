package com.pyrus.pyrusservicedesk._ref.whitetea.instance_keeper

internal interface InstanceKeeperDispatcher : InstanceKeeper {

    fun destroy()

}

@Suppress("FunctionName")
internal fun InstanceKeeperDispatcher(): InstanceKeeperDispatcher = DefaultInstanceKeeperDispatcher()