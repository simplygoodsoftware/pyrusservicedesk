package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.dialogs.attach_files

import android.Manifest.permission.CAMERA
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.util.Consumer
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.injector
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk._ref.utils.dispatchTakePhotoIntent
import com.pyrus.pyrusservicedesk._ref.utils.dispatchTakeVideoIntent
import com.pyrus.pyrusservicedesk._ref.utils.hasPermission
import com.pyrus.pyrusservicedesk._ref.utils.hasPermissionInManifeset
import com.pyrus.pyrusservicedesk._ref.utils.insets.RootViewDeferringInsetsCallback
import com.pyrus.pyrusservicedesk._ref.utils.log.PLog
import com.pyrus.pyrusservicedesk.core.StaticRepository
import com.pyrus.pyrusservicedesk.databinding.PsdFragmentAttachFileVariantsBinding
import java.io.File

/**
 * UI that is used for attaching files to the comments.
 */
internal class AttachFileVariantsFragment: BottomSheetDialogFragment(), View.OnClickListener {

    private var capturePhotoUri: Uri? = null

    private val logSubscriber: Consumer<File> = Consumer {
        val uri = Uri.fromFile(it)
        sendResultAndClose(uri)
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
            val bottomSheetInternal = d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            BottomSheetBehavior.from(bottomSheetInternal!!).state = BottomSheetBehavior.STATE_EXPANDED
        }
        binding = PsdFragmentAttachFileVariantsBinding.inflate(inflater)
        binding.root.setBackgroundColor(ConfigUtils.getFileMenuBackgroundColor(inflater.context))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val deferringInsetsListener = RootViewDeferringInsetsCallback(
            persistentInsetTypes = WindowInsetsCompat.Type.systemBars(),
            deferredInsetTypes = WindowInsetsCompat.Type.ime()
        )
        ViewCompat.setOnApplyWindowInsetsListener(binding.root, deferringInsetsListener)

        binding.videoVariant.setOnClickListener(this)
        binding.photoVariant.setOnClickListener(this)
        binding.galleryVariant.setOnClickListener(this)
        binding.customVariant.visibility = if (StaticRepository.FILE_CHOOSER != null) VISIBLE else GONE
        StaticRepository.FILE_CHOOSER?.let {
            binding.customVariant.setOnClickListener(this)
            binding.customVariant.text = it.getLabel()
        }

        binding.sendLogsVariant.visibility = if (StaticRepository.logging) VISIBLE else GONE
        if (StaticRepository.logging) binding.sendLogsVariant.setOnClickListener(this)
        else binding.sendLogsVariant.setOnClickListener(null)

        val textColor = ConfigUtils.getFileMenuTextColor(requireContext())
        binding.videoVariant.setTextColor(textColor)
        binding.photoVariant.setTextColor(textColor)
        binding.galleryVariant.setTextColor(textColor)
        binding.sendLogsVariant.setTextColor(textColor)

        ConfigUtils.getMainFontTypeface()?.let {
            binding.videoVariant.typeface = it
            binding.photoVariant.typeface = it
            binding.galleryVariant.typeface = it
            binding.sendLogsVariant.typeface = it
        }

        binding.backgroundView.setOnClickListener {
            dismiss()
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
            binding.videoVariant -> startTakingVideo()
            binding.photoVariant -> startTakingPhoto()
            binding.galleryVariant -> startPickingImage()
            binding.customVariant -> openCustomChooser()
            binding.sendLogsVariant -> PLog.collectLogs()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (!isExpectedResult(requestCode) || resultCode != RESULT_OK) {
            capturePhotoUri = null
            return
        }
        val isPhoto = requestCode == REQUEST_CODE_TAKE_PHOTO
        val isVideo = requestCode == REQUEST_CODE_TAKE_VIDEO
        val location: Uri? = if (isPhoto || isVideo) capturePhotoUri else data?.data
        if (location != null) {
            if (!isPhoto && !isVideo && requestCode != REQUEST_CODE_CUSTOM_CHOOSER) {
                context?.contentResolver?.takePersistableUriPermission(
                    location,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            sendResultAndClose(location)
        }
        capturePhotoUri = null
    }

    private fun sendResultAndClose(uri: Uri) {
        val router = injector().router
        router.sendResult(requireArguments().getString(RESULT_KEY)!!, uri)
        dismiss()
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
            // We forced to use Intent.ACTION_OPEN_DOCUMENT to save uri access when the application
            // is restarted
            it.addCategory(Intent.CATEGORY_OPENABLE)
            it.action = Intent.ACTION_OPEN_DOCUMENT
            it.flags = it.flags or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            it.type = "*/*"
            startActivityForResult(it, REQUEST_CODE_PICK_IMAGE)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_CODE_PERMISSION && requestCode != REQUEST_CODE_PERMISSION_VIDEO)
            return
        val granted = mutableListOf<String>()
        permissions.forEachIndexed {
                index, permission ->
            if (grantResults[index] == PackageManager.PERMISSION_GRANTED)
                granted.add(permission)
        }
        if (granted.isNotEmpty())
            onPermissionsGranted(requestCode == REQUEST_CODE_PERMISSION_VIDEO)
    }

    private fun startTakingVideo() {
        context?.let {
            /*
             * Note: if you app targets {@link android.os.Build.VERSION_CODES#M M} and above
             * and declares as using the {@link android.Manifest.permission#CAMERA} permission which
             * is not granted, then attempting to use this action will result in a {@link
             * java.lang.SecurityException}.
             * https://developer.android.com/reference/android/provider/MediaStore#ACTION_IMAGE_CAPTURE
             */
            if (it.hasPermissionInManifeset(CAMERA) && it.hasPermission(CAMERA).not())
                requestPermissions(arrayOf(CAMERA), REQUEST_CODE_PERMISSION_VIDEO)
            else
                capturePhotoUri = dispatchTakeVideoIntent(REQUEST_CODE_TAKE_VIDEO)
        }
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

    private fun onPermissionsGranted(isVideo: Boolean) {
        activity?.let {
            if (it.hasPermission(CAMERA)) {
                if (isVideo) startTakingVideo()
                else startTakingPhoto()
            }
        }
    }

    private fun isExpectedResult(requestCode: Int): Boolean {
        return when (requestCode) {
            REQUEST_CODE_TAKE_PHOTO -> true
            REQUEST_CODE_TAKE_VIDEO -> true
            REQUEST_CODE_PICK_IMAGE -> true
            REQUEST_CODE_CUSTOM_CHOOSER -> true
            else -> false
        }
    }

    internal companion object {
        private const val REQUEST_CODE_PERMISSION = 0
        private const val REQUEST_CODE_CUSTOM_CHOOSER = 1
        private const val REQUEST_CODE_PICK_IMAGE = 2
        private const val REQUEST_CODE_TAKE_PHOTO = 3
        private const val REQUEST_CODE_TAKE_VIDEO = 4
        private const val REQUEST_CODE_PERMISSION_VIDEO = 5

        private const val STATE_KEY_PHOTO_URI = "STATE_KEY_PHOTO_URI"
        private const val RESULT_KEY = "RESULT_KEY"

        fun newInstance(key: String): AttachFileVariantsFragment {
            val fragment = AttachFileVariantsFragment()
            fragment.arguments = bundleOf(RESULT_KEY to key)
            return fragment
        }
    }

}