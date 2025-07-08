package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.databinding.PsdViewHolderRatingItemBinding
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.entries.SimpleRatingEntry
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.AdapterBase
import com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.ViewHolderBase
import com.pyrus.pyrusservicedesk.utils.ConfigUtils

internal class RatingAdapter(private val entryList: List<SimpleRatingEntry>): RecyclerView.Adapter<RatingAdapter.SimpleRatingHolder>() {


    private var onRatingClickListener: ((Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleRatingHolder {
        return SimpleRatingHolder(parent)
    }

    override fun getItemCount(): Int {
        return entryList.size
    }

    override fun onBindViewHolder(holder: SimpleRatingHolder, position: Int) {
        holder.bindItem(entryList[position])
    }

    internal inner class SimpleRatingHolder(parent: ViewGroup) :
        ViewHolderBase<SimpleRatingEntry>(parent, R.layout.psd_view_holder_rating_item) {

//        private val binding: PsdViewHolderRatingItemBinding = PsdViewHolderRatingItemBinding.inflate(
//            LayoutInflater.from(parent.context))

        private var rating: Int = 0

        private val rating1 = itemView.findViewById<ImageButton>(R.id.rating)

        private val ratingText1 = itemView.findViewById<TextView>(R.id.ratingText)


        init {
            ConfigUtils.getSecondaryColorOnMainBackground(itemView.context).apply {
                itemView.setBackgroundColor(this)
            }
            itemView.setOnClickListener {
                onRatingClickListener?.invoke(
                    rating
                )
            }
        }

        override fun bindItem(item: SimpleRatingEntry) {
            super.bindItem(item)
            rating = item.rating
            rating1.setImageResource(item.iconRes)
            ratingText1.text = item.ratingText
        }
    }

    fun setOnRatingClickListener(listener: ((Int) -> Unit)) {
        onRatingClickListener = listener
    }
}