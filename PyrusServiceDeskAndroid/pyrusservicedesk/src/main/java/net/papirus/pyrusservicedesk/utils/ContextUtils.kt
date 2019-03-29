package net.papirus.pyrusservicedesk.utils

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProviders
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import net.papirus.pyrusservicedesk.presentation.viewmodel.ViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

private const val DATE_FORMAT_CAPTURE_IMAGES = "yyyyMMdd_HHmmss"

internal fun <T : ViewModel> FragmentActivity.getViewModel(viewModelClass: Class<T>): Lazy<T> {

    return lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProviders.of(
            this,
            ViewModelFactory(intent)
        ).get(viewModelClass)
    }
}

internal fun <T : ViewModel> Fragment.getViewModelWithActivityScope(viewModelClass: Class<T>): Lazy<T> {
    return lazy(LazyThreadSafetyMode.NONE) { activity!!.getViewModel(viewModelClass).value }
}

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
            intent.resolveActivity(activity!!.packageManager)?.also { componentName ->
                activity!!.grantUriPermission(
                    componentName.packageName,
                    it,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
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