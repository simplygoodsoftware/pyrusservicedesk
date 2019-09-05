package com.pyrus.pyrusservicedesk.presentation.ui.view

import android.content.Context
import android.graphics.*
import androidx.appcompat.widget.AppCompatImageView
import android.util.AttributeSet

/**
 * [AppCompatImageView] that decorates the view with an outline
 * using the given [outlineColor], [outlineWidth], [outlineRadius]
 * [edges] is used for setting up the edges that are should be outlined.
 */
internal class OutlineImageView @JvmOverloads constructor(context: Context,
                                                          attrs: AttributeSet? = null,
                                                          defStyleAttr: Int = 0)
    : AppCompatImageView(context, attrs, defStyleAttr) {

    internal companion object {
        const val EDGE_LEFT = 1
        const val EDGE_TOP = 2
        const val EDGE_RIGHT = 4
        const val EDGE_BOTTOM = 8
    }

    var outlineColor = Color.TRANSPARENT
    var outlineWidth = 0
    var outlineRadius = 0
    var edges = EDGE_LEFT or EDGE_TOP or EDGE_RIGHT or EDGE_BOTTOM

    private var mask: Bitmap? = null
    private val maskPaint = Paint().apply {
        flags = Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        onDrawForegroundCompat(canvas) // api >= 23 is required for [onDrawForeground]
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mask?.let {
            if (!it.isRecycled)
                it.recycle()
        }
        mask = createMask(w, h)
    }

    private fun onDrawForegroundCompat(canvas: Canvas?) {
        if (canvas == null || canvas.width == 0 || canvas.height == 0)
            return
        if (mask == null)
            mask = createMask(canvas.width, canvas.height)
        canvas.drawBitmap(mask!!, 0f, 0f, maskPaint)
    }

    private fun createMask(width: Int, height: Int): Bitmap {
        val mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(mask)

        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
        paint.color = Color.WHITE
        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRect(rect, paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
        paint.color = outlineColor
        rect.left -= if (hasEdge(EDGE_LEFT)) 0f else outlineRadius.toFloat()
        rect.top -= if (hasEdge(EDGE_TOP)) 0f else outlineRadius.toFloat()
        rect.right += if (hasEdge(EDGE_RIGHT)) 0f else outlineRadius.toFloat()
        rect.bottom += if (hasEdge(EDGE_BOTTOM)) 0f else outlineRadius.toFloat()
        canvas.drawRoundRect(rect, outlineRadius.toFloat(), outlineRadius.toFloat(), paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        rect.left += if (hasEdge(EDGE_LEFT)) outlineWidth.toFloat() else 0f
        rect.top += if (hasEdge(EDGE_TOP)) outlineWidth.toFloat() else 0f
        rect.right -= if (hasEdge(EDGE_RIGHT)) outlineWidth.toFloat() else 0f
        rect.bottom -= if (hasEdge(EDGE_BOTTOM)) outlineWidth.toFloat() else 0f
        canvas.drawRoundRect(rect, outlineRadius.toFloat(), outlineRadius.toFloat(), paint)
        return mask
    }

    private fun hasEdge(edge: Int): Boolean = edges and edge == edge

}