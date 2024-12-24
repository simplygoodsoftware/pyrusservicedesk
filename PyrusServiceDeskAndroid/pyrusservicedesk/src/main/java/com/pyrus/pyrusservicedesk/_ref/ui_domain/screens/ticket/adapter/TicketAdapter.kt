package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketView
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.new_entries.CommentEntryV2
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.view_holders.ButtonsHolder
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.view_holders.DateViewHolder
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.view_holders.InboundCommentHolder
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.view_holders.OutboundCommentHolder
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.view_holders.RatingCommentHolder
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.view_holders.RatingHolder
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.view_holders.WelcomeMessageHolder
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.ViewHolderBase

/**
 * Adapter that is used for rendering comment feed of the ticket screen.
 */
internal class TicketAdapter(
    private val onErrorCommentEntryClickListener: (id: Long) -> Unit,
    private val onEvent: (event: TicketView.Event) -> Unit,
    private val onTextCommentLongClicked: (String) -> Unit,
    private val onRatingClickListener: (Int) -> Unit,
): ListAdapter<CommentEntryV2, ViewHolderBase<CommentEntryV2>>(CommentsItemCallback()) {

    override fun getItemViewType(position: Int): Int = when(val entry = getItem(position)) {
        is CommentEntryV2.Buttons -> VIEW_TYPE_COMMENT_BUTTONS
        is CommentEntryV2.Comment -> if (entry.isInbound) VIEW_TYPE_COMMENT_OUTBOUND else VIEW_TYPE_COMMENT_INBOUND
        is CommentEntryV2.Date -> VIEW_TYPE_DATE
        is CommentEntryV2.Rating -> VIEW_TYPE_COMMENT_RATING
        is CommentEntryV2.RatingSelector -> VIEW_TYPE_RATING
        is CommentEntryV2.SimpleText -> VIEW_TYPE_WELCOME_MESSAGE
    }

    @Suppress("UNCHECKED_CAST")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderBase<CommentEntryV2> {
        return when(viewType) {
            VIEW_TYPE_COMMENT_INBOUND -> InboundCommentHolder(
                parent,
                onErrorCommentEntryClickListener,
                onEvent,
                onTextCommentLongClicked,
            )
            VIEW_TYPE_COMMENT_OUTBOUND -> OutboundCommentHolder(
                parent,
                onErrorCommentEntryClickListener,
                onEvent,
                onTextCommentLongClicked,
            )
            VIEW_TYPE_WELCOME_MESSAGE -> WelcomeMessageHolder(parent)
            VIEW_TYPE_RATING -> RatingHolder(parent, onRatingClickListener)
            VIEW_TYPE_COMMENT_RATING -> RatingCommentHolder(parent, onErrorCommentEntryClickListener)
            VIEW_TYPE_COMMENT_BUTTONS -> ButtonsHolder(parent)
            else -> DateViewHolder(parent)
        } as ViewHolderBase<CommentEntryV2>
    }

    override fun onBindViewHolder(holder: ViewHolderBase<CommentEntryV2>, position: Int) {
        holder.bindItem(getItem(position))
    }

    override fun onViewAttachedToWindow(holder: ViewHolderBase<CommentEntryV2>) {
        super.onViewAttachedToWindow(holder)
        holder.itemView.translationX = 0f
        holder.itemView.findViewById<View>(R.id.author_and_comment)?.let { it.translationX = 0f }
    }

    companion object {
        const val VIEW_TYPE_COMMENT_INBOUND = 0
        const val VIEW_TYPE_COMMENT_OUTBOUND = 1
        const val VIEW_TYPE_WELCOME_MESSAGE = 2
        const val VIEW_TYPE_DATE = 3
        const val VIEW_TYPE_RATING = 4
        const val VIEW_TYPE_COMMENT_RATING = 5
        const val VIEW_TYPE_COMMENT_BUTTONS = 6

        const val MAX_SYMBOLS_BEFORE_LEFT_ALIGNMENT = 8
    }

}