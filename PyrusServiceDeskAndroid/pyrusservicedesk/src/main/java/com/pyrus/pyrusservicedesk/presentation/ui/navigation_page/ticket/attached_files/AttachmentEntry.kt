package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.attached_files

internal interface AttachmentEntry {

    val type : Type

    enum class Type {
        TEXT,
        IMAGE
    }

}