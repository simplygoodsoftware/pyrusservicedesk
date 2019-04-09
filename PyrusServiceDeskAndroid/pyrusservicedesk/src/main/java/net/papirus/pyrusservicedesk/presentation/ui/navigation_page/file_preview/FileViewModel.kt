package net.papirus.pyrusservicedesk.presentation.ui.navigation_page.file_preview

import android.net.Uri

internal class FileViewModel(val fileUri: Uri,
                             val isLocal: Boolean,
                             val canBePreviewed: Boolean,
                             val hasError: Boolean = false)