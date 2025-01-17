package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.dialogs.attach_files

import android.Manifest.permission.CAMERA
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
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.injector
import com.pyrus.pyrusservicedesk._ref.utils.BottomSheetFragment
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk._ref.utils.MIME_TYPE_IMAGE_ANY
import com.pyrus.pyrusservicedesk._ref.utils.dispatchTakePhotoIntent
import com.pyrus.pyrusservicedesk._ref.utils.hasPermission
import com.pyrus.pyrusservicedesk._ref.utils.hasPermissionInManifeset
import com.pyrus.pyrusservicedesk._ref.utils.insets.RootViewDeferringInsetsCallback
import com.pyrus.pyrusservicedesk._ref.utils.isCapturingPhotoSupported
import com.pyrus.pyrusservicedesk._ref.utils.log.PLog
import com.pyrus.pyrusservicedesk.core.StaticRepository
import com.pyrus.pyrusservicedesk.databinding.PsdFragmentAttachFileVariantsBinding
import java.io.File

/**
 * UI that is used for attaching files to the comments.
 */
internal class AttachFileVariantsFragment: BottomSheetFragment(), View.OnClickListener {

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

    override fun onCreateBottomSheetView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = PsdFragmentAttachFileVariantsBinding.inflate(inflater, container, false)
        binding.content.setBackgroundColor(ConfigUtils.getFileMenuBackgroundColor(inflater.context))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val deferringInsetsListener = RootViewDeferringInsetsCallback(
            persistentInsetTypes = WindowInsetsCompat.Type.systemBars(),
            deferredInsetTypes = WindowInsetsCompat.Type.ime()
        )
        ViewCompat.setOnApplyWindowInsetsListener(binding.root, deferringInsetsListener)

        binding.photoVariant.setOnClickListener(this)
        binding.photoVariant.visibility = if (isCapturingPhotoSupported()) VISIBLE else GONE
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
        binding.photoVariant.setTextColor(textColor)
        binding.galleryVariant.setTextColor(textColor)
        binding.sendLogsVariant.setTextColor(textColor)

        ConfigUtils.getMainFontTypeface()?.let {
            binding.photoVariant.typeface = it
            binding.galleryVariant.typeface = it
            binding.sendLogsVariant.typeface = it
        }

        binding.backgroundView.setOnClickListener {
            closeFragment()
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

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (!isExpectedResult(requestCode) || resultCode != RESULT_OK) {
            capturePhotoUri = null
            return
        }
        val isPhoto = requestCode == REQUEST_CODE_TAKE_PHOTO
        val location: Uri? = if (isPhoto) capturePhotoUri else data?.data
        if (location != null) {
            if (!isPhoto && requestCode != REQUEST_CODE_CUSTOM_CHOOSER) {
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
        router.exit()
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
            it.action = Intent.ACTION_OPEN_DOCUMENT
            it.flags = it.flags or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            it.type = MIME_TYPE_IMAGE_ANY
            startActivityForResult(it, REQUEST_CODE_PICK_IMAGE)
        }
    }

    @Deprecated("Deprecated in Java")
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
            onPermissionsGranted()
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

    private fun onPermissionsGranted() {
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

    internal companion object {
        private const val REQUEST_CODE_PERMISSION = 0
        private const val REQUEST_CODE_CUSTOM_CHOOSER = 1
        private const val REQUEST_CODE_PICK_IMAGE = 2
        private const val REQUEST_CODE_TAKE_PHOTO = 3

        private const val STATE_KEY_PHOTO_URI = "STATE_KEY_PHOTO_URI"
        private const val RESULT_KEY = "RESULT_KEY"

        fun newInstance(key: String): AttachFileVariantsFragment {
            val fragment = AttachFileVariantsFragment()
            fragment.arguments = bundleOf(RESULT_KEY to key)
            return fragment
        }
    }

}