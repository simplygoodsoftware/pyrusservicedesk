package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets

import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.User
import com.pyrus.pyrusservicedesk._ref.data.TicketHeader
import com.pyrus.pyrusservicedesk._ref.data.multy_chat.TicketSetInfo
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsContract.ContentState
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsView.Model
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsView.Model.TicketSetInfoEntry
import com.pyrus.pyrusservicedesk._ref.utils.RequestUtils.getOrganisationLogoUrl
import com.pyrus.pyrusservicedesk._ref.utils.TextProvider
import com.pyrus.pyrusservicedesk._ref.utils.isImage
import com.pyrus.pyrusservicedesk._ref.utils.isVideo
import com.pyrus.pyrusservicedesk._ref.utils.plus
import com.pyrus.pyrusservicedesk._ref.utils.textRes
import com.pyrus.pyrusservicedesk.core.getUsers
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.HtmlTagUtils

internal object TicketsMapper {

    fun map(state: ContentState): Model = when(state) {
        is ContentState.Content -> {

            val ticketSet = state.ticketSets?.map {
                map(it, state.isUserTriggerLoading, state.loadUserIds, state.filter)
            } ?: emptyList()

            var page = 0
            val ticketSets = state.ticketSets ?: emptyList()
            for (i in ticketSets.indices) {
                if(ticketSets[i].appId == state.pageAppId) {
                    page = i
                    break
                }
            }

            val ticketsSetByAppName = state.ticketSets?.associateBy { it.appId }
            val titleUrl = ticketsSetByAppName?.get(state.pageAppId)?.orgLogoUrl?.let {
                getOrganisationLogoUrl(it, state.account.domain)
            }
            val titleText = ticketsSetByAppName?.get(state.pageAppId)?.orgName

            val showFilter = state.pageAppId != null && state.account.getUsers().filter { it.appId == state.pageAppId }.size > 1

            Model(
                titleText = titleText,
                titleImageUrl = titleUrl,
                showFilter = showFilter,
                filterName = state.filter?.userName,
                ticketsIsEmpty = state.ticketSets.isNullOrEmpty(),
                filterEnabled = state.filter != null,
                tabLayoutIsVisible = state.account.getUsers().size > 1,
                ticketSetInfo = Model.TicketSetInfo(page, ticketSet),
                showNoConnectionError = false,
                isLoading = false,
            )
        }
        ContentState.Error -> Model(
            titleText = null,
            titleImageUrl = null,
            filterName = null,
            showFilter = false,
            ticketsIsEmpty = true,
            filterEnabled = false,
            tabLayoutIsVisible = false,
            ticketSetInfo = null,
            showNoConnectionError = true,
            isLoading = false,
        )
        ContentState.Loading -> Model(
            titleText = null,
            titleImageUrl = null,
            showFilter = false,
            filterName = null,
            ticketsIsEmpty = true,
            filterEnabled = false,
            tabLayoutIsVisible = false,
            ticketSetInfo = null,
            showNoConnectionError = false,
            isLoading = true,
        )
    }

    private fun map(
        ticketSetInfo: TicketSetInfo,
        isUserTriggerLoading: Boolean,
        loadUserIds: Set<String>,
        filter: User?,
    ): TicketSetInfoEntry {

        val isLoading =
            ticketSetInfo.userIds.size == 1 && ticketSetInfo.userIds.first() in loadUserIds
                || filter != null && filter.appId == ticketSetInfo.appId && filter.userId in loadUserIds

        val filteredTickets = when {
            isLoading -> emptyList()
            filter == null || filter.appId != ticketSetInfo.appId -> ticketSetInfo.tickets
            else -> ticketSetInfo.tickets.filter { it.userId == filter.userId }
        }

        return TicketSetInfoEntry(
            appId = ticketSetInfo.appId,
            titleText = ticketSetInfo.orgName,
            tickets = filteredTickets.map(::map),
            isUserTriggerLoading = isUserTriggerLoading,
            isLoading = isLoading
        )
    }

    private fun map(header: TicketHeader): Model.TicketHeaderEntry = Model.TicketHeaderEntry(
        ticketId = header.ticketId,
        userId = header.userId,
        title = header.subject?.let(HtmlTagUtils::cleanTags),
        lastCommentText = header.lastComment?.let(::getLastCommentText),
        lastCommentCreationTime = header.lastComment?.creationDate,
        isRead = header.isRead,
        isLoading = header.isLoading,
    )

    private fun getLastCommentText(lastComment: TicketHeader.LastComment): TextProvider? {
        val authorName = when {
            lastComment.author?.isUser == true -> R.string.psd_you.textRes()
            else -> (lastComment.author?.name ?: "").textRes()
        }
        return getLastCommentText(authorName, lastComment.lastAttachmentName, lastComment.body)
    }

    private fun getLastCommentText(author: TextProvider, lastAttach: String?, body: String?): TextProvider? {
        val cleanText = body?.let(HtmlTagUtils::cleanTags)

        val divider = ": ".textRes()
        val paperclip = "\uD83D\uDCCE ".textRes() // 📎
        return when {
            lastAttach != null -> author + divider + paperclip + getAttachType(lastAttach)
            !cleanText.isNullOrBlank() -> author + divider + cleanText
            else -> null
        }
    }

    private fun getAttachType(name: String): TextProvider = when {
        name.isImage() -> R.string.psd_type_photo.textRes()
        name.isVideo() -> R.string.psd_type_video.textRes()
        else -> R.string.psd_type_file.textRes()
    }

}
