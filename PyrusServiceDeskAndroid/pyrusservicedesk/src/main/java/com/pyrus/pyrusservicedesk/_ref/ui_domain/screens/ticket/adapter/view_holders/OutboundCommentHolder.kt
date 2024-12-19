package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.view_holders

import android.net.Uri
import android.view.ViewGroup
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.presentation.ui.view.CommentView
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.FileData

internal class OutboundCommentHolder(
    parent: ViewGroup,
    onErrorCommentEntryClickListener: (id: Long) -> Unit,
    onFileReadyToPreviewClickListener: (fileData: FileData) -> Unit,
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