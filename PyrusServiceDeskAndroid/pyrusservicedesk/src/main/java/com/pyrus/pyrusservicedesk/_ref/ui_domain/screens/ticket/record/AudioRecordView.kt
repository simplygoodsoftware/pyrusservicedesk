package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.record

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.core.animation.addListener
import androidx.core.view.isVisible
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketContract.RecordState
import com.pyrus.pyrusservicedesk._ref.ui_domain.screens.ticket.TicketView.Event
import com.pyrus.pyrusservicedesk.utils.UiUtils
import com.pyrus.pyrusservicedesk.utils.dp
import com.pyrus.pyrusservicedesk.utils.hapticFeedback


internal class AudioRecordView(
    private val recordWaves: RecordIndicationView,
    private val recordButton: View,
    private val sendButton: View,
    private val onEvent: (Event) -> Unit,
    private val requestPermission: () -> Boolean,
    private val parentView: View,
    private val actionCircle: CircleFrameLayout,
    private val actionCircleHolder: View,
    private val lockStopView: LockStopView,
    private val lockStopLayout: View,
    private val cancelRecordHint: View,
    private val cancelHoldingRecordButton: View,
    private val removePendingAudioButton: View,
    private val pendingPlayerLayout: View,
    private val attachButton: View,
    private val inputEditText: View,
    private val timerView: TimerView,
    private val recordDot: View,
) {

    private var isPressed = false
    private var canInteract = false

    private val mLongPressedRunnable: Runnable
    private val mMargin: Float

    private var ignoreAnyEvent = true

    private var startTouchTime: Long = -1

    private var startedDraggingX = -1f
    private var startedDraggingY = -1f

    private val cancelDistance = 140f.dp()
    private val lockDistance = 57f.dp()
    private var threshold: Float = 2f.dp()

    private var recordStateAnimator: Animator? = null
    private var resetStateAnimator: Animator? = null
    private var recordDotAnimator: Animator? = null

    private var isRecording: Boolean = false

    init {
        setOnTouchListener()

        mLongPressedRunnable = Runnable {
            recordButton.hapticFeedback()
            onEvent(Event.OnStartRecord)
            isPressed = true
        }
        mMargin = UiUtils.dpToPx(16.0f)
    }

    fun setRecordingIndicationVisibility(isVisible: Boolean) {
        playWavesAnimation(isVisible)
    }

    fun canInteract(canInteract: Boolean) {
        this.canInteract = canInteract
        resetState()
    }

    fun showAudioRecordTooltip() {
        val animHeight: Int = UiUtils.dpToPx(8)
        val a: ObjectAnimator = ObjectAnimator
            .ofFloat<View>(
                recordButton,
                View.TRANSLATION_Y,
                0f,
                animHeight.toFloat(),
                -animHeight.toFloat(),
                animHeight.toFloat(),
                -animHeight.toFloat(),
                0f
            )
            .setDuration(500L)
        a.interpolator = LinearInterpolator()
        a.start()
    }

    fun updateFrequency(recordedSegmentValues: ShortArray) {
        recordWaves.setRecordedSegmentValues(recordedSegmentValues)
    }

    fun applyRecordState(recordState: RecordState) {
        val isRecording = recordState is RecordState.Recording
        val isInputStateVisible = recordState is RecordState.None
        val isHoldRecording = recordState is RecordState.HoldRecording
        val isPendingRecord = recordState is RecordState.PendingRecord

        this.isRecording = isRecording

        if (isHoldRecording) {
            lockStopView.animateToEnd()
        }
        if (!isHoldRecording && !isRecording) {
            lockStopView.cancelAnimation()
        }

        val lockAnimation = animateLockVisibility(isRecording || isHoldRecording)
        val cancelHoldingRecordAnimator = animateCancelHoldingRecordVisibility(isHoldRecording)

        val cancelRecordHintAnimator = cancelRecordHint.animateVisibility(isRecording) {
            cancelRecordHint.translationX = 0f
        }

        val removePendingAudioAnimator = removePendingAudioButton.animateVisibility(isPendingRecord)
        val pendingPlayerAnimator = pendingPlayerLayout.animateVisibility(isPendingRecord)

        val attachAnimator = attachButton.animateVisibility(
            isVisible = isInputStateVisible,
            duration = 150L,
            toInvisibleStartDelay = 150L
        )
        val inputAnimator = inputEditText.animateVisibility(
            isVisible = isInputStateVisible,
            duration = 150L,
            toInvisibleStartDelay = 150L,
            invisibleState = View.INVISIBLE
        )

        val circleAnimator = animateActionCircleVisibility(isRecording || isHoldRecording)

        val timerAnimator = animateTimerVisibility(isRecording || isHoldRecording)
        if (recordState is RecordState.Recording) {
            timerView.start(System.currentTimeMillis() - recordState.recordStartTime)
        }
        else if (recordState is RecordState.HoldRecording) {
            timerView.start(System.currentTimeMillis() - recordState.recordStartTime)
        }

        recordStateAnimator?.cancel()

        val animators = listOfNotNull(
            lockAnimation,
            cancelHoldingRecordAnimator,
            cancelRecordHintAnimator,
            removePendingAudioAnimator,
            pendingPlayerAnimator,
            attachAnimator,
            inputAnimator,
            circleAnimator,
            timerAnimator,
        )

        val animationSet = AnimatorSet()
        animationSet.playTogether(*animators.toTypedArray())
        recordStateAnimator = animationSet
        animationSet.start()

        if (isRecording || isHoldRecording) {
            recordDotAnimator = animateRecordDot()
            recordDotAnimator?.start()
        }
        else {
            recordDotAnimator?.cancel()
            recordDot.alpha = 0f
            recordDot.isVisible = false
        }
    }

    private fun animateRecordDot(): Animator {
        val duration = 1000L
        return ObjectAnimator.ofFloat<View>(recordDot, View.ALPHA, 1f, 0.6f, 1f).apply {
            this.duration = duration
            this.repeatCount = ObjectAnimator.INFINITE
            this.startDelay = 200L
            this.addListener(onStart = {recordDot.isVisible = true})
        }
    }

    private fun animateTimerVisibility(isVisible: Boolean): AnimatorSet {
        val duration = 150L
        val interpolator = LinearInterpolator()
        val shift = (-20f).dp()
        val alphaAnimator = ObjectAnimator.ofFloat<View>(timerView, View.ALPHA, if (isVisible) 1f else 0f).apply {
            this.duration = duration
            this.interpolator = interpolator
        }
        val translateAnimator = ObjectAnimator.ofFloat<View>(timerView, View.TRANSLATION_X, if (isVisible) 0f else shift).apply {
            this.duration = duration
            this.interpolator = interpolator
        }

        val animationSet = AnimatorSet()
        animationSet.playTogether(alphaAnimator, translateAnimator)
        if (isVisible) {
            alphaAnimator.addListener(onStart = {
                timerView.visibility = View.VISIBLE
            })
        }
        else {
            animationSet.addListener(onEnd = {
                timerView.visibility = View.GONE
                timerView.reset()
            })
        }
        return animationSet
    }

    private fun View.animateVisibility(
        isVisible: Boolean,
        duration: Long = 300L,
        toInvisibleStartDelay: Long = 0L,
        invisibleState: Int = View.GONE,
        onEnd: (() -> Unit)? = null,
    ): Animator {
        val alphaAnimator = when {
            isVisible -> ObjectAnimator.ofFloat<View>(this, View.ALPHA, 1f).apply {
                this.duration = duration
                this.interpolator = interpolator
            }
            else -> ObjectAnimator.ofFloat<View>(this, View.ALPHA, 0f).apply {
                this.duration = duration
                this.interpolator = interpolator
                this.startDelay = toInvisibleStartDelay
            }
        }
        if (isVisible) {
            alphaAnimator.addListener(onStart = {
                this.visibility = View.VISIBLE
                this.alpha = 0f
            })
        }
        else {
            alphaAnimator.addListener(onEnd = {
                this.visibility = invisibleState
                onEnd?.invoke()
            })
        }
        return alphaAnimator
    }

    private fun animateActionCircleVisibility(isVisible: Boolean): Animator {
        val alphaAnimator = ObjectAnimator.ofFloat<View>(actionCircleHolder, View.ALPHA, if (isVisible) 1f else 0.4f).apply {
            this.duration = duration
            this.interpolator = interpolator
        }
        val circleSizeAnimator = ValueAnimator.ofFloat(actionCircle.getCircleDiameter(), if (isVisible) 74f.dp() else 24f.dp()).apply {
            this.duration = duration
            this.interpolator = interpolator
            addUpdateListener {
                val value = it.animatedValue as Float
                actionCircle.setCircleDiameter(value)
            }
        }

        val animationSet = AnimatorSet()
        animationSet.playTogether(alphaAnimator, circleSizeAnimator)
        if (isVisible) {
            alphaAnimator.addListener(onStart = {
                actionCircleHolder.visibility = View.VISIBLE
                actionCircleHolder.alpha = 0.4f
                actionCircle.setCircleDiameter(24f.dp())
            })
        }
        else {
            animationSet.addListener(onEnd = { actionCircleHolder.visibility = View.GONE })
        }
        return animationSet
    }

    private fun animateCancelHoldingRecordVisibility(isVisible: Boolean): Animator {
        val duration = 250L
        val interpolator = DecelerateInterpolator()

        val alphaAnimator = ObjectAnimator.ofFloat<View>(cancelHoldingRecordButton, View.ALPHA, if (isVisible) 1f else 0f).apply {
            this.duration = duration
            this.interpolator = interpolator
        }
        val yTranslateAnimator = ObjectAnimator.ofFloat<View>(cancelHoldingRecordButton, View.TRANSLATION_Y, if (isVisible) 0f else (-16f).dp()).apply {
            this.duration = duration
            this.interpolator = interpolator
        }

        val animationSet = AnimatorSet()
        animationSet.playTogether(alphaAnimator, yTranslateAnimator)
        if (isVisible) {
            alphaAnimator.addListener(onStart = {
                cancelHoldingRecordButton.visibility = View.VISIBLE
                cancelHoldingRecordButton.alpha = 0f
                cancelHoldingRecordButton.translationY = (-16f).dp()
            })
        }
        else {
            animationSet.addListener(onEnd = { cancelHoldingRecordButton.visibility = View.GONE })
        }
        return animationSet
    }

    private fun animateLockVisibility(isVisible: Boolean): Animator {
        val duration = 250L
        val interpolator = AccelerateDecelerateInterpolator()

        val alphaAnimation = when {
            isVisible -> ObjectAnimator.ofFloat<View>(lockStopLayout, View.ALPHA, 1f).apply {
                this.duration = duration
                this.interpolator = interpolator
            }
            else -> ObjectAnimator.ofFloat<View>(lockStopLayout, View.ALPHA, 0.4f).apply {
                this.duration = duration
                this.interpolator = interpolator
            }
        }

        val scaleXAnimation = when {
            isVisible -> ObjectAnimator.ofFloat<View>(lockStopLayout, View.SCALE_X, 1f).apply {
                this.duration = duration
                this.interpolator = interpolator
            }
            else -> ObjectAnimator.ofFloat<View>(lockStopLayout, View.SCALE_X, 0.2f).apply {
                this.duration = duration
                this.interpolator = interpolator
            }
        }
        val scaleYAnimation = when {
            isVisible -> ObjectAnimator.ofFloat<View>(lockStopLayout, View.SCALE_Y, 1f).apply {
                this.duration = duration
                this.interpolator = interpolator
            }
            else -> ObjectAnimator.ofFloat<View>(lockStopLayout, View.SCALE_Y, 0.2f).apply {
                this.duration = duration
                this.interpolator = interpolator
            }
        }
        val translateAnimation = when {
            isVisible -> ObjectAnimator.ofFloat<View>(lockStopLayout, View.TRANSLATION_Y, 0f).apply {
                this.duration = duration
                this.interpolator = interpolator
            }
            else -> ObjectAnimator.ofFloat<View>(lockStopLayout, View.TRANSLATION_Y, 44f.dp()).apply {
                this.duration = duration
                this.interpolator = interpolator
            }
        }

        val animationSet = AnimatorSet()
        animationSet.playTogether(alphaAnimation, scaleXAnimation, scaleYAnimation, translateAnimation)
        if (isVisible) {
            animationSet.addListener(onStart = {
                lockStopLayout.visibility = View.VISIBLE
                lockStopLayout.alpha = 0.4f
                lockStopLayout.scaleX = 0.2f
                lockStopLayout.scaleY = 0.2f
                lockStopLayout.translationY = 44f.dp()
            })
        }
        else {
            animationSet.addListener(onEnd = {
                lockStopLayout.visibility = View.GONE
                lockStopView.cleanAnimation()
            })
        }
        return animationSet
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setOnTouchListener() {
        recordButton.setOnTouchListener(OnTouchListener { view: View?, event: MotionEvent ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                ignoreAnyEvent = false
            }

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    resetState()
                    startTouchTime = System.currentTimeMillis()
                    if (!requestPermission()) {
                        return@OnTouchListener true
                    }

                    recordWaves.postDelayed(mLongPressedRunnable, LONG_PRESS_DURATION)
                }

                MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                    recordWaves.removeCallbacks(mLongPressedRunnable)

                    if (isPressed) {
                        if (isRecording) {
                            onEvent(Event.OnStopRecord)
                        }
                    }
                    else {
                        if (isRecording) {
                            onEvent(Event.OnCancelRecord)
                        }
                        onEvent(Event.OnMicShortClicked)
                    }

                    isPressed = false
                    ignoreAnyEvent = true

                    resetState()
                }

                MotionEvent.ACTION_MOVE -> {
                    if (!canInteract) {
                        return@OnTouchListener true
                    }
                    var x: Float = event.rawX
                    var y: Float = event.rawY

                    var distXCanMove: Float = 80f.dp()

                    if (startedDraggingX == -1f) {
                        startedDraggingX = x
                        distXCanMove = parentView.measuredWidth * 0.35f
                        if (distXCanMove > cancelDistance) {
                            distXCanMove = cancelDistance
                        }
                    }
                    if (startedDraggingY == -1f) {
                        startedDraggingY = y
                    }

                    if (!isPressed) {
                        return@OnTouchListener true
                    }
                    if (ignoreAnyEvent) {
                        return@OnTouchListener true
                    }

                    var currentCancelProgress = 0f

                    if (startedDraggingX != -1f) {

                        var rawDist: Float = -(x - startedDraggingX)

                        val dist = rawDist.coerceIn(0f, distXCanMove)

                        recordButton.translationX = -dist
                        sendButton.translationX = -dist
                        actionCircleHolder.translationX = -dist + 15f.dp()
                        cancelRecordHint.translationX = -dist / 2.5f

                        var cancelProgress: Float = (dist / distXCanMove).coerceIn(0f, 1f)
                        currentCancelProgress = cancelProgress

                        cancelRecordHint.alpha = 1f - cancelProgress

                        val triggerDistance = distXCanMove - threshold
                        if (rawDist >= triggerDistance) {
                            recordButton.hapticFeedback()
                            onEvent(Event.OnCancelRecord)
                            ignoreAnyEvent = true
                            resetState()
                        }
                    }
                    if (startedDraggingY != -1f) {
                        var rawDist: Float = -(y - startedDraggingY)

                        val dist = rawDist.coerceIn(0f, lockDistance)
                        var lockProgress: Float = (dist / lockDistance).coerceIn(0f, 1f)
                        lockStopView.setAnimationProgress(0, lockProgress)
                        lockStopLayout.translationY = -dist

                        val triggerDistance = lockDistance - threshold
                        if (rawDist >= triggerDistance && currentCancelProgress < 0.7) {
                            recordButton.hapticFeedback()
                            onEvent(Event.OnLockRecord)
                            ignoreAnyEvent = true
                            resetState()
                        }
                    }

                }
            }
            false
        })
    }

    private fun resetState() {
        startedDraggingX = -1f
        startedDraggingY = -1f
        startTouchTime = -1L

        resetStateAnimator?.cancel()

        val animatorSet = AnimatorSet()
        val translateMic = ObjectAnimator.ofFloat(recordButton, "translationX", 0f).apply {
            duration = 350
            interpolator = DecelerateInterpolator()
        }
        val translateSend = ObjectAnimator.ofFloat(sendButton, "translationX", 0f).apply {
            duration = 350
            interpolator = DecelerateInterpolator()
        }
        val translateCircle = ObjectAnimator.ofFloat(actionCircleHolder, "translationX", 15f.dp()).apply {
            duration = 350
            interpolator = DecelerateInterpolator()
        }
        val translateLock = ObjectAnimator.ofFloat(lockStopLayout, "translationY", 0f).apply {
            duration = 350
            interpolator = DecelerateInterpolator()
        }
        val translateCancel = ObjectAnimator.ofFloat(cancelRecordHint, "translationY", 0f).apply {
            duration = 350
            interpolator = DecelerateInterpolator()
        }
        animatorSet.playTogether(
            translateMic,
            translateSend,
            translateLock,
            translateCancel,
            translateCircle,
        )
        resetStateAnimator = animatorSet
        animatorSet.start()
    }

    private fun playWavesAnimation(show: Boolean) {
        val translationYTo = if (show) 0f else 64f.dp()
        val translationY: ObjectAnimator = ObjectAnimator.ofFloat<View>(recordWaves, View.TRANSLATION_Y, translationYTo)
        val from = if (show) 0f else 1f
        val to = if (show) 1f else 0f
        val alpha: ObjectAnimator = ObjectAnimator.ofFloat<View>(recordWaves, View.ALPHA, from, to)
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(translationY, alpha)
        animatorSet.addListener(
            onStart = { if (show) recordWaves.visibility = View.VISIBLE },
            onEnd = { if (!show) recordWaves.visibility = View.INVISIBLE }
        )
        animatorSet.interpolator = if (show) DecelerateInterpolator() else AccelerateInterpolator()
        animatorSet.setDuration(ANIM_DURATION)
        animatorSet.start()
    }

    companion object {
        private const val ANIM_DURATION = 250L
        private const val LONG_PRESS_DURATION = 150L
    }
}
