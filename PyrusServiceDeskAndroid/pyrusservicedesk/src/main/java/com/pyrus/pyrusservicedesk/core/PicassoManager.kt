package com.pyrus.pyrusservicedesk.core

import android.app.Application
import com.pyrus.pyrusservicedesk.log.PLog
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File

class PicassoManager(private val appContext: Application) {

    fun providePicasso(okHttpClientBuilder : OkHttpClient.Builder): Picasso {
        val cacheDir = File(appContext.cacheDir, "image-cache")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        val cacheSize = 50 * 1024 * 1024L // 50 MB
        val cache = Cache(cacheDir, cacheSize)
        return Picasso.Builder(appContext)
            .downloader(OkHttp3Downloader(okHttpClientBuilder.cache(cache).build()))
            .build()
    }

    fun dispose(picasso: Picasso) {
        picasso.cancelTag(this)
        picasso.shutdown()
        clearPicassoCache()
    }

    private fun clearPicassoCache() {
        try {
            val cacheDir = File(appContext.cacheDir, "image-cache")
            if (cacheDir.exists()) {
                cacheDir.deleteRecursively()
            }
        } catch (e: SecurityException) {
            PLog.e(TAG, "Failed to clear cache", e)
        }
    }

    companion object {
        private const val TAG = "PicassoManager"
    }


}