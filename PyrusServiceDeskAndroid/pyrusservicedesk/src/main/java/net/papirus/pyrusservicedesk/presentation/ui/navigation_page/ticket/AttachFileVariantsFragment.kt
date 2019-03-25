package net.papirus.pyrusservicedesk.presentation.ui.navigation_page.ticket

import android.Manifest.permission.CAMERA
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.BottomSheetDialog
import android.support.design.widget.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.ViewGroup
import kotlinx.android.synthetic.main.psd_fragment_attach_file_variants.*
import net.papirus.pyrusservicedesk.utils.INTENT_IMAGE_TYPE
import net.papirus.pyrusservicedesk.utils.getViewModelWithActivityScope
import net.papirus.pyrusservicedesk.utils.hasPermission
import java.text.SimpleDateFormat
import java.util.*


internal class AttachFileVariantsFragment: BottomSheetDialogFragment(), View.OnClickListener {

    private companion object {
        const val REQUEST_CODE_PERMISSION = 0

        const val REQUEST_PICK_IMAGE = 1
        const val REQUEST_TAKE_PHOTO = 2
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
        gallery_variant.setOnClickListener(this)
        logs_variant.setOnClickListener(this)
        logs_variant.visibility = INVISIBLE
    }

    override fun onClick(view: View) {
        when (view) {
            photo_variant -> startTakingPhoto()
            gallery_variant -> startPickingImage()
            logs_variant -> sendLogs()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (!isExpectedResult(requestCode) || resultCode != RESULT_OK) {
            capturePhotoUri = null
            return
        }
        val isPhoto = requestCode == REQUEST_TAKE_PHOTO
        val location: Uri? = if (isPhoto) capturePhotoUri else data?.data
        location?.let {
            sharedModel.onFilePicked(it)
            dismiss()
        }
        capturePhotoUri = null
    }

    private fun sendLogs() {

    }

    private fun startPickingImage() {
        Intent(Intent.ACTION_GET_CONTENT).also{
            it.type = INTENT_IMAGE_TYPE
            startActivityForResult(it, REQUEST_PICK_IMAGE)
        }
    }

    private fun startTakingPhoto() {
        activity?.let { activity ->

            val permissionToAsk = mutableListOf<String>()
            if (!activity.hasPermission(WRITE_EXTERNAL_STORAGE))
                permissionToAsk.add(WRITE_EXTERNAL_STORAGE)
            if (!activity.hasPermission(CAMERA))
                permissionToAsk.add(CAMERA)

            if (permissionToAsk.isEmpty()) {
                val contentValues = ContentValues().also {
                    val timeStamp: String =
                        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                            .format(Date())
                    it.put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_$timeStamp.jpg")
                    it.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")
                    it.put(MediaStore.Images.Media.DATE_ADDED, timeStamp)
                }
                capturePhotoUri =
                        activity.contentResolver?.insert(
                            EXTERNAL_CONTENT_URI,
                            contentValues)
                Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, capturePhotoUri)
                    intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    intent.resolveActivity(activity.packageManager)?.also { componentName ->
                        activity.grantUriPermission(
                            componentName.packageName,
                            capturePhotoUri,
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        startActivityForResult(intent, REQUEST_TAKE_PHOTO)
                    }
                }
            }
            else
                requestPermissions(permissionToAsk.toTypedArray(), REQUEST_CODE_PERMISSION)
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

    private fun onPermissionsGranted(permissions: Array<String>) {
        when {
            activity?.let { it.hasPermission(WRITE_EXTERNAL_STORAGE) && it.hasPermission(CAMERA) } ?: false ->
                startTakingPhoto()
        }
    }

    private fun isExpectedResult(requestCode: Int): Boolean {
        return requestCode == REQUEST_TAKE_PHOTO || requestCode == REQUEST_PICK_IMAGE
    }
}