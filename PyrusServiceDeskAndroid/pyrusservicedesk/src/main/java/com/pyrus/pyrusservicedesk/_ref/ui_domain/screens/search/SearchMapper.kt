package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.search

import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.search.SearchContract.Message
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.search.SearchContract.State
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.tickets.TicketsMapper
import com.pyrus.pyrusservicedesk._ref.utils.TextProvider
import com.pyrus.pyrusservicedesk._ref.utils.textRes
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.cleanTags


internal object SearchMapper {

    fun map(event: SearchView.Event) : Message.Out? = when(event) {
        SearchView.Event.OnCloseClick -> Message.Out.OnCloseClick
        is SearchView.Event.OnSearchChanged -> null
        is SearchView.Event.OnTicketClick -> Message.Out.OnTicketClick(
            ticketId = event.ticketId,
            commentId = event.commentId,
            userId = event.userId
        )
    }

    fun mapSearch(event: SearchView.Event) : Message.Out? = when(event) {
        is SearchView.Event.OnSearchChanged -> Message.Out.OnSearchChanged(event.text)
        else -> null
    }

    fun map(effect: SearchContract.Effect.Out) : SearchView.Effect = when(effect) {
        SearchContract.Effect.Out.CloseKeyBoard -> SearchView.Effect.CloseKeyboard
        SearchContract.Effect.Out.Exit -> SearchView.Effect.Exit
        is SearchContract.Effect.Out.OpenTicket -> SearchView.Effect.OpenTicket(effect.ticketId, effect.commentId, effect.user)
    }

    fun map(state: State) : SearchView.Model {

        val query = state.requestSearchText
        val entries = state.suggestions.map { item ->

            val description: TextProvider? =
                item.commentInfo?.body?.cleanTags(" ")?.let { getDescription(query, it).textRes() }
                    ?: item.commentInfo?.let { TicketsMapper.getLastCommentText(it) }

            SearchResultEntry(
                ticketId = item.ticketId,
                commentId = item.commentId,
                userId = item.userId,
                title = item.title.cleanTags(" "),
                description = description,
                query = query,
                commentCreationTime = item.creationTime
            )
        }

        return SearchView.Model(
            searchResults = entries,
            search = state.currentSearchText,
            showEmptyPage = state.currentSearchText.isBlank() || entries.isEmpty(),
            emptyPageText = when {
                state.currentSearchText.isBlank() || query.isBlank() -> R.string.psd_search_by_tickets.textRes()
                entries.isEmpty() -> R.string.psd_no_suggestions.textRes()
                else -> null
            }
        )
    }

    private fun getDescription(query: String, rawDescription: String): String {
        return getTrimmedText(query, rawDescription, 40)
    }

    private fun getTrimmedText(query: String, rawDescription: String, maxCharsBefore: Int): String {
        val words = rawDescription.split(" ")

        var indexOfFirstWord = -1
        for (i in words.indices) {
            if (words[i].indexOf(query) == -1) continue
            else {
                indexOfFirstWord = i
                break
            }
        }

        val indexOfPrevWord = indexOfFirstWord - 1

        return when {
            indexOfFirstWord == -1 -> rawDescription
            indexOfPrevWord < 0 -> rawDescription
            else -> {
                var prevWordsLength = 0
                val prevWords = ArrayList<String>()
                var indexOfWord = -1
                for (i in indexOfPrevWord downTo 0) {
                    val word = words[i]
                    prevWordsLength += word.length
                    if (prevWordsLength > maxCharsBefore) {
                        if (prevWords.isEmpty()) {
                            prevWords += "…${word.takeLast(maxCharsBefore/2)}"
                        }
                        break
                    }
                    indexOfWord = i
                    prevWords += word
                }
                val text = (prevWords.reversed() + words.subList(indexOfFirstWord, words.size))
                    .joinToString(" ")
                if (indexOfWord > 0) "…$text"
                else text
            }
        }
    }

}