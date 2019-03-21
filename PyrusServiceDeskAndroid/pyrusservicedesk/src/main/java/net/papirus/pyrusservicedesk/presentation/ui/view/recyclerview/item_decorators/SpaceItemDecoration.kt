package net.papirus.pyrusservicedesk.presentation.ui.view.recyclerview.item_decorators

import android.graphics.Rect
import android.support.annotation.Px
import android.support.v7.widget.RecyclerView
import android.view.View

internal class SpaceItemDecoration(@Px val space: Int): RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect,
                                view: View,
                                parent: RecyclerView,
                                state: RecyclerView.State) {

        outRect.bottom = space

        // Add top margin only for the first item to avoid double space between items
        if (parent.getChildLayoutPosition(view) == 0) {
            outRect.top = space
        } else {
            outRect.top = 0
        }
    }
}