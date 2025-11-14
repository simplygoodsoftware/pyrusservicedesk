package com.pyrus.pyrusservicedesk

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class OpenTicketAction(
    val ticketId: Long,
    val user: User,
): Parcelable