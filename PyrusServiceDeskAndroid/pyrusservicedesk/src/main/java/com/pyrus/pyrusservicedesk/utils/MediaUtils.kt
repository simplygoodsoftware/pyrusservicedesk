package com.pyrus.pyrusservicedesk.utils

import com.pyrus.pyrusservicedesk.utils.FileFormat.*

internal const val MIME_TYPE_IMAGE_ANY = "image/*"
internal const val MIME_TYPE_IMAGE_JPEG = "image/jpeg"
internal const val BYTES_IN_MEGABYTE = 1000000
internal const val BYTES_IN_KILOBYTE = 1000

/**
 * @return TRUE if preview for the file format is supported.
 */
internal fun String.canBePreviewed() = hasAnyFormatOf(JPEG, JPG, PNG, GIF, TXT)

/**
 * @return TRUE of file has specified [format]
 */
internal fun String.hasFormat(format: FileFormat) = endsWith(format.extension, true)

/**
 * @return TRUE of file has any format of [formats]
 */
internal fun String.hasAnyFormatOf(vararg formats: FileFormat) = formats.any { hasFormat(it) }

internal enum class FileFormat (val extension: String){
    JPEG(".jpeg"),
    JPG(".jpg"),
    GIF(".gif"),
    PNG(".png"),
    TXT(".txt")
}