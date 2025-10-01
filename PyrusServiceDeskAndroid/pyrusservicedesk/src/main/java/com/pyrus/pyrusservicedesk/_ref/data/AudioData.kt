package com.pyrus.pyrusservicedesk._ref.data

import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.fingerprints.AudioStatus

/**
 * Represents ticket object.
 * @param attachId attachId.
 * @param position current position.
 * @param status downloading and playing status.
 */

data class AudioData(
    val position: Int,
    val status: AudioStatus,
    val audioFullTime: Long?,
    val audioCurrentTime: Long?,
    val url: String,
)