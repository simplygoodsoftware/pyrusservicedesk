package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayoutManager
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketView
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.entries.CommentEntry.ButtonEntry
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk.databinding.PsdViewHolderButtonBinding
import com.pyrus.pyrusservicedesk.databinding.PsdViewHolderButtonLinkBinding

internal class ButtonsAdapter(
    private val onEvent: (event: TicketView.Event) -> Unit,
): RecyclerView.Adapter<ButtonsAdapter.ButtonViewHolder>() {


    private var items: List<ButtonEntry> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun setItems(items: List<ButtonEntry>) {
        this.items = items
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) = when(items[position]) {
        is ButtonEntry.Link -> 0
        is ButtonEntry.Simple -> 1
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ButtonViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when(viewType) {
            0 -> ButtonLinkHolder(
                PsdViewHolderButtonLinkBinding.inflate(layoutInflater, parent, false),
            )
            else -> ButtonSimpleHolder(
                PsdViewHolderButtonBinding.inflate(layoutInflater, parent, false),
                onEvent
            )
        }
    }

    override fun onBindViewHolder(
        holder: ButtonViewHolder,
        position: Int,
    ) = when(holder) {
        is ButtonLinkHolder -> holder.bind(items[position] as ButtonEntry.Link)
        is ButtonSimpleHolder -> holder.bind(items[position] as ButtonEntry.Simple)
    }

    override fun getItemCount(): Int = items.size



    sealed class ButtonViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    private class ButtonLinkHolder(
        private val binding: PsdViewHolderButtonLinkBinding,
    ) : ButtonViewHolder(binding.root) {

        private var link: String? = null

        init {
            val tint = ConfigUtils.getAccentColor(itemView.context)
            binding.root.background.setColorFilter(tint, PorterDuff.Mode.SRC_ATOP)
            binding.btnButton.setTextColor(ConfigUtils.getAccentColor(itemView.context))

            binding.shareImageView.setColorFilter(tint, PorterDuff.Mode.SRC_ATOP)
            binding.btnButton.setOnClickListener {
                try {
                    binding.btnButton.context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                        data = link?.toUri()
                    })
                } catch (exception: Exception) {
                    exception.printStackTrace()
                }
            }
        }

        fun bind(entry: ButtonEntry.Link) {
            link = entry.link
            val lp = binding.btnButton.layoutParams
            if (lp is FlexboxLayoutManager.LayoutParams) {
                lp.flexGrow = 1f
            }
            binding.btnButton.text = entry.text
        }
    }

    private class ButtonSimpleHolder(
        private val binding: PsdViewHolderButtonBinding,
        private val onEvent: (event: TicketView.Event) -> Unit,
    ) : ButtonViewHolder(binding.root) {

        private var text: String? = null
        init {
            val tint = ConfigUtils.getAccentColor(itemView.context)
            binding.btnButton.background.setColorFilter(tint, PorterDuff.Mode.SRC_ATOP)
            binding.btnButton.setTextColor(ConfigUtils.getAccentColor(itemView.context))
            binding.btnButton.setOnClickListener {
                onEvent(TicketView.Event.OnButtonClick(text ?: ""))
            }
        }

        fun bind(entry: ButtonEntry.Simple) {
            text = entry.text
            val lp = binding.btnButton.layoutParams
            if (lp is FlexboxLayoutManager.LayoutParams) {
                lp.flexGrow = 1f
            }
            binding.btnButton.text = entry.text
        }
    }


}



