package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets

import com.github.terrakok.cicerone.Router
import com.github.terrakok.cicerone.androidx.ActivityScreen
import com.pyrus.pyrusservicedesk.SdConstants.PYRUS_BASE_DOMAIN
import com.pyrus.pyrusservicedesk.User
import com.pyrus.pyrusservicedesk.Vendor
import com.pyrus.pyrusservicedesk._ref.SdScreens
import com.pyrus.pyrusservicedesk._ref.data.multy_chat.TicketsInfo
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.ContentState
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.Effect
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.Message
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.State
import com.pyrus.pyrusservicedesk._ref.utils.AddUserEventBus
import com.pyrus.pyrusservicedesk._ref.utils.AudioWrapper
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk._ref.utils.RequestUtils
import com.pyrus.pyrusservicedesk._ref.utils.Try
import com.pyrus.pyrusservicedesk._ref.utils.isAudio
import com.pyrus.pyrusservicedesk._ref.utils.navigation.setFadeAnimation
import com.pyrus.pyrusservicedesk._ref.utils.navigation.setSlideRightAnimation
import com.pyrus.pyrusservicedesk._ref.utils.singleFlow
import com.pyrus.pyrusservicedesk._ref.whitetea.core.Actor
import com.pyrus.pyrusservicedesk._ref.whitetea.core.StoreFactory
import com.pyrus.pyrusservicedesk._ref.whitetea.core.adaptCast
import com.pyrus.pyrusservicedesk._ref.whitetea.core.logic.Logic
import com.pyrus.pyrusservicedesk._ref.whitetea.utils.adapt
import com.pyrus.pyrusservicedesk.core.Account
import com.pyrus.pyrusservicedesk.core.getExtraUsers
import com.pyrus.pyrusservicedesk.core.getUsers
import com.pyrus.pyrusservicedesk.sdk.repositories.AccountStore
import com.pyrus.pyrusservicedesk.sdk.repositories.LocalCommandsStore
import com.pyrus.pyrusservicedesk.sdk.repositories.SdRepository
import com.pyrus.pyrusservicedesk.sdk.repositories.UserInternal
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

private const val TAG = "TicketsListFeature"

