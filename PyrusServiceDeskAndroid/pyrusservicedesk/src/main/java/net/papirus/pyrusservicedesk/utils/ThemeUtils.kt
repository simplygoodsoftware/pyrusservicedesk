package net.papirus.pyrusservicedesk.utils

import android.content.Context
import android.graphics.drawable.Drawable
import android.support.annotation.ColorInt
import android.support.v4.content.ContextCompat
import com.example.pyrusservicedesk.R
import net.papirus.pyrusservicedesk.ServiceDeskActivity

internal class ThemeUtils{

    companion object {
        @ColorInt
        fun getAccentColor(activity: Context): Int {
            return when{
                ServiceDeskActivity.getStyle().themeColor != null -> ServiceDeskActivity.getStyle().themeColor!!
                else -> getColor(activity, R.attr.colorAccent)
            }
        }

        fun getTitle(context: Context): String {
            return when{
                !ServiceDeskActivity.getStyle().title.isNullOrEmpty() -> ServiceDeskActivity.getStyle().title!!
                else -> context.resources.getString(R.string.psd_organization_support)
            }
        }

        fun getWelcomeMessage(): String? = ServiceDeskActivity.getStyle().welcomeMessage

        fun getSupportAvatar(context: Context): Drawable {
            return when {
                ServiceDeskActivity.getStyle().supportAvatar != null -> ServiceDeskActivity.getStyle().supportAvatar!!
                else -> ContextCompat.getDrawable(context, R.drawable.psd_error)!!
            }
        }

    }
}

