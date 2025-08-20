package com.pyrus.pyrusservicedesk.core.refresh

import com.pyrus.pyrusservicedesk._ref.utils.MILLISECONDS_IN_MINUTE
import com.pyrus.pyrusservicedesk._ref.utils.MILLISECONDS_IN_SECOND
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Actor
import com.pyrus.pyrusservicedesk._ref.whitetea.core.StoreFactory
import com.pyrus.pyrusservicedesk._ref.whitetea.core.logic.Logic
import com.pyrus.pyrusservicedesk.core.refresh.AutoRefreshContract.StartUpdates
import com.pyrus.pyrusservicedesk.sdk.repositories.SdRepository
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

    fun create(): AutoRefreshFeature = storeFactory.create(
        name = TAG,
        initialState = Unit,
        reducer = FeatureReducer(),
        actor = AutoRefreshActor(
            repository = repository,
            preferencesManager = preferencesManager
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
): Actor<StartUpdates, Unit> {

    override fun handleEffect(effect: StartUpdates): Flow<Unit> = flow {
        while (currentCoroutineContext().isActive) {

            repository.sync()

            val startTime = System.currentTimeMillis()
            while (true) {
                val lastActiveTime = preferencesManager.getLastActiveTime()
                val interval = getTicketsUpdateInterval(lastActiveTime)

                val endTime = startTime + interval
                val currentTime = System.currentTimeMillis()

                if (currentTime > endTime) {
                    break
                }

                delay(1000)
            }
        }
    }

    private fun getTicketsUpdateInterval(lastActiveTime: Long): Long {
        val diff = System.currentTimeMillis() - lastActiveTime
        return when {
            diff < 1.5 * MILLISECONDS_IN_MINUTE -> 5L * MILLISECONDS_IN_SECOND
            diff < 5 * MILLISECONDS_IN_MINUTE -> 15L * MILLISECONDS_IN_SECOND
            else -> MILLISECONDS_IN_MINUTE.toLong()
        }
    }

}