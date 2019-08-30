package com.pyrus.pyrusservicedesk.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.pyrus.pyrusservicedesk.presentation.viewmodel.ViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

private const val DATE_FORMAT_CAPTURE_IMAGES = "yyyyMMdd_HHmmss"

/**
 * Lazily provides view model.
 */
internal fun <T : ViewModel> androidx.fragment.app.FragmentActivity.getViewModel(viewModelClass: Class<T>): Lazy<T> {

    return lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProviders.of(
            this,
            ViewModelFactory(intent)
        ).get(viewModelClass)
    }
}

/**
 * Lazily provides view model with activity scope. Such view model can be used as event bus within single activity
 */
internal fun <T : ViewModel> androidx.fragment.app.Fragment.getViewModelWithActivityScope(viewModelClass: Class<T>): Lazy<T> {
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
internal fun androidx.fragment.app.Fragment.dispatchTakePhotoIntent(requestCode: Int): Uri? {
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
    val contentValues = ContentValues().also {
        val timeStamp: String =
            SimpleDateFormat(DATE_FORMAT_CAPTURE_IMAGES, Locale.getDefault())
                .format(Date())
        it.put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_$timeStamp.jpg")
        it.put(MediaStore.Images.Media.MIME_TYPE, MIME_TYPE_IMAGE_JPEG)
        it.put(MediaStore.Images.Media.DATE_ADDED, timeStamp)
    }
    return contentResolver?.insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues)
}