internal class TicketsFeatureFactory(
    private val storeFactory: StoreFactory,
    private val repository: SdRepository,
    private val router: Router,
    private val commandsStore: LocalCommandsStore,
    private val addUserEventBus: AddUserEventBus,
    private val audioWrapper: AudioWrapper,
    private val accountStore: AccountStore,
) {

    fun create(): TicketsFeature = storeFactory.create(
        name = TAG,
        initialState = State(ContentState.Loading),
        reducer = FeatureReducer(),
        actor = TicketsActor(
            repository = repository,
            router = router,
            commandsStore = commandsStore,
            addUserEventBus = addUserEventBus,
            audioWrapper = audioWrapper,
            accountStore = accountStore,
        ).adaptCast(),
        initialEffects = listOf(
            Effect.Inner.UpdateTickets(force = false),
            Effect.Inner.TicketsSetFlow,
            Effect.Inner.AddUserEventFlow,
            Effect.Inner.UpdateAudioData,
            Effect.Inner.CheckExtraUsers,
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
                val users = contentState.users.filter { it.appId == contentState.pageAppId }

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
                val users = contentState.users
                val filter = contentState.filter?.userId
                effects { +Effect.Outer.ShowFilterMenu(selectedAppId, filter, users) }
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
                        closedTicketsIsExpanded = false,
                    ))
                }
            }
            is Message.Outer.OnCreateTicketClick -> {
                val contentState = state.contentState as? ContentState.Content ?: return
                val appId = contentState.pageAppId ?: return

                val users = contentState.users

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
                val user = contentState.users.find { it.userId == message.userId }
                val users = contentState.users.filter { it.appId == user?.appId }
                val filter = if (user == null || users.size <= 1) null else user
                val pageAppId = user?.appId ?: contentState.pageAppId

                if (filter != null && contentState.filter != filter) {
                    effects { +Effect.Outer.ScrollUp }
                }

                state {
                    state.copy(
                        contentState = contentState.copy(filter = filter, pageAppId = pageAppId)
                    )
                }
            }

            is Message.Outer.OnDialogPositiveButtonClick -> {
                if (message.goBack) {
                    val multichatButtons = ConfigUtils.getMultichatButtons()
                    if (multichatButtons?.backButton != null) {
                        try {
                            message.activity.startActivity(multichatButtons.backButton)
                        } catch (e: Exception) {
                            // TODO show error ui
                        }
                    }
                }
                else {
                    val contentState = state.contentState as? ContentState.Content ?: return
                    state {
                        state.copy(
                            contentState = contentState.copy(
                                filter = null,
                                pageAppId = contentState.users.firstOrNull()?.appId
                            )
                        )
                    }
                }
            }
            is Message.Outer.OnClosedTicketsTitleCLick -> {
                val contentState = state.contentState as? ContentState.Content ?: return
                effects { +Effect.Outer.ScrollToClosedHeader }
                state {
                    state.copy(contentState = contentState.copy(closedTicketsIsExpanded = !contentState.closedTicketsIsExpanded))
                }
            }

            Message.Outer.OnRightButtonClick -> effects { +Effect.Inner.OpenRightButtonScreen }
            Message.Outer.OnSearchClick -> effects { +Effect.Inner.OpenSearchScreen }
        }
    }

    private fun Result.handleInner(message: Message.Inner) {
        when (message) {
            is Message.Inner.UpdateTicketsCompleted -> {

                val users = message.ticketsInfo.users
                val pendingFilter = message.pendingFilter?.let { preparePendingFilter(it, users) }

                val contentState = when (val currentDataState = state.contentState) {
                    is ContentState.Content -> {
                        val users = message.ticketsInfo.users

                        val tabId = message.pendingFilter?.appId ?: validateTabId(users, currentDataState.pageAppId)
                        val filter = pendingFilter ?: validateFilter(tabId, users, currentDataState.filter)

                        if (filter != null && currentDataState.filter != filter) {
                            effects { +Effect.Outer.ScrollUp }
                        }

                        currentDataState.copy(
                            users = users,
                            ticketSets = message.ticketsInfo.ticketSetInfoList,
                            isUserTriggerLoading = false,
                            userWithData = message.ticketsInfo.usersWithData,
                            filter = filter,
                            pageAppId = tabId,
                        )
                    }

                    is ContentState.Error,
                    is ContentState.Loading -> createInitialContentState(
                        ticketsInfo = message.ticketsInfo,
                        pendingFilter = pendingFilter,
                        pendingTabId = message.pendingFilter?.appId
                    )
                }

                state { state.copy(contentState = contentState) }
            }
            is Message.Inner.UpdateTicketsFailed -> {
                when(val currentDataState = state.contentState) {
                    is ContentState.Content -> state {
                        state.copy(contentState = currentDataState.copy(isUserTriggerLoading = false))
                    }
                    is ContentState.Error -> {}
                    is ContentState.Loading -> state { state.copy(contentState = ContentState.Error) }
                }
            }

            is Message.Inner.OpenTicket -> effects { +Effect.Outer.OpenTicket(message.ticketId, message.commentId, message.user) }
        }
    }

    private fun preparePendingFilter(filter: User, users: List<User>): User? {
        val selectedAppUsers = users.groupBy { it.appId }
        val currentUsers = selectedAppUsers[filter.appId]
        return if (currentUsers != null && currentUsers.size > 1) filter else null
    }

    private fun validateFilter(tabId: String?, users: List<User>, filter: User?): User? {
        return if (filterIsValid(tabId, filter, users)) filter else null
    }

    private fun filterIsValid(tabId: String?, filter: User?, users: List<User>): Boolean {
        if (filter == null ) return true
        if (tabId != filter.appId) return false
        if (users.find { it.userId == filter.userId } == null) return false
        if (users.count { it.appId == filter.appId } <= 1) return false
        return true
    }

    private fun validateTabId(users: List<User>, currentAppTabId: String?): String? {
        val currentTabIsValid = currentAppTabId == null || users.find { it.appId == currentAppTabId } != null
        return if (currentTabIsValid) currentAppTabId else users.firstOrNull()?.appId
    }

    private fun createInitialContentState(
        ticketsInfo: TicketsInfo,
        pendingFilter: User?,
        pendingTabId: String?
    ) : ContentState.Content {

        val users = ticketsInfo.users

        val filter =
            if (pendingFilter != null && users.find { it.userId == pendingFilter.userId  } == null) null
            else pendingFilter

        val pageAppId =
            if (pendingTabId != null && users.find { it.appId == pendingTabId } == null) null
            else pendingTabId

        return ContentState.Content(
            users = ticketsInfo.users,
            pageAppId = pageAppId ?: ticketsInfo.ticketSetInfoList.firstOrNull()?.appId,
            filter = filter,
            ticketSets = ticketsInfo.ticketSetInfoList,
            isUserTriggerLoading = false,
            userWithData = ticketsInfo.usersWithData,
            closedTicketsIsExpanded = false,
        )
    }

}

