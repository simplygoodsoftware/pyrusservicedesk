package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets

import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.User
import com.pyrus.pyrusservicedesk._ref.data.TicketHeader
import com.pyrus.pyrusservicedesk._ref.data.multy_chat.TicketSetInfo
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.ContentState
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsView.Model
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsView.Model.TabEntry
import com.pyrus.pyrusservicedesk._ref.utils.TextProvider
import com.pyrus.pyrusservicedesk._ref.utils.isAudio
import com.pyrus.pyrusservicedesk._ref.utils.isImage
import com.pyrus.pyrusservicedesk._ref.utils.isVideo
import com.pyrus.pyrusservicedesk._ref.utils.plus
import com.pyrus.pyrusservicedesk._ref.utils.textRes
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.cleanTags

internal object TicketsMapper {

    fun map(state: ContentState): Model = when(state) {
        is ContentState.Content -> {

            val tabs = state.ticketSets
                ?.map { map(it, it.appId == state.pageAppId) }
                ?: emptyList()

            val ticketHeaders = state.ticketSets?.firstOrNull { it.appId == state.pageAppId }

            val filter = state.filter
            val tabId = state.pageAppId

            val idsWithData = state.userWithData.map { it.userId }.toSet()
            val appIdsWithData = state.userWithData.map { it.appId }.toSet()

            val userSetWithData = when {
                filter != null && tabId != null -> filter.userId in idsWithData && tabId in appIdsWithData
                filter != null -> filter.userId in idsWithData
                tabId != null -> tabId in appIdsWithData
                else -> false
            }

            val tickets: List<Model.TicketsEntry> =
                if (!userSetWithData) emptyList()
                else ticketHeaders
                    ?.let { mapToTicketEntries(it, state.closedTicketsIsExpanded, state.filter) }
                    ?: emptyList()

            val ticketsSetByAppName = state.ticketSets?.takeIf { it.isNotEmpty() }?.associateBy { it.appId }
            val titleUrl = ticketsSetByAppName?.get(state.pageAppId)?.orgLogoUrl
            val titleText = when {
                ticketsSetByAppName == null -> null
                ticketsSetByAppName.size == 1 -> ticketsSetByAppName[state.pageAppId]?.orgName?.cleanTags()?.textRes()
                else -> R.string.all_conversations.textRes()
            }

            val showFilter = state.pageAppId != null && state.users.filter { it.appId == state.pageAppId }.size > 1

            val tabLayoutIsVisible = state.users.groupBy { it.appId }.size > 1

            Model(
                titleText = titleText,
                titleImageUrl = titleUrl,
                tabLayoutIsVisible = tabLayoutIsVisible,
                tickets = tickets,
                appTabs = tabs,
                showNoConnectionError = false,
                isLoading = !userSetWithData,
                isUserTriggerLoading = state.isUserTriggerLoading,
                showCreateTicketPicture = userSetWithData && tickets.isEmpty(),
            )
        }
        ContentState.Error -> Model(
            titleText = null,
            titleImageUrl = null,
            tabLayoutIsVisible = false,
            tickets = emptyList(),
            appTabs = emptyList(),
            showNoConnectionError = true,
            isLoading = false,
            isUserTriggerLoading = false,
            showCreateTicketPicture = false,
        )
        ContentState.Loading -> Model(
            titleText = null,
            titleImageUrl = null,
            tabLayoutIsVisible = false,
            tickets = emptyList(),
            appTabs = emptyList(),
            showNoConnectionError = false,
            isLoading = true,
            isUserTriggerLoading = false,
            showCreateTicketPicture = false,
        )
    }

    private fun map(ticketSetInfo: TicketSetInfo, isSelected: Boolean): TabEntry = TabEntry(
        appId = ticketSetInfo.appId,
        titleText = ticketSetInfo.orgName,
        isSelected = isSelected,
    )

    private fun mapToTicketEntries(
        ticketSetInfo: TicketSetInfo,
        closedTicketsIsExpanded: Boolean,
        filter: User?,
    ): List<Model.TicketsEntry> {

        val filteredTickets = when {
            filter == null || filter.appId != ticketSetInfo.appId -> ticketSetInfo.tickets
            else -> ticketSetInfo.tickets.filter { it.userId == filter.userId }
        }
        val ticketsList = filteredTickets.filter { it.isActive }.map(::map).toMutableList()
        val closedTicketsList = filteredTickets.filter { !it.isActive }.map(::map)
        if (closedTicketsList.isNotEmpty()) {
            ticketsList.add(Model.TicketsEntry.ClosedTicketTitleEntry(
                isExpanded = closedTicketsIsExpanded,
                count = closedTicketsList.size,
            ))
        }
        if (closedTicketsIsExpanded) {
            ticketsList.addAll(closedTicketsList)
        }
        return ticketsList
    }

    private fun map(header: TicketHeader): Model.TicketsEntry {

        val title: TextProvider = if (header.subject.isNullOrBlank()) R.string.new_ticket.textRes()
        else header.subject.cleanTags().textRes()

        return Model.TicketsEntry.TicketHeaderEntry(
            ticketId = header.ticketId,
            userId = header.userId,
            title = title,
            lastCommentText = header.lastComment?.let(::getLastCommentText),
            lastCommentIconRes = header.lastComment?.lastAttachmentName.let(::getLastCommentIconRes),
            lastCommentCreationTime = header.lastComment?.creationDate,
            isRead = header.isRead,
            isLoading = header.isLoading,
        )
    }

    fun getLastCommentIconRes(lastCommentName: String?): Int? {
        return when {
            lastCommentName != null && lastCommentName.isAudio() -> R.drawable.ic_audio
            lastCommentName != null -> R.drawable.psd_ic_attachment
            else -> null
        }
    }

    fun getLastCommentText(lastComment: TicketHeader.LastComment): TextProvider? {
        val authorName = when {
            lastComment.author?.isUser == true -> R.string.psd_you.textRes()
            lastComment.author?.name != null -> lastComment.author.name.cleanTags(" ").textRes()
            else -> null
        }
        return getLastCommentText(authorName, lastComment.lastAttachmentName, lastComment.body)
    }

    private fun getLastCommentText(author: TextProvider?, lastAttach: String?, body: String?): TextProvider? {
        val cleanText = body?.cleanTags(" ")

        val divider = ": ".textRes()
        val authorAndDivider = if (author == null) "".textRes() else author + divider
        val paperclip = "[icon] "//"\uD83D\uDCCE ".textRes() // 📎
        return when {
            lastAttach != null -> authorAndDivider + paperclip + getAttachType(lastAttach)
            !cleanText.isNullOrBlank() -> authorAndDivider + cleanText
            else -> null
        }
    }

    private fun getAttachType(name: String): TextProvider = when {
        name.isImage() -> R.string.psd_type_photo.textRes()
        name.isVideo() -> R.string.psd_type_video.textRes()
        name.isAudio() -> R.string.psd_type_audio.textRes()
        else -> R.string.psd_type_file.textRes()
    }

}
