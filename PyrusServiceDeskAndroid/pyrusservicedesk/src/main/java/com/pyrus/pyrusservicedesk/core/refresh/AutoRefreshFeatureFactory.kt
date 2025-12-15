package com.pyrus.pyrusservicedesk.core.refresh

import android.util.Log
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk._ref.utils.MILLISECONDS_IN_DAY
import com.pyrus.pyrusservicedesk._ref.utils.MILLISECONDS_IN_HOUR
import com.pyrus.pyrusservicedesk._ref.utils.MILLISECONDS_IN_MINUTE
import com.pyrus.pyrusservicedesk._ref.utils.MILLISECONDS_IN_SECOND
import com.pyrus.pyrusservicedesk._ref.utils.isSuccess
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Actor
import com.pyrus.pyrusservicedesk._ref.whitetea.core.StoreFactory
import com.pyrus.pyrusservicedesk._ref.whitetea.core.logic.Logic
import com.pyrus.pyrusservicedesk.sdk.repositories.SdRepository
import com.pyrus.pyrusservicedesk.sdk.repositories.SystemMessageStore
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
    private val systemMessageStore: SystemMessageStore,
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
            systemMessageStore = systemMessageStore
        ),
        initialEffects = listOf(AutoRefreshContract.Effect.StartUpdates, AutoRefreshContract.Effect.StartUpdatesSystemMessage)
    )

}

private class FeatureReducer: Logic<Unit, Unit, AutoRefreshContract.Effect>() {
    override fun Result.update(message: Unit) {}
}

private class AutoRefreshActor(
    private val repository: SdRepository,
    private val preferencesManager: PreferencesManager,
    private val liveUpdates: LiveUpdates,
    private val systemMessageStore: SystemMessageStore,
): Actor<AutoRefreshContract.Effect, Unit> {

    override fun handleEffect(effect: AutoRefreshContract.Effect): Flow<Unit> = when(effect) {
        is AutoRefreshContract.Effect.StartUpdates -> flow {
            while (currentCoroutineContext().isActive) {

                if (liveUpdates.isStarted || PyrusServiceDesk.sdIsOpen)
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

        is AutoRefreshContract.Effect.StartUpdatesSystemMessage -> flow {
            systemMessageStore.ticketStateFlow().collect { ticketIds ->
                Log.d("EP ", "ticketIds: $ticketIds")
                if (ticketIds != null) {
                    for (ticketId in ticketIds) {
                        val resultTry = repository.sendCalcOperatorTime(ticketId)
                        if (resultTry != null && resultTry.isSuccess()) {
                            systemMessageStore.setOperatorResponseTimeMessage(ticketId, resultTry.value.operatorResponseTimeMessage)
                        }
                    }
                }
                val startTime = System.currentTimeMillis()
                while (true) {
                    val interval = MILLISECONDS_IN_MINUTE

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

    private fun getTicketsUpdateInterval(lastActiveTime: Long): Long {
        val diff = System.currentTimeMillis() - lastActiveTime
        return when {
            diff <= MILLISECONDS_IN_MINUTE -> 5L * MILLISECONDS_IN_SECOND
            diff <= 5 * MILLISECONDS_IN_MINUTE -> 15L * MILLISECONDS_IN_SECOND
            diff <= MILLISECONDS_IN_HOUR -> MILLISECONDS_IN_MINUTE.toLong()
            diff <= 3 * MILLISECONDS_IN_DAY -> 3 * MILLISECONDS_IN_MINUTE.toLong()
            else -> -1L
        }
    }

}