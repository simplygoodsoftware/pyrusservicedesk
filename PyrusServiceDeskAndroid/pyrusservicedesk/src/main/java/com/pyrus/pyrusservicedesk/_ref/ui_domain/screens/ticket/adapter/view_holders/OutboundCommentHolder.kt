package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.view_holders

import android.view.ViewGroup
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketView
import com.pyrus.pyrusservicedesk.presentation.ui.view.CommentView

internal class OutboundCommentHolder(
    parent: ViewGroup,
    onEvent: (event: TicketView.Event) -> Unit,
) : CommentHolder(
    parent,
    R.layout.psd_view_holder_comment_outbound,
    onEvent
) {

    override val comment: CommentView = itemView.findViewById(R.id.comment)

}