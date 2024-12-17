package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter

import android.net.Uri
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.view_holders.ButtonsHolder
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.view_holders.DateViewHolder
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.view_holders.InboundCommentHolder
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.view_holders.OutboundCommentHolder
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.view_holders.RatingCommentHolder
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.view_holders.RatingHolder
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.view_holders.WelcomeMessageHolder
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.new_entries.CommentEntryV2
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.ViewHolderBase
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.item_decorators.SpaceMultiplier

/**
 * Adapter that is used for rendering comment feed of the ticket screen.
 */
internal class TicketAdapter(
    private val onErrorCommentEntryClickListener: (id: CommentEntryV2) -> Unit,
    private val onFileReadyToPreviewClickListener: (uri: Uri) -> Unit,
    private val onTextCommentLongClicked: (String) -> Unit,
    private val onRatingClickListener: (Int) -> Unit,
): ListAdapter<CommentEntryV2, ViewHolderBase<CommentEntryV2>>(CommentsItemCallback()) {

    /**
     * [SpaceMultiplier] implementation for customizing spaces between items fot the feed.
     */
    val itemSpaceMultiplier = object: SpaceMultiplier {
        override fun getMultiplier(adapterPosition: Int): Float {
            return when {
                adapterPosition <= 0 -> 1f
                getItem(adapterPosition) is CommentEntryV2.Comment
                        && getItem(adapterPosition -1) is CommentEntryV2.Comment
                        && (getItem(adapterPosition) as CommentEntryV2.Comment).isInbound !=
                            (getItem(adapterPosition - 1) as CommentEntryV2.Comment).isInbound -> 2f
                else -> 1f
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when(val entry = getItem(position)) {
            is CommentEntryV2.Buttons -> VIEW_TYPE_COMMENT_BUTTONS
            is CommentEntryV2.Comment -> if (entry.isInbound) VIEW_TYPE_COMMENT_OUTBOUND else  VIEW_TYPE_COMMENT_INBOUND
            is CommentEntryV2.Date -> VIEW_TYPE_DATE
            is CommentEntryV2.Rating -> VIEW_TYPE_COMMENT_RATING
            CommentEntryV2.RatingSelector -> VIEW_TYPE_RATING
            is CommentEntryV2.SimpleText -> VIEW_TYPE_WELCOME_MESSAGE
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderBase<CommentEntryV2> {
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