package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets

import com.github.terrakok.cicerone.Router
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.Screens
import com.pyrus.pyrusservicedesk._ref.data.multy_chat.TicketsInfo
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.ContentState
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.Effect
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.Message
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.State
import com.pyrus.pyrusservicedesk._ref.utils.AddUserEventBus
import com.pyrus.pyrusservicedesk._ref.utils.TextProvider
import com.pyrus.pyrusservicedesk._ref.utils.Try
import com.pyrus.pyrusservicedesk._ref.utils.isSuccess
import com.pyrus.pyrusservicedesk._ref.utils.navigation.setSlideRightAnimation
import com.pyrus.pyrusservicedesk._ref.utils.singleFlow
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Actor
import com.pyrus.pyrusservicedesk._ref.whitetea.core.StoreFactory
import com.pyrus.pyrusservicedesk._ref.whitetea.core.adaptCast
import com.pyrus.pyrusservicedesk._ref.whitetea.core.logic.Logic
import com.pyrus.pyrusservicedesk._ref.whitetea.utils.adapt
import com.pyrus.pyrusservicedesk.core.getUsers
import com.pyrus.pyrusservicedesk.sdk.repositories.LocalCommandsStore
import com.pyrus.pyrusservicedesk.sdk.repositories.Repository
import com.pyrus.pyrusservicedesk.sdk.repositories.UserInternal
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

private const val TAG = "TicketsListFeature"

internal class TicketsFeatureFactory(
    private val storeFactory: StoreFactory,
    private val repository: Repository,
    private val router: Router,
    private val commandsStore: LocalCommandsStore,
    private val addUserEventBus: AddUserEventBus,
) {

    fun create(): TicketsFeature = storeFactory.create(
        name = TAG,
        initialState = State(ContentState.Loading),
        reducer = FeatureReducer(),
        actor = TicketsActor(
            repository = repository,
            router = router,
            commandsStore = commandsStore,
            addUserEventBus = addUserEventBus
        ).adaptCast(),
        initialEffects = listOf(
            Effect.Inner.UpdateTickets(false),
            Effect.Inner.TicketsSetFlow,
            Effect.Inner.EventsFlow,
        ),
    ).adapt { it as? Effect.Outer }

}

private class FeatureReducer: Logic<State, Message, Effect>() {

    override fun Result.update(message: Message) {
        when(message) {
            is Message.Outer -> handleOuter(message)
            is Message.Inner -> handleInner(message)
        }
    }

