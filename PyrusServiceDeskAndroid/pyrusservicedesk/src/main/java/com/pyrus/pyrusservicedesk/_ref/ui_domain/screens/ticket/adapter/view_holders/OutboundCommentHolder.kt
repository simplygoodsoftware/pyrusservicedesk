package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.view_holders

import android.view.ViewGroup
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries.CommentEntry
import com.pyrus.pyrusservicedesk.presentation.ui.view.CommentView
import com.pyrus.pyrusservicedesk.sdk.data.Attachment

internal class OutboundCommentHolder(
    parent: ViewGroup,
    onErrorCommentEntryClickListener: (entry: CommentEntry) -> Unit,
    onFileReadyToPreviewClickListener: (attachment: Attachment) -> Unit,
    onTextCommentLongClicked: (String) -> Unit,
) : CommentHolder(
    parent,
    R.layout.psd_view_holder_comment_outbound,
    onErrorCommentEntryClickListener,
    onFileReadyToPreviewClickListener,
    onTextCommentLongClicked
) {

    override val comment: CommentView = itemView.findViewById(R.id.comment)

}