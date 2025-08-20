package com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.support

import androidx.room.Embedded
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.CommentInfo
import com.pyrus.pyrusservicedesk.sdk.repositories.data_base.data.TicketEntity

internal data class TicketWithComment(
    @Embedded val ticket: TicketEntity,
    @Embedded val comment: CommentInfo?,
)