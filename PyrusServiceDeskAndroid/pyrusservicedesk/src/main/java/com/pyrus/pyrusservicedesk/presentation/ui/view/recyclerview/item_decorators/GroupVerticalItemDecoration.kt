package com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.item_decorators

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

internal class GroupVerticalItemDecoration(
    private val viewType: Int,
    private val innerDivider: Int,
    private val outerDivider: Int,
    private val invert: Boolean = false,
    private val excludeTypes: Set<Int> = emptySet() // works with any
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)

        val viewHolder = parent.getChildViewHolder(view)
        val itemViewType = viewHolder.itemViewType

        if (itemViewType in excludeTypes) return

        if (viewType != TYPE_ANY && viewType != itemViewType) return

        val adapter = parent.adapter ?: return
        val currentPosition = parent.getChildAdapterPosition(view).takeIf { it != RecyclerView.NO_POSITION } ?: viewHolder.oldPosition

        val isPrevTargetView = adapter.isPrevTargetView(currentPosition, itemViewType)
        val isNextTargetView = adapter.isNextTargetView(currentPosition, itemViewType)

        val oneSideInnerDivider = innerDivider / 2
        if (invert) with(outRect) {
            top = if (isNextTargetView) oneSideInnerDivider else outerDivider
            bottom = if (isPrevTargetView) oneSideInnerDivider else outerDivider
        }
        else with(outRect) {
            top = if (isPrevTargetView) oneSideInnerDivider else outerDivider
            bottom = if (isNextTargetView) oneSideInnerDivider else outerDivider
        }
    }

    private fun RecyclerView.Adapter<*>.isPrevTargetView(
        currentPosition: Int,
        viewType: Int
    ) = currentPosition != 0 && getItemViewType(currentPosition - 1) == viewType

    private fun RecyclerView.Adapter<*>.isNextTargetView(
        currentPosition: Int,
        viewType: Int
    ): Boolean {
        val lastIndex = itemCount - 1
        return currentPosition != lastIndex && getItemViewType(currentPosition + 1) == viewType
    }

    companion object {
        const val TYPE_ANY = Int.MIN_VALUE
    }

}