package net.papirus.pyrusservicedesk.presentation.ui.navigation_page.ticket

import android.Manifest.permission.CAMERA
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.BottomSheetDialog
import android.support.design.widget.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import kotlinx.android.synthetic.main.psd_fragment_attach_file_variants.*
import net.papirus.pyrusservicedesk.PyrusServiceDesk
import net.papirus.pyrusservicedesk.utils.*

/**
 * UI that is used for attaching files to the comments.
 */
internal class AttachFileVariantsFragment: BottomSheetDialogFragment(), View.OnClickListener {

    private companion object {
        const val REQUEST_CODE_PERMISSION = 0
        const val REQUEST_CODE_CUSTOM_CHOOSER = 1
        const val REQUEST_CODE_PICK_IMAGE = 2
        const val REQUEST_CODE_TAKE_PHOTO = 3
    }

    private var capturePhotoUri: Uri? = null
    private val sharedModel: TicketSharedViewModel by getViewModelWithActivityScope(TicketSharedViewModel::class.java)

    @SuppressLint("InflateParams")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        dialog.setOnShowListener { dialog ->
            val d = dialog as BottomSheetDialog
            val bottomSheetInternal = d.findViewById<View>(android.support.design.R.id.design_bottom_sheet)
            BottomSheetBehavior.from(bottomSheetInternal!!).state = BottomSheetBehavior.STATE_EXPANDED
        }
        return inflater.inflate(com.example.pyrusservicedesk.R.layout.psd_fragment_attach_file_variants, null, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        photo_variant.setOnClickListener(this)
        photo_variant.visibility = if (isCapturingPhotoSupported()) VISIBLE else GONE
        gallery_variant.setOnClickListener(this)
        custom_variant.setOnClickListener(this)
        custom_variant.visibility = if (PyrusServiceDesk.FILE_CHOOSER == null) INVISIBLE else VISIBLE
    }

    override fun onClick(view: View) {
        when (view) {
            photo_variant -> startTakingPhoto()
            gallery_variant -> startPickingImage()
            custom_variant -> openCustomChooser()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (!isExpectedResult(requestCode) || resultCode != RESULT_OK) {
            capturePhotoUri = null
            return
        }
        val isPhoto = requestCode == REQUEST_CODE_TAKE_PHOTO
        val location: Uri? = if (isPhoto) capturePhotoUri else data?.data
        location?.let {
            sharedModel.onFilePicked(it)
            dismiss()
        }
        capturePhotoUri = null
    }

    private fun openCustomChooser() {
        val chooserIntent = PyrusServiceDesk.FILE_CHOOSER?.getIntent() ?: return
        activity?.packageManager?.resolveActivity(chooserIntent, 0) ?: return
        startActivityForResult(chooserIntent, REQUEST_CODE_CUSTOM_CHOOSER)
    }

    private fun startPickingImage() {
        Intent(Intent.ACTION_GET_CONTENT).also{
            it.type = MIME_TYPE_IMAGE_ANY
            startActivityForResult(it, REQUEST_CODE_PICK_IMAGE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_CODE_PERMISSION)
            return
        val granted = mutableListOf<String>()
        permissions.forEachIndexed {
                index, permission ->
            if (grantResults[index] == PackageManager.PERMISSION_GRANTED)
                granted.add(permission)
        }
        if (!granted.isEmpty())
            onPermissionsGranted(granted.toTypedArray())
    }

    private fun startTakingPhoto() {
        activity?.let { activity ->

            val permissionToAsk = mutableListOf<String>()
            if (!activity.hasPermission(WRITE_EXTERNAL_STORAGE))
                permissionToAsk.add(WRITE_EXTERNAL_STORAGE)
            if (!activity.hasPermission(CAMERA))
                permissionToAsk.add(CAMERA)

            if (permissionToAsk.isEmpty())
                capturePhotoUri = dispatchTakePhotoIntent(REQUEST_CODE_TAKE_PHOTO)
            else
                requestPermissions(permissionToAsk.toTypedArray(), REQUEST_CODE_PERMISSION)
        }
    }

    private fun onPermissionsGranted(permissions: Array<String>) {
        when {
            activity?.let { it.hasPermission(WRITE_EXTERNAL_STORAGE) && it.hasPermission(CAMERA) } ?: false ->
                startTakingPhoto()
        }
    }

    private fun isExpectedResult(requestCode: Int): Boolean {
        return when (requestCode){
            REQUEST_CODE_TAKE_PHOTO -> true
            REQUEST_CODE_PICK_IMAGE -> true
            REQUEST_CODE_CUSTOM_CHOOSER -> true
            else -> false
        }
    }
}