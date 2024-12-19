package com.pyrus.pyrusservicedesk._ref.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.pyrus.pyrusservicedesk.presentation.viewmodel.ViewModelFactory
import java.io.File
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

private const val DATE_FORMAT_CAPTURE_IMAGES = "yyyyMMdd_HHmmss"

/**
 * Lazily provides view model.
 */
internal fun <T : ViewModel> FragmentActivity.getViewModel(viewModelClass: Class<T>): Lazy<T> {

    return lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProvider(this, ViewModelFactory(intent, application))[viewModelClass]
    }
}

/**
 * Lazily provides view model with activity scope. Such view model can be used as event bus within single activity
 */
internal fun <T : ViewModel> Fragment.getViewModelWithActivityScope(viewModelClass: Class<T>): Lazy<T> {
    return lazy(LazyThreadSafetyMode.NONE) { activity!!.getViewModel(viewModelClass).value }
}

/**
 * Provides the name of the application. Not an extension over the [Context] as may be used
 * with [AndroidViewModel.getApplication] which is surprisingly returns an <T extends Application> an can't be
 * used as receiver without being explicitly cast.
 */
internal fun getApplicationName(context: Context): String = context.getString(context.applicationInfo.labelRes)

/**
 * Dispatches event for taking photo that should be handled by the receiver fragment.
 */
internal fun Fragment.dispatchTakePhotoIntent(requestCode: Int): Uri? {
    val capturePhotoUri = activity?.let {
        when {
            isCapturingPhotoSupported() ->it.createPhotoUriApi16AndAbove()
            else -> null
        }
    }
    capturePhotoUri?.let {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
            intent.putExtra(MediaStore.EXTRA_OUTPUT, it)
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            intent.resolveActivity(activity!!.packageManager)?.let {
                startActivityForResult(intent, requestCode)
            }
        }
    }
    return capturePhotoUri
}

private fun Context.createPhotoUriApi16AndAbove(): Uri? {
    val timeStamp: String =
        SimpleDateFormat(DATE_FORMAT_CAPTURE_IMAGES, Locale.getDefault())
            .format(Date())
    val fileName = "IMG_$timeStamp.jpg"

    val mediaStorageDir = getExternalFilesDir(Environment.DIRECTORY_DCIM) ?: return null

    if (!mediaStorageDir.exists()) {
        return null
    }

    // Return the file target for the photo based on filename
    val file = File(mediaStorageDir.path + File.separator + fileName)

    if (!file.createNewFile()) {
        return null
    }

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
        try {
            FileProvider.getUriForFile(this, "${packageName}.com.pyrus.pyrusservicedesk.sdk.PSDFileProvider", file)
        }
        catch (e: Exception) {
            return null
        }
    else {
        Uri.fromFile(file)
    }
}