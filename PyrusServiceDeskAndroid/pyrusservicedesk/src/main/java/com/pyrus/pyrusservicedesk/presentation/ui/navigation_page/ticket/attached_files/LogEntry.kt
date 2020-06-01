package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.attached_files

import android.net.Uri

internal class LogEntry(
    val logUri : Uri,
    val logName : String
) : AttachmentEntry {

    override val type : AttachmentEntry.Type = AttachmentEntry.Type.LOG

}
