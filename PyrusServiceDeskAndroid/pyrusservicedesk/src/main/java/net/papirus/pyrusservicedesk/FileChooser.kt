package net.papirus.pyrusservicedesk

import android.content.Intent

/**
 * Interface should be implemented to be passed into [PyrusServiceDesk.registerFileChooser] to be able to
 * attach custom files with comments
 */
interface FileChooser {
    /**
     * Text that is shown as one of the available variants to pick an attachment from.
     */
    fun getLabel(): String

    /**
     * Intent that is used for launching custom file chooser when user clicks on the variant with the label, provided
     * by [getLabel]
     */
    fun getIntent(): Intent
}