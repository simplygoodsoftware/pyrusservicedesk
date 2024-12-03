package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.view_holders

import android.content.Intent
import android.graphics.PorterDuff
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.databinding.PsdViewHolderButtonsBinding
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.TicketAdapter
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.entries.ButtonEntry
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.adapter.entries.ButtonsEntry
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.ViewHolderBase
import com.pyrus.pyrusservicedesk.utils.ConfigUtils

internal class ButtonsHolder(parent: ViewGroup): ViewHolderBase<ButtonsEntry>(parent,
    R.layout.psd_view_holder_buttons
) {

    private val binding = PsdViewHolderButtonsBinding.bind(itemView)

    override fun bindItem(item: ButtonsEntry) {
        super.bindItem(item)

        item.buttons.forEachIndexed { index, buttonEntry ->
            (binding.flButtons.getChildAt(index) as? TextView)?.apply {
                val buttonText = buttonEntry.text


                if (buttonText.length > TicketAdapter.MAX_SYMBOLS_BEFORE_LEFT_ALIGNMENT) {
                    gravity = Gravity.START
                }
                else {
                    gravity - Gravity.CENTER
                }

                val frame = background
                val tint = ConfigUtils.getAccentColor(itemView.context)
                frame.setColorFilter(tint, PorterDuff.Mode.SRC_ATOP)
                setTextColor(ConfigUtils.getAccentColor(itemView.context))


                when(buttonEntry) {
                    is ButtonEntry.Link -> {
                        text = "$buttonText   "
                        val shareDrawable = AppCompatResources.getDrawable(
                            context,
                            R.drawable.ic_share
                        )!!
                        shareDrawable.setColorFilter(tint, PorterDuff.Mode.SRC_ATOP)
                        setCompoundDrawablesWithIntrinsicBounds(null, null, shareDrawable, null)
                        setOnClickListener {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW).apply { data =
                                    Uri.parse(buttonEntry.link)
                                })
                            }
                            catch (exception: Exception) {
                                exception.printStackTrace()
                            }
                        }

                    }

                    is ButtonEntry.Simple -> {
                        text = buttonText
                        setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                        setOnClickListener { item.onButtonClick.invoke(buttonText) }
                    }
                }

                visibility = View.VISIBLE
            }
        }

        repeat(binding.flButtons.childCount - item.buttons.size) {
            binding.flButtons.getChildAt(binding.flButtons.childCount - 1 - it).apply {
                visibility = View.GONE
                setOnClickListener(null)
            }
        }
    }
}