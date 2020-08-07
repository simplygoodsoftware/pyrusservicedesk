package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.attached_files

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.app.ActivityCompat
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.presentation.ui.view.OutlineImageView
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.AdapterBase
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.ViewHolderBase
import com.squareup.picasso.Picasso

private const val VIEW_TYPE_TEXT = 0
private const val VIEW_TYPE_IMAGE = 1

internal class AttachedFileAdapter(
    private val onRemoveEntryClickListener: (attachmentEntry : AttachmentEntry) -> Unit
) : AdapterBase<AttachmentEntry>() {

    override fun getItemViewType(position: Int): Int {
        return with(itemsList[position]) {
            return@with when (type) {
                AttachmentEntry.Type.TEXT -> VIEW_TYPE_TEXT
                AttachmentEntry.Type.IMAGE -> VIEW_TYPE_IMAGE
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderBase<AttachmentEntry> {
        return when(viewType){
            VIEW_TYPE_TEXT -> TextHolder(parent)
            else -> ImageHolder(parent)
        }
    }

    override fun setItems(items: List<AttachmentEntry>) {
        this.itemsList = items.toMutableList()
    }

    private inner class TextHolder(parent: ViewGroup) : AbstractHolder(parent, R.layout.psd_view_holder_text_file) {

        override lateinit var removeButton: View

        override fun bindItem(item: AttachmentEntry) {
            if (item !is TextEntry)
                return

            removeButton = itemView.findViewById(R.id.vh_tf_remove_button)

            itemView.findViewById<TextView>(R.id.vh_tf_name_tv).text = item.attachment.name

            super.bindItem(item)
        }

    }

    private inner class ImageHolder(parent: ViewGroup) : AbstractHolder(parent, R.layout.psd_view_holder_image_file) {

        override lateinit var removeButton: View

        private lateinit var imageView : OutlineImageView

        override fun bindItem(item: AttachmentEntry) {
            if (item !is ImageEntry)
                return

            removeButton = itemView.findViewById(R.id.vh_image_remove_button)
            imageView = itemView.findViewById(R.id.vh_image_iv)

            imageView.outlineColor = ActivityCompat.getColor(imageView.context, R.color.psd_comment_preview_outline)
            imageView.outlineRadius = imageView.resources.getDimensionPixelSize(R.dimen.psd_comment_radius)
            imageView.outlineWidth = imageView.resources.getDimensionPixelSize(R.dimen.psd_comment_preview_outline_radius)

            Picasso
                .get()
                .load(item.attachment.localUri)
                .fit()
                .centerCrop()
                .into(imageView)

            super.bindItem(item)
        }

        override fun onDetachedFromWindow() {
            Picasso
                .get()
                .cancelRequest(imageView)

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
