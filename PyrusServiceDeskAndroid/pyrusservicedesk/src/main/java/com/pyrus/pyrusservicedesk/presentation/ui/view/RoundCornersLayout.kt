package com.pyrus.pyrusservicedesk.presentation.ui.view

import android.content.Context
import android.graphics.*
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.graphics.Paint.FILTER_BITMAP_FLAG
import android.util.AttributeSet
import android.widget.FrameLayout
import com.pyrus.pyrusservicedesk.utils.getColorByAttrId


/**
 * [FrameLayout] implementation that decorate a final view with a rounded corners, cropping its content.
 */
internal class RoundCornersLayout @JvmOverloads constructor(context: Context,
                                                            attrs: AttributeSet? = null,
                                                            defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {

    /**
     * Radius that is used for drawing round corners
     */
    var cornerRadius = 0f

    private val maskPaint = Paint().apply {
        flags = ANTI_ALIAS_FLAG or FILTER_BITMAP_FLAG
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
    }
    private var mask: Bitmap? = null

    override fun draw(canvas: Canvas?) {
        super.draw(canvas)
        onDrawForegroundCompat(canvas) // api >= 23 is required for [onDrawForeground]
    }

    private fun onDrawForegroundCompat(canvas: Canvas?) {
        if (canvas == null || width == 0 || height == 0)
            return
        if (mask == null)
            mask = createMask(width, height)
        canvas.drawBitmap(mask!!, 0f, 0f, maskPaint)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mask?.let {
            if (!it.isRecycled)
                it.recycle()
        }
        mask = createMask(width, height)
    }

    private fun createMask(width: Int, height: Int): Bitmap {
        val mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(mask)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = getColorByAttrId(context, android.R.attr.windowBackground)
        }

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        canvas.drawRoundRect(
            RectF(0f, 0f, width.toFloat(), height.toFloat()),
            cornerRadius,
            cornerRadius,
            paint)

        return mask
    }
}