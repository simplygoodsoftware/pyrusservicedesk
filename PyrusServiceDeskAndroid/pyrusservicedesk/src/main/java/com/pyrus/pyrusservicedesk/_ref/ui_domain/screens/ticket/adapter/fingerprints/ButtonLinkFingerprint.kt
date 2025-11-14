package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.fingerprints

import android.content.Intent
import android.graphics.PorterDuff
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.android.flexbox.FlexboxLayoutManager
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.entries.CommentEntry.ButtonEntry
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk.databinding.PsdViewHolderButtonLinkBinding
import com.pyrus.pyrusservicedesk.payload_adapter.BaseViewHolder
import com.pyrus.pyrusservicedesk.payload_adapter.ItemFingerprint
import com.pyrus.pyrusservicedesk.payload_adapter.PayloadActionBuilder
import com.pyrus.pyrusservicedesk.payload_adapter.diff
import kotlin.reflect.KClass


internal class ButtonLinkFingerprint: ItemFingerprint<ButtonEntry.Link>() {
    override val layoutId: Int = R.layout.psd_view_holder_button_link
    override val entryKeyKClass: KClass<*> = ButtonEntry.Link::class
    override fun getViewHolder(
        layoutInflater: LayoutInflater,
        parent: ViewGroup
    ): BaseViewHolder<ButtonEntry.Link> {
        return ButtonLinkHolder(
            PsdViewHolderButtonLinkBinding.inflate(layoutInflater, parent, false),
        )
    }

    override fun areItemsTheSame(
        oldItem: ButtonEntry.Link,
        newItem: ButtonEntry.Link
    ) = true
}

internal class ButtonLinkHolder(
    private val binding: PsdViewHolderButtonLinkBinding,
) : BaseViewHolder<ButtonEntry.Link>(binding.root) {

    private var link: String? = null

    init {
        val tint = ConfigUtils.getAccentColor(itemView.context)
        binding.root.background.setColorFilter(tint, PorterDuff.Mode.SRC_ATOP)
        binding.btnButton.setTextColor(ConfigUtils.getAccentColor(itemView.context))

        binding.shareImageView.setColorFilter(tint, PorterDuff.Mode.SRC_ATOP)
        binding.btnButton.setOnClickListener {
            try {
                binding.btnButton.context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(link)
                })
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
        }
    }

    override fun bind(builder: PayloadActionBuilder<ButtonEntry.Link>) = builder.diff {
        link = entry.link
        this.entry::text.payloadCheck {
            val lp = binding.btnButton.layoutParams
            if (lp is FlexboxLayoutManager.LayoutParams) {
                lp.flexGrow = 1f
            }
            binding.btnButton.text = entry.text
        }
    }
}