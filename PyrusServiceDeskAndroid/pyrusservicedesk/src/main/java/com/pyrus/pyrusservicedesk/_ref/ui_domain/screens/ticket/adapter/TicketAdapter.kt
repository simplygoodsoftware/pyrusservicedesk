package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter

import android.view.View
import android.view.ViewGroup
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.view_holders.ButtonsHolder
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.view_holders.DateViewHolder
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.view_holders.InboundCommentHolder
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.view_holders.OutboundCommentHolder
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.view_holders.RatingCommentHolder
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.view_holders.RatingHolder
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.view_holders.WelcomeMessageHolder
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries.CommentEntry
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries.TicketEntry
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries.Type
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.AdapterBase
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.ViewHolderBase
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.item_decorators.SpaceMultiplier
import com.pyrus.pyrusservicedesk.sdk.data.Attachment

/**
 * Adapter that is used for rendering comment feed of the ticket screen.
 */
internal class TicketAdapter(
    private val onErrorCommentEntryClickListener: (entry: CommentEntry) -> Unit,
    private val onFileReadyToPreviewClickListener: (attachment: Attachment) -> Unit,
    private val onTextCommentLongClicked: (String) -> Unit,
    private val onRatingClickListener: (Int) -> Unit,
): AdapterBase<TicketEntry>() {

    /**
     * [SpaceMultiplier] implementation for customizing spaces between items fot the feed.
     */
    val itemSpaceMultiplier = object: SpaceMultiplier {
        override fun getMultiplier(adapterPosition: Int): Float {
            return when {
                adapterPosition <= 0 -> 1f
                itemsList[adapterPosition].type == Type.Comment
                        && itemsList[adapterPosition -1].type == Type.Comment
                        && (itemsList[adapterPosition] as CommentEntry).comment.isInbound !=
                            (itemsList[adapterPosition - 1] as CommentEntry).comment.isInbound -> 2f
                else -> 1f
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return with(itemsList[position]) {
            return@with when {
                type == Type.Date -> VIEW_TYPE_DATE
                type == Type.WelcomeMessage -> VIEW_TYPE_WELCOME_MESSAGE
                type == Type.Rating -> VIEW_TYPE_RATING
                type == Type.Buttons -> VIEW_TYPE_COMMENT_BUTTONS
                (this as CommentEntry).comment.rating != null -> VIEW_TYPE_COMMENT_RATING
                this.comment.isInbound -> VIEW_TYPE_COMMENT_OUTBOUND
                else -> VIEW_TYPE_COMMENT_INBOUND
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderBase<TicketEntry> {
        return when(viewType){
            VIEW_TYPE_COMMENT_INBOUND -> InboundCommentHolder(
                parent,
                onErrorCommentEntryClickListener,
                onFileReadyToPreviewClickListener,
                onTextCommentLongClicked,
            )
            VIEW_TYPE_COMMENT_OUTBOUND -> OutboundCommentHolder(
                parent,
                onErrorCommentEntryClickListener,
                onFileReadyToPreviewClickListener,
                onTextCommentLongClicked,
            )
            VIEW_TYPE_WELCOME_MESSAGE -> WelcomeMessageHolder(parent)
            VIEW_TYPE_RATING -> RatingHolder(parent, onRatingClickListener)
            VIEW_TYPE_COMMENT_RATING -> RatingCommentHolder(parent, onErrorCommentEntryClickListener)
            VIEW_TYPE_COMMENT_BUTTONS -> ButtonsHolder(parent)
            else -> DateViewHolder(parent)
        } as ViewHolderBase<TicketEntry>
    }

    override fun onViewAttachedToWindow(holder: ViewHolderBase<TicketEntry>) {
        super.onViewAttachedToWindow(holder)
        holder.itemView.translationX = 0f
        holder.itemView.findViewById<View>(R.id.author_and_comment)?.let { it.translationX = 0f }
    }

    override fun setItems(items: List<TicketEntry>) {
        this.itemsList = items.toMutableList()
    }

    companion object {
        private const val VIEW_TYPE_COMMENT_INBOUND = 0
        private const val VIEW_TYPE_COMMENT_OUTBOUND = 1
        private const val VIEW_TYPE_WELCOME_MESSAGE = 2
        private const val VIEW_TYPE_DATE = 3
        private const val VIEW_TYPE_RATING = 4
        private const val VIEW_TYPE_COMMENT_RATING = 5
        private const val VIEW_TYPE_COMMENT_BUTTONS = 6


        const val MAX_SYMBOLS_BEFORE_LEFT_ALIGNMENT = 8
    }

}