package net.papirus.pyrusservicedesk.utils

import android.os.Build

/**
 * @return TRUE if capturing photos is supported.
 */
internal fun isCapturingPhotoSupported() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN