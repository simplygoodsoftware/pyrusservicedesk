package net.papirus.pyrusservicedesk.utils

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.ThumbnailUtils
import com.example.pyrusservicedesk.R
import com.squareup.picasso.Transformation
import net.papirus.pyrusservicedesk.sdk.data.Author

internal val CIRCLE_TRANSFORMATION = object : Transformation {
    override fun key(): String = "Circle"
    override fun transform(source: Bitmap): Bitmap = source.circle()
}

internal fun getSimpleAvatar(context: Context, author: Author): Drawable {
    return BitmapDrawable(
        context.resources,
        getSquareAvatar(
            context,
            Color.parseColor(author.avatarColorString),
            author.getInitials())
            .circle())
}

/**
 * Makes circled bitmap from the source. It makes new square bitmap 96x96 px from the center of the source
 * and inscribe it into an oval. If the source image is less than 96x96, it will be stretched.
 * @return new 96x96 px bitmap with circle image
 */
internal fun Bitmap.circle(): Bitmap {
    val dimension = this.width
    val scaled = ThumbnailUtils.extractThumbnail(this , dimension, dimension, ThumbnailUtils.OPTIONS_RECYCLE_INPUT)
    val output = Bitmap.createBitmap(
        scaled.width,
        scaled.height, Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(output)
    val color = Color.RED
    val paint = Paint()
    val rect = Rect(0, 0, scaled.width, scaled.height)
    val rectF = RectF(rect)
    paint.isAntiAlias = true
    canvas.drawARGB(0, 0, 0, 0)
    paint.color = color
    canvas.drawOval(rectF, paint)
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(scaled, rect, rect, paint)
    scaled.recycle()
    return output
}

private fun getSquareAvatar(context: Context, color: Int, text: String): Bitmap {
    val dimension = context.resources.getDimensionPixelSize(R.dimen.psd_avatar_bitmap_size)
    val bitmap = Bitmap.createBitmap(dimension, dimension, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.style = Paint.Style.FILL
    paint.color = Color.WHITE
    paint.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
    paint.textAlign = Paint.Align.CENTER
    paint.textSize = context.resources.getDimension(R.dimen.psd_avatar_text_size)
    canvas.drawColor(color)
    val y = canvas.height / 2 - (paint.descent() + paint.ascent()) / 2
    canvas.drawText(text, (canvas.width / 2).toFloat(), y, paint)
    return bitmap
}
