package com.pyrus.pyrusservicedesk.core.refresh

import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk._ref.utils.MILLISECONDS_IN_MINUTE
import com.pyrus.pyrusservicedesk._ref.utils.isSuccess
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Actor
import com.pyrus.pyrusservicedesk._ref.whitetea.core.StoreFactory
import com.pyrus.pyrusservicedesk._ref.whitetea.core.logic.Logic
import com.pyrus.pyrusservicedesk.sdk.repositories.LocalTicketsStore
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
    private val localTicketsStore: LocalTicketsStore,
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
            systemMessageStore = systemMessageStore,
            localTicketsStore = localTicketsStore,
        ),
        initialEffects = listOf(
            AutoRefreshContract.Effect.StartUpdates,
            AutoRefreshContract.Effect.StartUpdatesSystemMessage
        )
    )

}

private class FeatureReducer : Logic<Unit, Unit, AutoRefreshContract.Effect>() {
    override fun Result.update(message: Unit) {}
}

private class AutoRefreshActor(
    private val repository: SdRepository,
    private val preferencesManager: PreferencesManager,
    private val liveUpdates: LiveUpdates,
    private val systemMessageStore: SystemMessageStore,
    private val localTicketsStore: LocalTicketsStore,
) : Actor<AutoRefreshContract.Effect, Unit> {

    override fun handleEffect(effect: AutoRefreshContract.Effect): Flow<Unit> = when (effect) {
        is AutoRefreshContract.Effect.StartUpdates -> flow {
            while (currentCoroutineContext().isActive) {
                val interval = liveUpdates.getTicketsUpdateInterval(preferencesManager)
                if ((liveUpdates.isStarted || PyrusServiceDesk.sdIsOpen) && interval != -1L)
                    repository.sync()

                val startTime = System.currentTimeMillis()
                while (true) {
                    val interval = liveUpdates.getTicketsUpdateInterval(preferencesManager)

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
            systemMessageStore.ticketStateFlow().collect { id ->
                startSendCalcOperatorTime(id)
            }
        }
    }

    private suspend fun startSendCalcOperatorTime(ticketId: Long?) {
        var isActive = if (ticketId != null) localTicketsStore.getTicketWithComments(ticketId)?.ticket?.isActive else null
        var id = systemMessageStore.ticketId()
        while (ticketId != null && isActive == true && id != null) {
            val resultTry = repository.sendCalcOperatorTime(ticketId)
            if (resultTry != null && resultTry.isSuccess()) {
                systemMessageStore.setOperatorResponseTimeMessage(
                    ticketId,
                    resultTry.value.operatorResponseTimeMessage
                )
            }
            val startTime = System.currentTimeMillis()
            while (true) {
                val interval = MILLISECONDS_IN_MINUTE

                val endTime = startTime + interval
                val currentTime = System.currentTimeMillis()

                id = systemMessageStore.ticketId()
                if (currentTime > endTime || id == null) {
                    break
                }

                delay(1000)
            }
            isActive = localTicketsStore.getTicketWithComments(ticketId)?.ticket?.isActive
            id = systemMessageStore.ticketId()
        }
    }
}