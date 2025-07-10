package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pyrus.pyrusservicedesk.databinding.PsdViewHolderRatingItemBinding
import com.pyrus.pyrusservicedesk.sdk.data.intermediate.RatingTextValues

internal class RatingAdapter(private val onItemClick: (Int) -> Unit) : ListAdapter<RatingTextValues, SimpleRatingHolder>(MyItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleRatingHolder {
        val binding = PsdViewHolderRatingItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SimpleRatingHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: SimpleRatingHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class SimpleRatingHolder(
        private val binding: PsdViewHolderRatingItemBinding,
        private val onItemClick: (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private var rating: Int = 0
        init {
            binding.ratingText.setOnClickListener {
                onItemClick(rating)
            }
        }

        fun bind(item: RatingTextValues) {
            rating = item.rating ?: 0
            binding.ratingText.text = item.text
        }
    }

class MyItemDiffCallback : DiffUtil.ItemCallback<RatingTextValues>() {
    override fun areItemsTheSame(oldItem: RatingTextValues, newItem: RatingTextValues): Boolean {
        return oldItem.rating == newItem.rating
    }

    override fun areContentsTheSame(oldItem: RatingTextValues, newItem: RatingTextValues): Boolean {
        return oldItem == newItem
    }
}