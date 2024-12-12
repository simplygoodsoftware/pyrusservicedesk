package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.view_holders

import android.net.Uri
import android.view.ViewGroup
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.entries.CommentEntry
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.new_entries.CommentEntryV2
import com.pyrus.pyrusservicedesk.presentation.ui.view.CommentView
import com.pyrus.pyrusservicedesk.sdk.data.Attachment

internal class OutboundCommentHolder(
    parent: ViewGroup,
    onErrorCommentEntryClickListener: (entry: CommentEntryV2.Comment) -> Unit,
    onFileReadyToPreviewClickListener: (uri: Uri) -> Unit,
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