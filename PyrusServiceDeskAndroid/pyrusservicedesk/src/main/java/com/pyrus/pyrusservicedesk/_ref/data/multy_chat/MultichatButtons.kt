package com.pyrus.pyrusservicedesk._ref.data.multy_chat

import android.content.Intent
import android.os.Parcelable
import androidx.annotation.DrawableRes
import kotlinx.parcelize.Parcelize

@Parcelize
data class MultichatButtons(
    @DrawableRes val rightButtonRes: Int,
    val rightButtonAction: Intent,
    val centerAction: Intent,
    val backButton: Intent,
) : Parcelable
