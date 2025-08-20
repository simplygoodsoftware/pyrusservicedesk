package com.pyrus.pyrusservicedesk.presentation.ui.view.recyclerview.item_decorators

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt

internal class DividerVerticalItemDecoration(
    private val dividerHeight: Int,
    private val dividerLeftMargin: Int,
    private val dividerColor: Int,
    private val dividerPredicate: (current: RecyclerView.ViewHolder?, next: RecyclerView.ViewHolder?) -> Boolean,
) : RecyclerView.ItemDecoration() {

    private val paint = Paint().apply {
        color = dividerColor
        style = Paint.Style.FILL
    }


    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)

        val position = parent.getChildLayoutPosition(view)
        if (position == RecyclerView.NO_POSITION || position == parent.adapter?.itemCount?.minus(1)) return

        val currentHolder = parent.findViewHolderForLayoutPosition(position)
        val nextHolder = parent.findViewHolderForLayoutPosition(position + 1)

        if (dividerPredicate(currentHolder, nextHolder)) {
            outRect.bottom = dividerHeight
        }
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val left = parent.paddingLeft
        val right = parent.width - parent.paddingRight

        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val position = parent.getChildLayoutPosition(child)
            if (position == RecyclerView.NO_POSITION) continue

            val currentHolder = parent.findViewHolderForLayoutPosition(position)
            val nextHolder = parent.findViewHolderForLayoutPosition(position + 1)

            if (dividerPredicate(currentHolder, nextHolder)) {
                paint.alpha = (child.alpha * 255).toInt()
                val top = child.bottom + child.translationY.roundToInt();
                val bottom = top + dividerHeight
                val leftSide = left + dividerLeftMargin
                c.drawRect(leftSide.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint)
            }
        }
    }

}