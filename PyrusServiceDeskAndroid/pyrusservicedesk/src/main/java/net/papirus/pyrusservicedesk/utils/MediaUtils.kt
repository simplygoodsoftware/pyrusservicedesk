package net.papirus.pyrusservicedesk.utils

import net.papirus.pyrusservicedesk.utils.FileFormat.*

internal const val INTENT_IMAGE_TYPE = "image/*"
internal const val BYTES_IN_MEGABYTE = 1000000
internal const val BYTES_IN_KILOBYTE = 1000


internal fun String.canBePreviewed() = hasAnyFormatOf(JPEG, JPG, PNG, GIF, TXT)

internal fun String.hasFormat(format: FileFormat) = endsWith(format.extension, true)

internal fun String.hasAnyFormatOf(vararg formats: FileFormat) = formats.any { hasFormat(it) }

internal enum class FileFormat (val extension: String){
    PDF(".pdf"),
    JPEG(".jpeg"),
    JPG(".jpg"),
    GIF(".gif"),
    PNG(".png"),
    DOC(".doc"),
    DOCX(".docx"),
    XLS(".xls"),
    XLSX(".xlsx"),
    RTF(".rtf"),
    TXT(".txt")
}