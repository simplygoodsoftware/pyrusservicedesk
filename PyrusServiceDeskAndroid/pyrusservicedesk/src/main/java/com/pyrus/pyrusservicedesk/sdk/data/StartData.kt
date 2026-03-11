package com.pyrus.pyrusservicedesk.sdk.data

import android.os.Parcelable
import com.pyrus.pyrusservicedesk.OpenTicketAction
import com.pyrus.pyrusservicedesk.core.Account
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class StartData(
    val account: Account,
    val openTicketAction: OpenTicketAction?,
    val sendComment: String?,
    val localeLanguage: String?,
    val localeCountry: String?,
) : Parcelable
