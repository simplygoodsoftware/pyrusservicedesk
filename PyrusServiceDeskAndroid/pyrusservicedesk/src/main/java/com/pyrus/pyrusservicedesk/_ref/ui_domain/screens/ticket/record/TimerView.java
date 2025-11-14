package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.record;

import static com.pyrus.pyrusservicedesk.utils.UiExtKt.dp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils;

public class TimerView extends View {
    private boolean isRunning;
    private String oldString;
    private long startTime;
    private long stopTime;
    private long lastSendTypingTime;

    private final SpannableStringBuilder replaceIn = new SpannableStringBuilder();
    private final SpannableStringBuilder replaceOut = new SpannableStringBuilder();
    private SpannableStringBuilder replaceStable = new SpannableStringBuilder();

    private StaticLayout inLayout;
    private StaticLayout outLayout;

    private float replaceTransition;

    private TextPaint textPaint;
    private final float replaceDistance = dp(15f);

    public TimerView(Context context) {
        super(context);
    }

    public TimerView(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public TimerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void start(long milliseconds) {
        isRunning = true;
        startTime = System.currentTimeMillis() - milliseconds;
        lastSendTypingTime = startTime;
        invalidate();
    }

    public void stop() {
        if (isRunning) {
            isRunning = false;
            if (startTime > 0) {
                stopTime = System.currentTimeMillis();
            }
            invalidate();
        }
        lastSendTypingTime = 0;
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if (textPaint == null) {
            textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setTextSize(dp(14));
//                textPaint.setTypeface(AndroidUtilities.bold());
            textPaint.setColor(ConfigUtils.getSecondaryColorOnMainBackground(getContext()));
        }
        long currentTimeMillis = System.currentTimeMillis();
        long t = isRunning ? (currentTimeMillis - startTime) : stopTime - startTime;
        long time = t / 1000;
        int ms = (int) (t % 1000L) / 10;

        if (isRunning && currentTimeMillis > lastSendTypingTime + 5000) {
            lastSendTypingTime = currentTimeMillis;
        }

        String newString = formatTimerDurationFast((int) time, ms);
        if (newString.length() >= 3 && oldString != null && oldString.length() >= 3 && newString.length() == oldString.length() && newString.charAt(newString.length() - 3) != oldString.charAt(newString.length() - 3)) {
            int n = newString.length();

            replaceIn.clear();
            replaceOut.clear();
            replaceStable.clear();
            replaceIn.append(newString);
            replaceOut.append(oldString);
            replaceStable.append(newString);

            int inLast = -1;
            int inCount = 0;
            int outLast = -1;
            int outCount = 0;


            for (int i = 0; i < n - 1; i++) {
                if (oldString.charAt(i) != newString.charAt(i)) {
                    if (outCount == 0) {
                        outLast = i;
                    }
                    outCount++;

                    if (inCount != 0) {
                        EmptyStubSpan span = new EmptyStubSpan();
                        if (i == n - 2) {
                            inCount++;
                        }
                        replaceIn.setSpan(span, inLast, inLast + inCount, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        replaceOut.setSpan(span, inLast, inLast + inCount, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        inCount = 0;
                    }
                }
                else {
                    if (inCount == 0) {
                        inLast = i;
                    }
                    inCount++;
                    if (outCount != 0) {
                        replaceStable.setSpan(new EmptyStubSpan(), outLast, outLast + outCount, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        outCount = 0;
                    }
                }
            }

            if (inCount != 0) {
                EmptyStubSpan span = new EmptyStubSpan();
                replaceIn.setSpan(span, inLast, inLast + inCount + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                replaceOut.setSpan(span, inLast, inLast + inCount + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (outCount != 0) {
                replaceStable.setSpan(new EmptyStubSpan(), outLast, outLast + outCount, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            inLayout = new StaticLayout(replaceIn, textPaint, getMeasuredWidth(), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            outLayout = new StaticLayout(replaceOut, textPaint, getMeasuredWidth(), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);

            replaceTransition = 1f;
        }
        else {
            if (replaceStable == null) {
                replaceStable = new SpannableStringBuilder(newString);
            }
            if (replaceStable.length() == 0 || replaceStable.length() != newString.length()) {
                replaceStable.clear();
                replaceStable.append(newString);
            }
            else {
                replaceStable.replace(replaceStable.length() - 1, replaceStable.length(), newString, newString.length() - 1 - (newString.length() - replaceStable.length()), newString.length());
            }
        }

        if (replaceTransition != 0) {
            replaceTransition -= 0.15f;
            if (replaceTransition < 0f) {
                replaceTransition = 0f;
            }
        }

        float y = (float) getMeasuredHeight() / 2;
        float x = 0;

        if (replaceTransition == 0) {
            replaceStable.clearSpans();
            StaticLayout staticLayout = new StaticLayout(replaceStable, textPaint, getMeasuredWidth(), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            canvas.save();
            canvas.translate(x, y - staticLayout.getHeight() / 2f);
            staticLayout.draw(canvas);
            canvas.restore();
        }
        else {
            if (inLayout != null) {
                canvas.save();
                textPaint.setAlpha((int) (255 * (1f - replaceTransition)));
                canvas.translate(x, y - inLayout.getHeight() / 2f - (replaceDistance * replaceTransition));
                inLayout.draw(canvas);
                canvas.restore();
            }

            if (outLayout != null) {
                canvas.save();
                textPaint.setAlpha((int) (255 * replaceTransition));
                canvas.translate(x, y - outLayout.getHeight() / 2f + (replaceDistance * (1f - replaceTransition)));
                outLayout.draw(canvas);
                canvas.restore();
            }

            canvas.save();
            textPaint.setAlpha(255);
            StaticLayout staticLayout = new StaticLayout(replaceStable, textPaint, getMeasuredWidth(), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            canvas.translate(x, y - staticLayout.getHeight() / 2f);
            staticLayout.draw(canvas);
            canvas.restore();
        }

        oldString = newString;

        if (isRunning || replaceTransition != 0) {
            invalidate();
        }
    }

    public void reset() {
        isRunning = false;
        stopTime = startTime = 0;
    }

    private String formatTimerDurationFast(long seconds, int ms) {
        StringBuilder stringBuilder = new StringBuilder();
        long minutes = seconds / 60;
        if (minutes >= 60) {
            stringBuilder.append(minutes / 60).append(":");
            normalizeTimePart(stringBuilder, minutes % 60);
            stringBuilder.append(":");
        }
        else {
            stringBuilder.append(minutes).append(":");
        }
        normalizeTimePart(stringBuilder, seconds % 60);
        stringBuilder.append(",").append(ms / 10);
        return stringBuilder.toString();
    }

    private void normalizeTimePart(StringBuilder stringBuilder, long time) {
        if (time < 10) {
            stringBuilder
                .append("0")
                .append(time);
        }
        else {
            stringBuilder.append(time);
        }
    }
}