    private fun Result.handleOuter(message: Message.Outer) {
        when (message) {
            is Message.Outer.OnFabItemClick -> {
                val contentState = state.contentState as? ContentState.Content ?: return
                val selectedAppId = contentState.pageAppId ?: return
                val users = contentState.account.getUsers().filter { it.appId == contentState.pageAppId }

                val selectedUser = contentState.filter

                if (users.size > 1 && selectedUser == null) {
                    effects { +Effect.Outer.ShowAddTicketMenu(selectedAppId, users) }
                }
                else if (users.size > 1 && selectedUser != null) {
                    val user = UserInternal(selectedUser.userId, selectedAppId)
                    effects { +Effect.Inner.OpenTicketScreen(user, null) }
                }
                else {
                    val firstUser = users.firstOrNull() ?: return
                    val user = UserInternal(firstUser.userId, selectedAppId)
                    effects { +Effect.Inner.OpenTicketScreen(user, null) }
                }
            }
            is Message.Outer.OnFilterClick -> {
                val contentState = state.contentState as? ContentState.Content ?: return
                val selectedAppId = contentState.pageAppId ?: return
                val users = contentState.account.getUsers()
                effects { +Effect.Outer.ShowFilterMenu(selectedAppId, message.selectedUserId, users) }
            }
            is Message.Outer.OnRetryClick -> {
                if (state.contentState !is ContentState.Error) return
                state { state.copy(contentState = ContentState.Loading) }
                effects { +Effect.Inner.UpdateTickets(force = true) }
            }
            is Message.Outer.OnRefresh -> {
                val contentState = state.contentState as? ContentState.Content ?: return
                state { state.copy(contentState = contentState.copy(isUserTriggerLoading = true)) }
                effects { +Effect.Inner.UpdateTickets(force = true) }
            }
            is Message.Outer.OnChangePage -> {
                val contentState = state.contentState as? ContentState.Content ?: return
                val currentFilter = contentState.filter
                val filter = if (currentFilter?.appId != message.appId) null else currentFilter

                state {
                    state.copy(contentState = contentState.copy(
                        pageAppId = message.appId,
                        filter = filter,
                    ))
                }
            }
            is Message.Outer.OnCreateTicketClick -> {
                val contentState = state.contentState as? ContentState.Content ?: return
                val appId = contentState.pageAppId ?: return

                val users = contentState.account.getUsers()

                val firstUser = users.first()

                if (users.size > 1) effects {
                    +Effect.Outer.ShowAddTicketMenu(appId, users)
                }
                else effects {
                    +Effect.Inner.OpenTicketScreen(
                        user = UserInternal(firstUser.userId, firstUser.appId),
                        ticketId = null,
                    )
                }
            }
            is Message.Outer.OnTicketClick -> {
                val contentState = state.contentState as? ContentState.Content ?: return
                val appId = contentState.pageAppId ?: return
                effects {
                    val user = UserInternal(message.userId, appId)
                    +Effect.Inner.OpenTicketScreen(user, message.ticketId)
                }
            }
            is Message.Outer.OnFilterSelected -> {
                val contentState = state.contentState as? ContentState.Content ?: return
                val user = contentState.account.getUsers().find { it.userId == message.userId }
                state {
                    state.copy(contentState = contentState.copy(filter = user))
                }
            }

            is Message.Outer.OnUsersIsEmpty -> effects { +Effect.Inner.Close }
        }
    }

    private fun Result.handleInner(message: Message.Inner) {
        when (message) {
            is Message.Inner.UpdateTicketsCompleted -> state {
                state.copy(contentState = when(val currentDateState = state.contentState) {
                    is ContentState.Content -> currentDateState.copy(
                        ticketSets = message.ticketsInfo.ticketSetInfoList,
                        isUserTriggerLoading = false,
                        loadUserIds = emptySet()
                    )
                    is ContentState.Error,
                    is ContentState.Loading -> createInitialContentState(message.ticketsInfo)
                })
            }
            is Message.Inner.UpdateTicketsFailed -> {
                when(state.contentState) {
                    is ContentState.Content -> {}
                    is ContentState.Error -> {}
                    is ContentState.Loading -> state { state.copy(contentState = ContentState.Error) }
                }
            }
            is Message.Inner.TicketsUpdated -> {
                val contentState = state.contentState as? ContentState.Content ?: return

                val fullUsers = message.ticketsInfo.account.getUsers()
                val currentUsers = contentState.account.getUsers().map { UserInternal(it.userId, it.appId) }.toSet()
                val newUsers = fullUsers.map { UserInternal(it.userId, it.appId) }.toSet()
                val diff = newUsers.minus(currentUsers)

                val authorAccessDenied = message.ticketsInfo.authorAccessDenied
                val firstCurrentUser = contentState.account.getUsers().firstOrNull()
                if (currentUsers.size <= 1 && fullUsers.size <= 1 && authorAccessDenied?.find { it == firstCurrentUser?.userId } != null) {
                    effects {
                        +Effect.Outer.ShowDialog(
                            message = TextProvider.Format(
                                R.string.psd_no_access_message,
                                listOf(firstCurrentUser?.userName ?: "")
                            ),
                            usersIsEmpty = true
                        )
                    }
                }

                val lastNewUser = diff.lastOrNull()?.let { fullUsers.find { fu -> fu.userId == it.userId} }

                val filter = when {
                    newUsers.find { it.userId == contentState.filter?.userId } == null -> null
                    lastNewUser == null -> contentState.filter
                    newUsers.count { it.appId == lastNewUser.appId } > 1 -> lastNewUser
                    else -> null
                }

                val pageAppId = (newUsers.find { it.appId == contentState.pageAppId } ?: newUsers.firstOrNull())?.appId

                state { state.copy(contentState = contentState.copy(
                    account = message.ticketsInfo.account,
                    ticketSets = message.ticketsInfo.ticketSetInfoList,
                    isUserTriggerLoading = false,
                    filter = filter,
                    pageAppId = lastNewUser?.appId ?: pageAppId,
                    loadUserIds = contentState.loadUserIds.toMutableSet()
                        .apply { lastNewUser?.let { add(it.userId) } }
                )) }

                if (diff.isNotEmpty()) {
                    effects { +Effect.Inner.UpdateTickets(true) }
                }
            }

            is Message.Inner.OnDialogAccessDenied -> {
                effects {
                    +Effect.Outer.ShowDialog(
                        message.message,
                        message.usersIsEmpty
                    )
                }
            }
        }
    }

