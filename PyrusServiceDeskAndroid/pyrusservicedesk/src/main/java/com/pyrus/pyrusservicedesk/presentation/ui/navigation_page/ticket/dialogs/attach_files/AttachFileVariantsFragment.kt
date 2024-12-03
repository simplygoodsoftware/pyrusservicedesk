package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.dialogs.attach_files

import android.Manifest.permission.CAMERA
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.util.Consumer
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.core.StaticRepository
import com.pyrus.pyrusservicedesk.databinding.PsdFragmentAttachFileVariantsBinding
import com.pyrus.pyrusservicedesk._ref.utils.log.PLog
import com.pyrus.pyrusservicedesk._ref.utils.*
import java.io.File

/**
 * UI that is used for attaching files to the comments.
 */
internal class AttachFileVariantsFragment: BottomSheetDialogFragment(), View.OnClickListener {

    private var capturePhotoUri: Uri? = null
    private val sharedModel: AttachFileSharedViewModel by getViewModelWithActivityScope(
        AttachFileSharedViewModel::class.java)

    private val logSubscriber: Consumer<File> = Consumer {
        if (it == null)
            return@Consumer
        val uri = Uri.fromFile(it)
        sharedModel.onFilePicked(uri)
        dismiss()
    }

    private lateinit var binding: PsdFragmentAttachFileVariantsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        capturePhotoUri = savedInstanceState?.getParcelable(STATE_KEY_PHOTO_URI)
    }

    @SuppressLint("InflateParams")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        dialog?.setOnShowListener { dialog ->
            val d = dialog as BottomSheetDialog
            val bottomSheetInternal = d.findViewById<View>(R.id.design_bottom_sheet)
            BottomSheetBehavior.from(bottomSheetInternal!!).state = BottomSheetBehavior.STATE_EXPANDED
        }
        binding = PsdFragmentAttachFileVariantsBinding.inflate(inflater, null, false)
        binding.root.setBackgroundColor(ConfigUtils.getFileMenuBackgroundColor(inflater.context))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.photoVariant.setOnClickListener(this)
        binding.photoVariant.visibility = if (isCapturingPhotoSupported()) VISIBLE else GONE
        binding.galleryVariant.setOnClickListener(this)
        binding.customVariant.visibility = if (StaticRepository.FILE_CHOOSER != null) VISIBLE else GONE
        StaticRepository.FILE_CHOOSER?.let {
            binding.customVariant.setOnClickListener(this)
            binding.customVariant.text = it.getLabel()
        }
        // TODO sds
//        binding.sendLogsVariant.visibility = if (PyrusServiceDesk.logging) VISIBLE else GONE
//        if (PyrusServiceDesk.logging)
//            binding.sendLogsVariant.setOnClickListener(this)

        val textColor = ConfigUtils.getFileMenuTextColor(requireContext())
        binding.photoVariant.setTextColor(textColor)
        binding.galleryVariant.setTextColor(textColor)
        binding.sendLogsVariant.setTextColor(textColor)

        ConfigUtils.getMainFontTypeface()?.let {
            binding.photoVariant.typeface = it
            binding.galleryVariant.typeface = it
            binding.sendLogsVariant.typeface = it
        }
    }

    override fun onStart() {
        super.onStart()
        PLog.addLogSubscriber(logSubscriber)
    }

    override fun onStop() {
        PLog.removeLogSubscriber(logSubscriber)
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(STATE_KEY_PHOTO_URI, capturePhotoUri)
    }

    override fun onClick(view: View) {
        when (view) {
            binding.photoVariant -> startTakingPhoto()
            binding.galleryVariant -> startPickingImage()
            binding.customVariant -> openCustomChooser()
            binding.sendLogsVariant -> PLog.collectLogs()
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
            if (!isPhoto
                && requestCode != REQUEST_CODE_CUSTOM_CHOOSER
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

                context?.contentResolver?.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            sharedModel.onFilePicked(it)
            dismiss()
        }
        capturePhotoUri = null
    }

    private fun openCustomChooser() {
        val chooserIntent = StaticRepository.FILE_CHOOSER?.getIntent() ?: return
        activity?.packageManager?.resolveActivity(chooserIntent, 0) ?: return
        startActivityForResult(chooserIntent,
            REQUEST_CODE_CUSTOM_CHOOSER
        )
    }

    private fun startPickingImage() {
        Intent().also{
            it.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                // We forced to use Intent.ACTION_OPEN_DOCUMENT to save uri access when the application
                // is restarted
                it.action = Intent.ACTION_OPEN_DOCUMENT
                it.flags = it.flags or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            }
            else{
                it.action = Intent.ACTION_GET_CONTENT
            }
            it.type = com.pyrus.pyrusservicedesk._ref.utils.MIME_TYPE_IMAGE_ANY
            startActivityForResult(it,
                REQUEST_CODE_PICK_IMAGE
            )
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
        if (granted.isNotEmpty())
            onPermissionsGranted(granted.toTypedArray())
    }

    private fun startTakingPhoto() {
        context?.let {
            /*
             * Note: if you app targets {@link android.os.Build.VERSION_CODES#M M} and above
             * and declares as using the {@link android.Manifest.permission#CAMERA} permission which
             * is not granted, then attempting to use this action will result in a {@link
             * java.lang.SecurityException}.
             * https://developer.android.com/reference/android/provider/MediaStore#ACTION_IMAGE_CAPTURE
             */
            if (it.hasPermissionInManifeset(CAMERA) && it.hasPermission(CAMERA).not())
                requestPermissions(arrayOf(CAMERA), REQUEST_CODE_PERMISSION)
            else
                capturePhotoUri = dispatchTakePhotoIntent(REQUEST_CODE_TAKE_PHOTO)
        }
    }

    private fun onPermissionsGranted(permissions: Array<String>) {
        activity?.let {
            if (it.hasPermission(CAMERA))
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

    private companion object {
        const val REQUEST_CODE_PERMISSION = 0
        const val REQUEST_CODE_CUSTOM_CHOOSER = 1
        const val REQUEST_CODE_PICK_IMAGE = 2
        const val REQUEST_CODE_TAKE_PHOTO = 3

        const val STATE_KEY_PHOTO_URI = "STATE_KEY_PHOTO_URI"
    }

}