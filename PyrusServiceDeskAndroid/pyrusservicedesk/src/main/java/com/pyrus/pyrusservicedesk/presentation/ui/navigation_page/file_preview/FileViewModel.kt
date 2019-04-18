package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.file_preview

import android.net.Uri

/**
 * Base class for file view model that is used by [FilePreviewActivity]
 *
 * @param fileUri uri of the file
 * @param isPreviewable TRUE if uri's content can be previewed
 * @param hasError TRUE if there is an error occurred
 * @param isDownloading TRUE if file is being currently downloaded
 * @param isLocal TRUE if file download complete successfully. When TRUE, [fileUri] must point to a local file resource.
 */
internal data class FileViewModel(val fileUri: Uri,
                                  val isPreviewable: Boolean,
                                  val hasError: Boolean = false,
                                  val isDownloading: Boolean = false,
                                  val isLocal: Boolean = false)
