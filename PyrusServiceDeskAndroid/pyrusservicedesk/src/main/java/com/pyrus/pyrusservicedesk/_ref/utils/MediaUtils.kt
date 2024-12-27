package com.pyrus.pyrusservicedesk._ref.utils

import com.pyrus.pyrusservicedesk._ref.utils.FileFormat.GIF
import com.pyrus.pyrusservicedesk._ref.utils.FileFormat.JPEG
import com.pyrus.pyrusservicedesk._ref.utils.FileFormat.JPG
import com.pyrus.pyrusservicedesk._ref.utils.FileFormat.PNG

internal const val MIME_TYPE_IMAGE_ANY = "image/*"
internal const val MIME_TYPE_IMAGE_JPEG = "image/jpeg"
internal const val BYTES_IN_MEGABYTE = 1000000
internal const val BYTES_IN_KILOBYTE = 1000

/**
 * @return TRUE if preview for the file format is supported.
 */
internal fun String.canBePreviewed() = isImage()

/**
 * return TRUE if the file is image
 */
internal fun String.isImage() = hasAnyFormatOf(JPEG, JPG, PNG, GIF)

/**
 * @return TRUE of file has specified [format]
 */
internal fun String.hasFormat(format: com.pyrus.pyrusservicedesk._ref.utils.FileFormat) = endsWith(format.extension, true)

/**
 * @return TRUE of file has any format of [formats]
 */
internal fun String.hasAnyFormatOf(vararg formats: com.pyrus.pyrusservicedesk._ref.utils.FileFormat) = formats.any { hasFormat(it) }

/**
 * Provides extension of the file. If filename doesn't contains extension returns empty string.
 */
internal fun String.getExtension(): String {
    return when{
        contains('.') -> split('.').last()
        else -> ""
    }
}

internal enum class FileFormat (val extension: String){
    JPEG(".jpeg"),
    JPG(".jpg"),
    GIF(".gif"),
    PNG(".png"),
    TXT(".txt")
}