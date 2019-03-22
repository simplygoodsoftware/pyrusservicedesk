package net.papirus.pyrusservicedesk.utils

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.support.annotation.ColorInt
import android.support.v7.content.res.AppCompatResources
import com.example.pyrusservicedesk.R
import net.papirus.pyrusservicedesk.PyrusServiceDesk

internal class ThemeUtils{

    companion object {

        private const val SCALE_SUPPORT_ICON_DEFAULT = .5f

        @ColorInt
        fun getAccentColor(activity: Context): Int {
            return when{
                PyrusServiceDesk.getTheme().themeColor != null -> PyrusServiceDesk.getTheme().themeColor!!
                else -> getColor(activity, R.attr.colorAccent)
            }
        }

        fun getTitle(context: Context): String {
            return when{
                !PyrusServiceDesk.getTheme().title.isNullOrEmpty() -> PyrusServiceDesk.getTheme().title!!
                else -> context.resources.getString(R.string.psd_organization_support)
            }
        }

        fun getWelcomeMessage(): String? = PyrusServiceDesk.getTheme().welcomeMessage

        fun getSupportAvatar(context: Context): Drawable {
            return when {
                PyrusServiceDesk.getTheme().supportAvatar != null -> PyrusServiceDesk.getTheme().supportAvatar!!
                else -> makeSupportAvatar(context, AppCompatResources.getDrawable(context, R.drawable.psd_support_avatar)!!)
            }
        }

        private fun makeSupportAvatar(context: Context, drawable: Drawable): Drawable {
            val bmp = Bitmap.createBitmap(drawable.intrinsicHeight, drawable.intrinsicWidth, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val paint = Paint()
            paint.color = getAccentColor(context)
            paint.isAntiAlias = true
            canvas.drawCircle(canvas.width / 2f, canvas.height / 2f, canvas.height / 2f, paint)

            val icon = Bitmap.createBitmap(drawable.intrinsicHeight, drawable.intrinsicWidth, Bitmap.Config.ARGB_8888)
            val iconCanvas = Canvas(icon)
            drawable.setBounds(0, 0, iconCanvas.width, iconCanvas.height);
            drawable.draw(iconCanvas)

            paint.colorFilter = PorterDuffColorFilter(getTextColorOnBackground(context, paint.color), PorterDuff.Mode.SRC_IN)
            val iconTranslate = (canvas.width - canvas.width*SCALE_SUPPORT_ICON_DEFAULT) / 2

            canvas.drawBitmap(
                icon,
                Matrix().apply {
                    postScale(SCALE_SUPPORT_ICON_DEFAULT, SCALE_SUPPORT_ICON_DEFAULT)
                    postTranslate(iconTranslate, iconTranslate)
                },
                paint)


            return BitmapDrawable(context.resources, bmp)
        }
    }
}

