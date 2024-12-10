package com.pyrus.pyrusservicedesk._ref.whitetea.instance_keeper

import com.pyrus.pyrusservicedesk._ref.whitetea.instance_keeper.InstanceKeeper.Instance

internal class DefaultInstanceKeeperDispatcher : InstanceKeeperDispatcher {

    private val map = HashMap<Any, Instance>()

    override fun get(key: Any): Instance? = map[key]

    override fun put(key: Any, instance: Instance) {
        check(key !in map) { "Another instance is already associated with the key: $key" }

        map[key] = instance
    }

    override fun remove(key: Any): Instance? = map.remove(key)

    override fun destroy() {
        map.values.forEach(Instance::onDestroy)
        map.clear()
    }
}