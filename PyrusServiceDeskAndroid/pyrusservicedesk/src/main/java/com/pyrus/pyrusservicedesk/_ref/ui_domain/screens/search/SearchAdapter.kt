package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.search

import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.tickets_list.adapters.setTimeoutClickListener
import com.pyrus.pyrusservicedesk._ref.utils.getTimeWhen
import com.pyrus.pyrusservicedesk._ref.utils.text
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.ViewHolderBase
import java.util.Calendar
import java.util.regex.Matcher
import java.util.regex.Pattern


/**
 * Adapter that is used for rendering comment feed of the ticket screen.
 */
internal class SearchAdapter(
    private val onClick: (ticketId: Long, commentId: Long?, userId: String) -> Unit,
) : ListAdapter<SearchResultEntry, ViewHolderBase<SearchResultEntry>>(SuggestionItemCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderBase<SearchResultEntry> =
        SuggestionViewHolder(parent, onClick)

    override fun onBindViewHolder(holder: ViewHolderBase<SearchResultEntry>, position: Int) {
        holder.bindItem(getItem(position))
    }

    private class SuggestionItemCallback : DiffUtil.ItemCallback<SearchResultEntry>() {

        override fun areItemsTheSame(
            oldItem: SearchResultEntry,
            newItem: SearchResultEntry,
        ): Boolean = oldItem.ticketId == newItem.ticketId

        override fun areContentsTheSame(
            oldItem: SearchResultEntry,
            newItem: SearchResultEntry,
        ): Boolean = oldItem == newItem

    }

}

internal class SuggestionViewHolder(
    parent: ViewGroup,
    onClick: (ticketId: Long, commentId: Long?, userId: String) -> Unit,
) : ViewHolderBase<SearchResultEntry>(parent, R.layout.psd_suggestion_item) {

    private val ticketName = itemView.findViewById<TextView>(R.id.ticket_title_tv)
    private val date = itemView.findViewById<TextView>(R.id.ticket_time_tv)
    private val lastComment = itemView.findViewById<TextView>(R.id.ticket_comment_tv)

    init {
        itemView.setTimeoutClickListener {
            val ticketId =
                getItem().ticketId
            val userId = getItem().userId
            val commentId = getItem().commentId

            onClick(ticketId, commentId, userId)
        }
    }

    override fun bindItem(entry: SearchResultEntry) {
        super.bindItem(entry)

        ticketName.text = selectQueryText(entry.title, entry.query)

        val commentText = entry.description?.text(itemView.context)?.let { selectQueryText(it, entry.query) }
        lastComment.text = commentText

        val lastCommentCreationTime = entry.commentCreationTime
        val dateText = lastCommentCreationTime?.getTimeWhen(itemView.context, Calendar.getInstance())
        date.text = dateText
    }

    private fun selectQueryText(text: String, query: String): SpannableString {
        val spannableString = SpannableString(text)
        val pattern: Pattern =
            Pattern.compile(Pattern.quote(query), Pattern.CASE_INSENSITIVE)
        val matcher: Matcher = pattern.matcher(text)
        while (matcher.find()) {
            val colorSpan =
                ForegroundColorSpan(ticketName.context.getColor(R.color.psd_accent_secondary))
            spannableString.setSpan(
                colorSpan,
                matcher.start(),
                matcher.end(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return spannableString
    }

}