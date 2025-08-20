package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.record

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.graphics.withRotation
import androidx.core.graphics.withTranslation
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk.utils.UiUtils.calculateValue
import com.pyrus.pyrusservicedesk.utils.dp

internal class LockStopView : View {

    constructor(context: Context) : this(context, null, 0)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private val paint = Paint().apply {
        color = ConfigUtils.getAccentColor(context)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 1f.dp()
        isAntiAlias = true
    }

    private val bodyRectangle = RectF().apply {
        left = 5f.dp()
        right = 19f.dp()
        top = 11f.dp()
        bottom = 23f.dp()
    }
    private val capRectangle = RectF().apply {
        left = 6.5f.dp()
        right =  24f.dp() - 6.5f.dp()
        top =  2f.dp()
        bottom =  20f.dp()
    }

    private var animationProgress: Float = 0f

    private var animator: ValueAnimator? = null

    fun setAnimationProgress(step: Int, progress: Float) {
        animationProgress = when(step) {
            0 -> calculateValue(progress, 0f, 0.33f)
            1 -> calculateValue(progress, 0.33f, 0.66f)
            else -> calculateValue(progress, 0.66f, 1f)
        }
        invalidate()
    }

    fun animateToEnd() {
        animator?.cancel()
        val animator = ValueAnimator.ofFloat(animationProgress, 1f)
        animator.setDuration(1000)
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.addUpdateListener({ animation ->
            val value = animation.getAnimatedValue() as Float
            animationProgress = value
            invalidate()
        })
        animator.start()
        this.animator = animator
    }

    fun cancelAnimation() {
        animator?.cancel()
        if (animationProgress == 0f) return
        animationProgress = 0f
    }

    fun cleanAnimation() {
        if (animationProgress == 0f) return
        animationProgress = 0f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {

        val width = canvas.width
        val height = canvas.height

        if (width <= 0 || height <= 0) return

        val rotation = calcValue(0f, 15f, -5f, 0f)
        val translationY = (-calcValue(0f, 1f, 1f, 0f)).dp()
        val startAngle = 360f - 180f * calcValue(start = 0.8f, step1 = 1f, step2 = 1f)
        val capAlpha = calcSpecificValue(
            startP = 0.66f,
            endP = 0.77f,
            startV = 255f,
            endV = 0f
        ).toInt()

        bodyRectangle.left = calcValue(start = 5f.dp(), end = 4f.dp())
        bodyRectangle.right = calcValue(start = 19f.dp(), end = 20f.dp())
        bodyRectangle.top = calcValue(start = 11f.dp(), end = 4f.dp())
        bodyRectangle.bottom = calcValue(start = 23f.dp(), end = 20f.dp())


        canvas.withTranslation(y = translationY) {
            canvas.withRotation(rotation, 12f.dp(), 18f.dp()) {
                val round = 2f.dp()
                paint.alpha = 255
                drawRoundRect(bodyRectangle, round, round, paint)

                val sweepAngle = 360f - startAngle
                paint.alpha = capAlpha
                drawArc(capRectangle, startAngle, sweepAngle, false, paint)
            }
        }

    }

    private fun calcValue(
        start: Float,
        step1: Float = start,
        step2: Float = step1,
        end: Float = step2,
    ): Float {
        val progress = animationProgress
        val step = 1f / 3f
        when {
            progress < step -> {
                val currentProgress = progress / step
                return calculateValue(currentProgress, start, step1)
            }
            progress < step * 2 -> {
                val currentProgress = (progress - step) / step
                return calculateValue(currentProgress, step1, step2)
            }
            else -> {
                val currentProgress = (progress - step * 2) / step
                return calculateValue(currentProgress, step2, end)
            }
        }
    }

    private fun calcSpecificValue(startP: Float, endP: Float, startV: Float, endV: Float): Float {
        val progress = animationProgress
        return when {
            progress < startP -> startV
            progress >= endP -> endV
            else -> {
                val p = (progress - startP) / (endP - startP)
                calculateValue(p, startV, endV)
            }
        }
    }

}