package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.attached_files

import android.net.Uri

internal class ImageEntry(
    val imageUri : Uri
) : AttachmentEntry {

    override val type : AttachmentEntry.Type = AttachmentEntry.Type.IMAGE

}