@OptIn(FlowPreview::class)
internal class TicketsActor(
    private val repository: SdRepository,
    private val router: Router,
    private val commandsStore: LocalCommandsStore,
    private val addUserEventBus: AddUserEventBus,
    private val audioWrapper: AudioWrapper,
    private val accountStore: AccountStore,
): Actor<Effect.Inner, Message.Inner> {


    private suspend fun sync(force: Boolean): Message.Inner {
        return when(val ticketsTry = repository.getTicketsInfo(force)) {
            is Try.Success -> {
                val pendingFilter = addUserEventBus.getAndRemovePendingFilter()
                val ticketsInfo = ticketsTry.value

                if (pendingFilter != null && pendingFilter in ticketsInfo.users) {
                    Message.Inner.UpdateTicketsCompleted(ticketsInfo, pendingFilter)
                }
                else {
                    Message.Inner.UpdateTicketsCompleted(ticketsInfo, null)
                }

            }
            is Try.Failure -> {
                ticketsTry.error.printStackTrace()
                Message.Inner.UpdateTicketsFailed
            }
        }
    }


    override fun handleEffect(effect: Effect.Inner): Flow<Message.Inner> = when(effect) {
        is Effect.Inner.UpdateTickets -> singleFlow {
            sync(effect.force)
        }

        is Effect.Inner.TicketsSetFlow -> {
            repository.getTicketsInfoFlow().map {
                val pendingFilter = addUserEventBus.getAndRemovePendingFilter()

                if (pendingFilter != null && pendingFilter in it.users) {
                    Message.Inner.UpdateTicketsCompleted(it, pendingFilter)
                }
                else {
                    Message.Inner.UpdateTicketsCompleted(it, null)
                }
            }
        }

        is Effect.Inner.AddUserEventFlow -> addUserEventBus.events().map { user ->
            sync(false)
        }

        is Effect.Inner.OpenTicketScreen -> singleFlow{
            val ticketId = effect.ticketId ?: commandsStore.getNextLocalId()
            Message.Inner.OpenTicket(ticketId, null, effect.user)
        }
        is Effect.Inner.Close -> flow { router.exit() }
        Effect.Inner.OpenRightButtonScreen -> flow {

            val applications = repository.getApplications()
            val vendors = applications.map {
                Vendor(
                    appId = it.appId,
                    orgName = it.orgName ?: "",
                    orgUrl = it.orgLogoUrl?.let { url -> RequestUtils.getOrganisationLogoUrl(url, PYRUS_BASE_DOMAIN) },
                    orgDescription = it.orgDescription
                )
            }.toTypedArray()

            val rightButtonAction = ConfigUtils.getMultichatButtons()?.rightButtonAction
            if (rightButtonAction != null) {
                router.navigateTo(ActivityScreen {
                    rightButtonAction.putExtra(Vendor.KEY_VENDORS, vendors)
                    rightButtonAction
                })
            }
        }
        is Effect.Inner.OpenSearchScreen -> flow {
            router.navigateTo(SdScreens.SearchScreen().setFadeAnimation())
        }

        is Effect.Inner.UpdateAudioData -> flow {
            val tickets1 = repository.getTicketsWithComments()
            val account = accountStore.getAccount()
            val uriList = tickets1.flatMap { tickets ->
                tickets.comments.flatMap { comments ->
                    comments.attachments.filter { it.name.isAudio() }.map { RequestUtils.getFileUrl(it.id, account, findUser(account, tickets.ticket.userId)) }
                }
            }
            audioWrapper.setAudioDurations(uriList)
        }

        is Effect.Inner.CheckExtraUsers -> flow {
            accountStore.accountStateFlow().collect { account ->
                val extraUsers = account.getExtraUsers()
                if (extraUsers.isNotEmpty()) {
                    repository.getTicketsInfo(true)
                    accountStore.cleanExtraUsers()
                }
            }
        }
    }

    private fun findUser(account: Account, userId: String): User? {
        return account.getUsers().find { it.userId == userId }
    }

}
