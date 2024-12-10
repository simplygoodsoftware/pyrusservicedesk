package com.pyrus.pyrusservicedesk._ref.whitetea.instance_keeper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.get
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Store
import com.pyrus.pyrusservicedesk._ref.whitetea.utils.Cancelable

/**
 * Creates a new instance of [InstanceKeeper] and attaches it to the provided AndroidX [ViewModelStore]
 */
@Suppress("FunctionName") // Factory function
internal fun InstanceKeeper(viewModelStore: ViewModelStore): InstanceKeeper =
    ViewModelProvider(
        viewModelStore,
        object : ViewModelProvider.Factory {

            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return InstanceKeeperViewModel() as T
            }
        }
    )
        .get<InstanceKeeperViewModel>()
        .instanceKeeperDispatcher

/**
 * Creates a new instance of [InstanceKeeper] and attaches it to the AndroidX [ViewModelStore]
 */
internal fun ViewModelStoreOwner.instanceKeeper(): InstanceKeeper = InstanceKeeper(viewModelStore)

internal class InstanceKeeperViewModel : ViewModel() {
    val instanceKeeperDispatcher: InstanceKeeperDispatcher = InstanceKeeperDispatcher()

    override fun onCleared() {
        instanceKeeperDispatcher.destroy()
    }
}

internal fun <T : Store<*, *, *>> InstanceKeeper.getStore(key: Any, factory: () -> T): T =
    getOrCreate(key = key) {
        StoreInstance(factory())
    }.store

internal fun <T : Store<*, *, *>> InstanceKeeper.getStoreOrNull(key: Any): T? =
    getOrNull<StoreInstance<T>>(key = key)?.store

internal fun <T : Cancelable> InstanceKeeper.getInstance(key: Any, factory: () -> T): T =
    getOrCreate(key = key) {
        CancelableInstance(factory())
    }.instance

internal fun <T : Cancelable> InstanceKeeper.getInstanceOrNull(key: Any): T? =
    getOrNull<CancelableInstance<T>>(key = key)?.instance

internal inline fun <reified T : Store<*, *, *>> InstanceKeeper.getStore(noinline factory: () -> T): T =
    getStore(key = T::class, factory = factory)

internal inline fun <reified T : Cancelable> InstanceKeeper.getInstance(noinline factory: () -> T): T =
    getInstance(key = T::class, factory = factory)

internal inline fun <reified T : Cancelable> InstanceKeeper.getInstanceOrNull(): T? =
    getInstanceOrNull(key = T::class)

internal inline fun <reified T : Store<*, *, *>> InstanceKeeper.getStoreOrNull(): T? =
    getStoreOrNull(key = T::class)

internal inline fun <reified T : InstanceKeeper.Instance> InstanceKeeper.getOrCreate(key: Any, factory: () -> T): T {
    var instance: T? = get(key) as T?
    if (instance == null) {
        instance = factory()
        put(key, instance)
    }

    return instance
}

internal inline fun <reified T : InstanceKeeper.Instance> InstanceKeeper.getOrNull(key: Any): T? {
    return get(key) as T?
}

internal inline fun <reified T : InstanceKeeper.Instance> InstanceKeeper.getOrCreate(factory: () -> T): T = getOrCreate(T::class, factory)

private class StoreInstance<out T : Store<*, *, *>>(
    val store: T
) : InstanceKeeper.Instance {
    override fun onDestroy() {
        store.cancel()
    }
}

private class CancelableInstance<out T : Cancelable>(
    val instance: T
) : InstanceKeeper.Instance {
    override fun onDestroy() {
        instance.cancel()
    }
}