package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.attached_files

import androidx.recyclerview.widget.DiffUtil

internal class AttachmentsDiffCallback(
    private val oldList: List<AttachmentEntry>,
    private val newList: List<AttachmentEntry>
) : DiffUtil.Callback() {

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldEntry = oldList[oldItemPosition]
        val newEntry = newList[newItemPosition]
        return when {
            oldEntry is ImageEntry
                    && newEntry is ImageEntry
                    && oldEntry.attachment == newEntry.attachment -> true
            oldEntry is TextEntry
                    && newEntry is TextEntry
                    && oldEntry.attachment == newEntry.attachment -> true
            else -> false
        }
    }

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldEntry = oldList[oldItemPosition]
        val newEntry = newList[newItemPosition]
        return when {
            oldEntry is ImageEntry
                    && newEntry is ImageEntry
                    && oldEntry.attachment == newEntry.attachment -> true
            oldEntry is TextEntry
                    && newEntry is TextEntry
                    && oldEntry.attachment == newEntry.attachment -> true
            else -> false
        }
    }

}
