package com.pyrus.pyrusservicedesk.presentation.ui.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils

internal object LinkUtils {

    fun createClickableSpan(url: String, context: Context, text: String? = null): ClickableSpan {
        return object : ClickableSpan() {

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true
                ds.color = ConfigUtils.getAccentColor(context)
            }

            override fun onClick(widget: View) {
                if (isLinkSafe(url, text)) {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW).apply { data = Uri.parse(url) })
                    }
                    catch (exception: Exception) {
                        exception.printStackTrace()
                    }
                }
                else {
                    showLinkDialog(context as Activity, url) {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW).apply { data = Uri.parse(url) })
                        }
                        catch (exception: Exception) {
                            exception.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    private fun String.removeProtocol(): String = this
        .removePrefix("https://")
        .removePrefix("http://")
        .removeSuffix("/")

    private fun isLinkSafe(url: String, text: String?): Boolean {
        val trustedUrls = ConfigUtils.getTrustedUrls() ?: emptyList()
        return text == url
            || trustedUrls.any { url.removeProtocol().startsWith(it.removeProtocol()) }
            || text == null
    }

    private fun showLinkDialog(context: Activity, url: String, onClick: ()-> Unit) {
        val message = SpannableStringBuilder(context.getString(R.string.link_warning, url))
        val range = Regex(url).find(message)?.range
        range?.let {
            if (range.first != -1 && range.last != -1 && range.last > range.first) {
                message.setSpan(createClickableSpan(url, context), range.first, range.last + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }

        AlertDialog.Builder(context)
            .setPositiveButton(R.string.psd_open) { _, _ -> onClick.invoke() }
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .setMessage(message)
            .create()
            .show()
    }

}