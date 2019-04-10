package com.pyrus.pyrusservicedesk.presentation.ui.view

import android.content.Context
import android.graphics.*
import android.graphics.drawable.LayerDrawable
import android.support.v4.content.ContextCompat
import android.support.v7.content.res.AppCompatResources
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.utils.getColor

/**
 * Drawable that can render counter above the navigation icon.
 */
internal class NavigationCounterDrawable(private val context: Context)
    : LayerDrawable(arrayOf(AppCompatResources.getDrawable(context, R.drawable.psd_menu))) {

    companion object {
        private const val SIZE_FACTOR = .55f
        private const val HALF_SIZE_FACTOR = SIZE_FACTOR / 2
        private const val COUNTER_MAX_VALUE = 99
        private const val COUNTER_TEXT_STUB = "99+"
    }

    private val backgroundPaint: Paint = Paint()
    private val textPaint: Paint

    /**
     * Value that is used for rendering counter.
     * When 0, counter icon is hidden. Max value is 99. Values above this are rendered as "99+"
     */
    var counter: Int = 0
        set(value) {
            if (value != field) {
                field = value
                invalidateSelf()
            }
        }

    init {
        backgroundPaint.color = ContextCompat.getColor(context, R.color.psd_action_bar_icon_color)
        backgroundPaint.isAntiAlias = true

        textPaint = Paint()
        textPaint.color = Color.WHITE
        textPaint.isAntiAlias = true
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = SIZE_FACTOR * intrinsicHeight
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        if (counter == 0)
            return

        val bounds = bounds
        val x = bounds.width().toFloat()
        val y = bounds.height() / 2f

        val text = when {
            counter > COUNTER_MAX_VALUE -> COUNTER_TEXT_STUB
            else -> counter.toString()
        }
        val textBounds = Rect()
        textPaint.getTextBounds(text, 0, text.length, textBounds)

        val backgroundRect = Rect().apply {
            bottom = (y + bounds.height() / 2.5f).toInt()
            top = (y - bounds.height() / 2.5f).toInt()
            right = (x +
                    Math.max(
                        textBounds.width() / 2f + this.height() / (2 + HALF_SIZE_FACTOR * 4),
                        this.height() / 2f))
                .toInt()
            left = (x -
                    Math.max(
                        textBounds.width() / 2f + this.height() / (2 + HALF_SIZE_FACTOR * 4),
                        this.height() / 2f))
                .toInt()
        }
        val cornerRad = context.resources.getDimension(R.dimen.psd_counter_radius)

        canvas.drawRoundRect(
            RectF(backgroundRect).apply { inset(-height()*0.15f, -height()*0.15f) },
            cornerRad,
            cornerRad,
            Paint().apply {
                color = getColor(context, R.attr.colorPrimary)
            })

        canvas.drawRoundRect(
            RectF(backgroundRect),
            cornerRad,
            cornerRad,
            backgroundPaint)

        val textY = bounds.height() / 2 - (textPaint.descent() + textPaint.ascent()) / 2

        canvas.drawText(text, x, textY, textPaint)
    }
}