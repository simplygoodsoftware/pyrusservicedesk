package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.record

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.widget.FrameLayout
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk.utils.dp

class CircleFrameLayout: FrameLayout {

    constructor(context: Context) : this(context, null, 0)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        setWillNotDraw(false)
    }

    private var circleDiameter = 74f.dp()
    private val circlePaint = Paint().apply {
        style = Paint.Style.FILL
        color = ConfigUtils.getSendButtonColor(context)
        isAntiAlias = true
    }

    fun setCircleDiameter(diameter: Float) {
        circleDiameter = diameter
        invalidate()
    }

    fun getCircleDiameter(): Float {
        return circleDiameter
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f - paddingLeft
        val cy = height / 2f - paddingBottom
        canvas.drawCircle(cx, cy, circleDiameter / 2f, circlePaint)
    }
}