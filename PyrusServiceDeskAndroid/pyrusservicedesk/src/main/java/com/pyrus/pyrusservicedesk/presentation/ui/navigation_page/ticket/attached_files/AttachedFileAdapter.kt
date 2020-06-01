package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.attached_files

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.AdapterBase
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.ViewHolderBase
import com.squareup.picasso.Picasso

private const val VIEW_TYPE_LOG = 0
private const val VIEW_TYPE_IMAGE = 1

internal class AttachedFileAdapter(
    private val onRemoveEntryClickListener: (attachmentEntry : AttachmentEntry) -> Unit
) : AdapterBase<AttachmentEntry>() {

    override fun getItemViewType(position: Int): Int {
        return with(itemsList[position]) {
            return@with when (type) {
                AttachmentEntry.Type.LOG -> VIEW_TYPE_LOG
                AttachmentEntry.Type.IMAGE -> VIEW_TYPE_IMAGE
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderBase<AttachmentEntry> {
        return when(viewType){
            VIEW_TYPE_LOG -> LogHolder(parent)
            else -> ImageHolder(parent)
        }
    }

    override fun setItems(items: List<AttachmentEntry>) {
        this.itemsList = items.toMutableList()
    }

    private inner class LogHolder(parent: ViewGroup) : AbstractHolder(parent, R.layout.psd_view_holder_log) {

        override lateinit var removeButton: View

        override fun bindItem(item: AttachmentEntry) {
            if (item !is LogEntry)
                return

            removeButton = itemView.findViewById(R.id.vh_log_remove_button)

            itemView.findViewById<TextView>(R.id.vh_log_name_tv).text = item.logName

            super.bindItem(item)
        }

    }

    private inner class ImageHolder(parent: ViewGroup) : AbstractHolder(parent, R.layout.psd_view_holder_image) {

        override lateinit var removeButton: View

        private lateinit var imageView : ImageView

        override fun bindItem(item: AttachmentEntry) {
            if (item !is ImageEntry)
                return

            removeButton = itemView.findViewById(R.id.vh_image_remove_button)
            imageView = itemView.findViewById(R.id.vh_image_iv)

            Picasso.get().load(item.imageUri).into(imageView)

            super.bindItem(item)
        }

        override fun onDetachedFromWindow() {
            Picasso.get().cancelRequest(imageView)

            super.onDetachedFromWindow()
        }

    }

    private abstract inner class AbstractHolder(
        parent: ViewGroup,
        @LayoutRes layoutRes: Int
    ) : ViewHolderBase<AttachmentEntry>(parent, layoutRes){

        abstract var removeButton : View

        override fun bindItem(item: AttachmentEntry) {
            super.bindItem(item)
            removeButton.setOnClickListener { onRemoveEntryClickListener.invoke(item) }
        }

    }

}
