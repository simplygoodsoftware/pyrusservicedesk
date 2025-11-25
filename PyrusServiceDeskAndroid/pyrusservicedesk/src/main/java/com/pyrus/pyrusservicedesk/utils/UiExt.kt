package com.pyrus.pyrusservicedesk.utils

import android.view.HapticFeedbackConstants
import android.view.View

internal fun Int.dp(): Int {
    return UiUtils.dpToPx(this)
}

internal fun Float.dp(): Float {
    return UiUtils.dpToPx(this)
}

internal fun View.hapticFeedback() {
    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP,  HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
}
