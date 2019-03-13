package net.papirus.pyrusservicedesk.ui.usecases.ticket

import net.papirus.pyrusservicedesk.ui.ui_updates.UiUpdateBase
import net.papirus.pyrusservicedesk.ui.usecases.ticket.entries.CommentEntry

internal class CommentChangedUiUpdate(
        val type: ChangeType,
        commentEntry: CommentEntry)
    : UiUpdateBase<CommentEntry>(commentEntry) {
}

internal enum class ChangeType {
    Added,
    Changed
}