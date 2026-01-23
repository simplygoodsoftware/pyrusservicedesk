package com.pyrus.pyrusservicedesk.core.refresh

import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk._ref.utils.MILLISECONDS_IN_DAY
import com.pyrus.pyrusservicedesk._ref.utils.MILLISECONDS_IN_HOUR
import com.pyrus.pyrusservicedesk._ref.utils.MILLISECONDS_IN_MINUTE
import com.pyrus.pyrusservicedesk._ref.utils.MILLISECONDS_IN_SECOND
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Actor
import com.pyrus.pyrusservicedesk._ref.whitetea.core.StoreFactory
import com.pyrus.pyrusservicedesk._ref.whitetea.core.logic.Logic
import com.pyrus.pyrusservicedesk.core.refresh.AutoRefreshContract.StartUpdates
import com.pyrus.pyrusservicedesk.sdk.repositories.SdRepository
import com.pyrus.pyrusservicedesk.sdk.updates.LiveUpdates
import com.pyrus.pyrusservicedesk.sdk.updates.PreferencesManager
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

private const val TAG = "AutoRefreshFeature"

internal class AutoRefreshFeatureFactory(
    private val storeFactory: StoreFactory,
    private val repository: SdRepository,
    private val preferencesManager: PreferencesManager,
) {

    fun create(
        liveUpdates: LiveUpdates
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

private class FeatureReducer: Logic<Unit, Unit, StartUpdates>() {
    override fun Result.update(message: Unit) {}
}

private class AutoRefreshActor(
    private val repository: SdRepository,
    private val preferencesManager: PreferencesManager,
    private val liveUpdates: LiveUpdates,
): Actor<StartUpdates, Unit> {

    override fun handleEffect(effect: StartUpdates): Flow<Unit> = flow {
        while (currentCoroutineContext().isActive) {
            val interval = liveUpdates.getTicketsUpdateInterval(preferencesManager)

            if ((liveUpdates.isStarted || PyrusServiceDesk.sdIsOpen) && interval != -1L)
                repository.sync()

            val startTime = System.currentTimeMillis()
            while (true) {
                var interval = liveUpdates.getTicketsUpdateInterval(preferencesManager)

                if (interval == -1L)
                    interval = 1000

                val endTime = startTime + interval
                val currentTime = System.currentTimeMillis()

                if (currentTime > endTime) {
                    break
                }

                delay(1000)
            }
        }
    }
}