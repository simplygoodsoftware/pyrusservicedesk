package net.papirus.pyrusservicedesk.presentation.ui.view.recyclerview.item_decorators

import android.graphics.Rect
import android.support.annotation.Px
import android.support.v7.widget.RecyclerView
import android.view.View

internal class SpaceItemDecoration(@Px val itemSpace: Int,
                                   private val itemSpaceMultiplier: SpaceMultiplier? = null)
    : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect,
                                view: View,
                                parent: RecyclerView,
                                state: RecyclerView.State) {

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

internal interface SpaceMultiplier {
    fun getMultiplier(adapterPosition: Int): Float
}