package com.pyrus.pyrusservicedesk.core.refresh

import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Actor
import com.pyrus.pyrusservicedesk._ref.whitetea.core.StoreFactory
import com.pyrus.pyrusservicedesk._ref.whitetea.core.logic.Logic
import com.pyrus.pyrusservicedesk.core.refresh.AutoRefreshContract.StartUpdates
import com.pyrus.pyrusservicedesk.sdk.repositories.SdRepository
import com.pyrus.pyrusservicedesk.sdk.updates.LiveUpdates
import com.pyrus.pyrusservicedesk.sdk.updates.PreferencesManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

private const val TAG = "AutoRefreshFeature"

internal class AutoRefreshFeatureFactory(
    private val storeFactory: StoreFactory,
    private val repository: SdRepository,
    private val preferencesManager: PreferencesManager,
) {

    fun create(
        liveUpdates: LiveUpdates,
    ): AutoRefreshFeature = storeFactory.create(
        name = TAG,
        initialState = Unit,
        reducer = FeatureReducer(),
        actor = AutoRefreshActor(
            repository = repository,
            preferencesManager = preferencesManager,
            liveUpdates = liveUpdates,
        ),
        initialEffects = listOf(StartUpdates)
    )

}

private class FeatureReducer : Logic<Unit, Unit, StartUpdates>() {
    override fun Result.update(message: Unit) {}
}

private class AutoRefreshActor(
    private val repository: SdRepository,
    private val preferencesManager: PreferencesManager,
    private val liveUpdates: LiveUpdates,
) : Actor<StartUpdates, Unit> {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun handleEffect(effect: StartUpdates): Flow<Unit> = combine(
        liveUpdates.isStartedFlow(),
        preferencesManager.getLastActiveTimeFlow(),
        PyrusServiceDesk.sdIsOpenFlow(),
    ) { isStarted, lastActiveTime, sdIsOpen ->
        val interval = liveUpdates.getTicketsUpdateInterval(lastActiveTime)
        if (isStarted || sdIsOpen)
            interval to lastActiveTime
        else
            -1L to -1L
    }
        .distinctUntilChanged()
        .flatMapLatest { data ->
            if (data.first == -1L) {
                flow { }
            }
            else {
                flow {
                    while (currentCoroutineContext().isActive) {
                        val interval = liveUpdates.getTicketsUpdateInterval(data.second)
                        repository.sync()
                        delay(interval)
                    }
                }
            }
        }
}