    private fun createInitialContentState(ticketsInfo: TicketsInfo) : ContentState.Content {
        val firstSet = ticketsInfo.ticketSetInfoList.firstOrNull()
        return ContentState.Content(
            account = ticketsInfo.account,
            pageAppId = firstSet?.appId,
            filter = null,
            ticketSets = ticketsInfo.ticketSetInfoList,
            isUserTriggerLoading = false,
            loadUserIds = emptySet()
        )
    }

}

@OptIn(FlowPreview::class)
internal class TicketsActor(
    private val repository: Repository,
    private val router: Router,
    private val commandsStore: LocalCommandsStore,
    private val addUserEventBus: AddUserEventBus,
): Actor<Effect.Inner, Message.Inner> {

    override fun handleEffect(effect: Effect.Inner): Flow<Message.Inner> = when(effect) {
        is Effect.Inner.UpdateTickets -> singleFlow {
            when(val ticketsTry = repository.getTicketsInfo(effect.force)) {
                is Try.Success -> {
                    Message.Inner.UpdateTicketsCompleted(ticketsTry.value)
                }
                is Try.Failure -> {
                    ticketsTry.error.printStackTrace()
                    Message.Inner.UpdateTicketsFailed
                }
            }
        }

        is Effect.Inner.EventsFlow -> flow {
            addUserEventBus.events().collect { user ->
                val ticketsTry = repository.getTicketsInfo(true)
                if (ticketsTry.isSuccess()) {

                    // TODO kate так себе вариант лучше в ответе добавить список заблокированных пользователей
                    // А что если ответ !isSuccess? (нужно узнать логику для этого кейса)
                    val userIsAccessDenied = ticketsTry.value.account.getUsers()
                        .find { it.userId == user.userId }

                    if (userIsAccessDenied == null) {
                        // TODO kate логику с фильтром стоит перенести сюда
                        emit(
                            Message.Inner.OnDialogAccessDenied(
                                // TODO kate Текст нам тут не нужен (TextProvider лучше вообще как можно меньше хранить на слое логики)
                                // поведение "если нет пользоваьеля – значит он заблокирован" выглядит неочевидо
                                message = TextProvider.Format(
                                    R.string.psd_no_access_message,
                                    listOf(user.userName)
                                ),
                                // тут мы перемещаем логику в reducer (зачем?) можно и тут обработать
                                usersIsEmpty = ticketsTry.value.account.getUsers().isEmpty()
                            )
                        )
                    }
                }
            }
        }

        is Effect.Inner.TicketsSetFlow -> {
            repository.getTicketsInfoFlow()
                .debounce(150)
                .map { Message.Inner.TicketsUpdated(it) }
        }
        is Effect.Inner.OpenTicketScreen -> flow {
            val ticketId = effect.ticketId ?: commandsStore.getNextLocalId()
            router.navigateTo(Screens.TicketScreen(ticketId, effect.user).setSlideRightAnimation())
        }

        is Effect.Inner.Close -> flow { router.exit() }
    }

}
