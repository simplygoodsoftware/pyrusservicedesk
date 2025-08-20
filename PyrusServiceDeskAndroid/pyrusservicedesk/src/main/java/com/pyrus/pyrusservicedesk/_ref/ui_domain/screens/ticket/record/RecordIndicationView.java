package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.record;

import static com.pyrus.pyrusservicedesk.utils.UiUtils.dpToPx;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pyrus.pyrusservicedesk.R;


/**
 * View which represent audio recording.
 */
public class RecordIndicationView extends View {

    private static final float BAR_QUANTITY = 45f;

    private final Paint bgPaint;
    private final Paint paint;
    private final RectF rect = new RectF();

    private final int[] colorsBackground;
    private final int colorBar;
    private final float barMinHeight;
    private final float gap;

    private float width = 0f;
    private float height = 0f;
    private short[] recordedSegmentValues;

    public RecordIndicationView(Context context) {
        this(context, null);
    }

    public RecordIndicationView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @SuppressLint("ResourceType")
    public RecordIndicationView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        colorsBackground = new int[] {
            context.getResources().getColor(R.color.psd_bg_record_gradient_1, context.getTheme()),
            context.getResources().getColor(R.color.psd_bg_record_gradient_2, context.getTheme()),
            context.getResources().getColor(R.color.psd_bg_record_gradient_3, context.getTheme())
        };

        colorBar = Color.WHITE;
        barMinHeight = dpToPx(1f);
        gap = dpToPx(1f);
        setPivotX(0f);
        setPivotY(0f);
    }

    /**
     * Sets values which are used to show recording progress.
     *
     * @param recordedSegmentValues Array with recorded segment values.
     */
    public void setRecordedSegmentValues(short[] recordedSegmentValues) {
        this.recordedSegmentValues = recordedSegmentValues;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        width = w;
        height = h;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        drawBackground(canvas, width, height);
        drawBars(canvas, width, height);
    }

    private void drawBackground(Canvas canvas, float width, float height) {

        float[] gradientPoints = {0, 0.5f, 1f};
        bgPaint.setShader(new LinearGradient(
            0,
            0,
            width,
            0,
            colorsBackground,
            gradientPoints,
            Shader.TileMode.MIRROR
        ));
        bgPaint.setStyle(Paint.Style.FILL);
        rect.set(0, 0, width, height);
        final float radius = rect.height() / 2f;
        canvas.drawRoundRect(rect, radius, radius, bgPaint);
    }

    private void drawBars(Canvas canvas, float width, float height) {
        if (recordedSegmentValues == null) {
            return;
        }

        paint.setColor(colorBar);
        final float horizontalPadding = width * 0.1f;
        final float barWidth = (width - horizontalPadding * 2f) / BAR_QUANTITY;
        final float numberOfShowedValues = recordedSegmentValues.length / BAR_QUANTITY;
        paint.setStrokeWidth(barWidth - gap);
        final float startX = horizontalPadding + (barWidth / 2f);
        final float heightHalf = height / 2f;

        for (int i = 0; i < BAR_QUANTITY; i++) {
            float barX = startX + (i * barWidth);

            int shortPosition = (int) Math.ceil(i * numberOfShowedValues);
            final float f = (float) (Math.sqrt((Math.abs(recordedSegmentValues[shortPosition])) / 32768f) * 0.9f);
            float barHeight = heightHalf * f;

            canvas.drawLine(barX, heightHalf, barX, heightHalf - (barHeight + barMinHeight), paint);
            canvas.drawLine(barX, heightHalf, barX, heightHalf + (barHeight + barMinHeight), paint);
        }
    }

}
