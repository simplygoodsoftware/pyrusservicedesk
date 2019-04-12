package com.pyrus.pyrusservicedesk.utils

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.drawable.RotateDrawable
import android.view.animation.Interpolator

internal fun RotateDrawable.animateInfinite(duration: Long, interpolator: Interpolator) {
    ObjectAnimator.ofInt(this, "level", 0, 10000).apply {
        repeatCount = ValueAnimator.INFINITE
        this.interpolator = interpolator
        this.duration = duration
    }.also { animator -> animator.start() }
}