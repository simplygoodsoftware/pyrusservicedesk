package com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.item_decorators

import android.graphics.Rect
import androidx.annotation.Px
import androidx.recyclerview.widget.RecyclerView
import android.view.View

/**
 * [RecyclerView.ItemDecoration] implementation that applies spacing between items in list.
 * NB: This applies spaces only between items.
 * Spaces preceding the first item and following the last one should be applied separately.
 * Spaces are applied only to the top bound of the item.
 *
 * @param itemSpace space in pixels that is applied by default.
 * @param itemSpaceMultiplier [SpaceMultiplier] implementation that is allowed to adjust default [itemSpace]
 * between items
 */
internal class SpaceItemDecoration(@Px val itemSpace: Int,
                                   private val itemSpaceMultiplier: SpaceMultiplier? = null)
    : androidx.recyclerview.widget.RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect,
                                view: View,
                                parent: androidx.recyclerview.widget.RecyclerView,
                                state: androidx.recyclerview.widget.RecyclerView.State) {

        if (parent.getChildAdapterPosition(view) == 0) {
            outRect.top = 0
            return
        }

        var spaceFinal = itemSpace
        itemSpaceMultiplier?.let {
            spaceFinal = (spaceFinal * it.getMultiplier(parent.getChildAdapterPosition(view))).toInt()
        }

        outRect.top = spaceFinal
    }
}

/**
 * Should be implemented to be able to adjust default space between items.
 */
internal interface SpaceMultiplier {
    /**
     * Provides multiplier that should be applied to each space that is going to be applied.
     * @param adapterPosition position of the item to which top space is going to be applied.
     * NB: 0th position always treated by [SpaceItemDecoration]
     */
    fun getMultiplier(adapterPosition: Int): Float
}