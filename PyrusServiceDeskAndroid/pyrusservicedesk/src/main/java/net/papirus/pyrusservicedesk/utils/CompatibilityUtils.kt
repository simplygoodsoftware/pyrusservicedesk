package net.papirus.pyrusservicedesk.utils

import android.os.Build

internal fun isCapturingPhotoSupported() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN