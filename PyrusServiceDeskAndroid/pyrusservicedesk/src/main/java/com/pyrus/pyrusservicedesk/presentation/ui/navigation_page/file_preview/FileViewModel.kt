package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.file_preview

import android.net.Uri

/**
 * Base class for file view model that is used by [FilePreviewActivity]
 *
 * @param fileUri uri of the file
 * @param hasError TRUE if there is an error occurred
 * @param isDownloading TRUE if file is being currently downloaded
 * @param isLocal TRUE if file download complete successfully. When TRUE, [fileUri] must point to a local file resource.
 */
internal sealed class FileViewModel(val fileUri: Uri,
                                    val hasError: Boolean,
                                    val isDownloading: Boolean ,
                                    val isLocal: Boolean)

/**
 * [FileViewModel] implementation that CAN be previewed by UI using fileUri
 */
internal class PreviewableFileViewModel(fileUri: Uri,
                                        hasError: Boolean = false,
                                        isDownloading: Boolean = false,
                                        isLocal: Boolean = false)
    : FileViewModel(fileUri, hasError, isDownloading, isLocal)

/**
 * [FileViewModel] implementation that CAN'T be previewed by UI using fileUri
 */
internal class NonPreviewableViewModel(fileUri: Uri,
                                       hasError: Boolean = false,
                                       isDownloading: Boolean = false,
                                       isLocal: Boolean = false)
    : FileViewModel(fileUri, hasError, isDownloading, isLocal)
