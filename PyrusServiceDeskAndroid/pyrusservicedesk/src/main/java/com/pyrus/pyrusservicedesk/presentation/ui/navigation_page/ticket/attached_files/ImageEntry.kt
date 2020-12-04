package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.attached_files

import com.pyrus.pyrusservicedesk.sdk.data.Attachment

internal class ImageEntry(
    val attachment: Attachment
) : AttachmentEntry {

    override val type : AttachmentEntry.Type = AttachmentEntry.Type.IMAGE

}
