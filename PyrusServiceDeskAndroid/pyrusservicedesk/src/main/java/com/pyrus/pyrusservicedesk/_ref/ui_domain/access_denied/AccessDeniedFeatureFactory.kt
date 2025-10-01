package com.pyrus.pyrusservicedesk._ref.ui_domain.access_denied

import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.ui_domain.access_denied.AccessFeatureContract.Effect
import com.pyrus.pyrusservicedesk._ref.ui_domain.access_denied.AccessFeatureContract.Message
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk._ref.utils.TextProvider
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Actor
import com.pyrus.pyrusservicedesk._ref.whitetea.core.StoreFactory
import com.pyrus.pyrusservicedesk._ref.whitetea.core.adaptCast
import com.pyrus.pyrusservicedesk._ref.whitetea.core.logic.Logic
import com.pyrus.pyrusservicedesk._ref.whitetea.utils.adapt
import com.pyrus.pyrusservicedesk.core.getUsers
import com.pyrus.pyrusservicedesk.sdk.AccessDeniedEventBus
import com.pyrus.pyrusservicedesk.sdk.repositories.AccountStore
import com.pyrus.pyrusservicedesk.sdk.repositories.LocalTicketsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow


internal class AccessDeniedFeatureFactory(
    private val storeFactory: StoreFactory,
    private val accountStore: AccountStore,
    private val accessDeniedEventBus: AccessDeniedEventBus,
    private val ticketsStore: LocalTicketsStore,
) {

    fun create(): AccessDeniedFeature = storeFactory.create(
        name = "AccessDeniedFeature",
        initialState = Unit,
        reducer = FeatureReducer(),
        actor = FeatureActor(
            accountStore = accountStore,
            accessDeniedEventBus = accessDeniedEventBus,
            ticketsStore = ticketsStore,
        ).adaptCast(),
        initialEffects = listOf(Effect.Inner.AccessDeniedFlow),
        effectAtOnceDelivery = true,
    ).adapt { it as? Effect.Outer }

}

private class FeatureReducer: Logic<Unit, Message, Effect>() {

    override fun Result.update(message: Message) {
        when (message) {
            is Message.Inner.AccessDenied -> {
                effects {
                    +Effect.Outer.ShowAccessDeniedDialog(
                        TextProvider.Format(
                            R.string.psd_no_access_message,
                            message.users.map { it.userName }
                        ),
                        message.usersIsEmpty
                    )
                }
            }

            is Message.Outer.OnDialogPositiveButtonClick -> {
                if (message.goBack) {
                    val multichatButtons = ConfigUtils.getMultichatButtons()
                    if (multichatButtons?.backButton != null) {
                        effects { +Effect.Outer.OpenBackwardScreen(multichatButtons.backButton) }
                    }
                    else {
                        effects { +Effect.Outer.CloseServiceDesk }
                    }
                }
            }
        }
    }

}

private class FeatureActor(
    private val accountStore: AccountStore,
    private val accessDeniedEventBus: AccessDeniedEventBus,
    private val ticketsStore: LocalTicketsStore,
) : Actor<Effect.Inner, Message.Inner> {

    override fun handleEffect(effect: Effect.Inner): Flow<Message.Inner> = when(effect) {
        Effect.Inner.AccessDeniedFlow -> flow {
            accessDeniedEventBus.events().collect { accessDeniedUsers ->
                val users = accountStore.getAccount().getUsers().filter { it !in accessDeniedUsers }
                if (users.isEmpty()) {
                    emit(Message.Inner.AccessDenied(accessDeniedUsers, true))
                    return@collect
                }

                val userIdsWithData = ticketsStore.getUsersWithData()
                val accessDeniedUsersWithoutCache = accessDeniedUsers.filter {
                    it !in userIdsWithData
                }
                if (accessDeniedUsersWithoutCache.isEmpty()) {
                    return@collect
                }

                emit(Message.Inner.AccessDenied(accessDeniedUsersWithoutCache, false))
            }
        }
    }
}