package net.papirus.pyrusservicedesk.ui.view.swiperefresh

import android.content.Context
import android.support.v4.view.MotionEventCompat
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.Transformation
import android.widget.AbsListView
import com.example.pyrusservicedesk.R


/**
 * Default SwipeRefreshLayout allows only swipe from top. This class allows top, bottom and both directions.
 *
 * Class was taken from https://github.com/omadahealth/SwipyRefreshLayout. v.1.2.3.
 * Differs from the original by mChildHasOwnDimensions field and the corresponding attribute which is used in onLayout()
 * method. This is necessary for us when we use DirectedSwipeRefresh as a parent of the comments RecyclerView
 * because we should manage the bottom padding of this RecyclerView.
 *
 * 18.04.2018 (https://pyrus.com/t#id22949791)
 * Modify [.onInterceptTouchEvent] to handle horizontal dragging.
 * Changes in lines: 90 (add variable [.mInitialDownX]),
 * 93 (add variable [.mIsHorizontalDrag]),
 * 774-780 (add [.getMotionEventX])
 */
internal class DirectedSwipeRefresh
/**
 * Constructor that is called when inflating SwipeRefreshLayout from XML.
 *
 * @param context
 * @param attrs
 */ @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : ViewGroup(context, attrs) {

    private var mTarget: View? = null // the target of the gesture
    private val mChildHasOwnDimensions: Boolean // if "false" mTarget has the same dimens as DirectSwipeRefresh
    private var mDirection: RefreshLayoutDirection? = null
    private var mBothDirection: Boolean = false
    private var mListener: OnRefreshListener? = null
    private var mRefreshing = false
    private val mTouchSlop: Int
    private var mTotalDragDistance = -1f
    private val mMediumAnimationDuration: Int
    private var mCurrentTargetOffsetTop: Int = 0
    // Whether or not the starting offset has been determined.
    private var mOriginalOffsetCalculated = false

    private var mInitialMotionY: Float = 0.toFloat()
    private var mInitialDownX: Float = 0.toFloat()
    private var mInitialDownY: Float = 0.toFloat()
    private var mIsBeingDragged: Boolean = false
    private var mIsHorizontalDrag: Boolean = false
    private var mActivePointerId = INVALID_POINTER
    // Whether this item is scaled up rather than clipped
    private val mScale: Boolean = false

    // Target is returning to its start offset because it was cancelled or a
    // refresh was triggered.
    private var mReturningToStart: Boolean = false
    private val mDecelerateInterpolator: DecelerateInterpolator

    var circleView: CircleImageView? = null
    private var mCircleViewIndex = -1

    protected var mFrom: Int = 0

    private var mStartingScale: Float = 0.toFloat()

    protected var mOriginalOffsetTop: Int = 0

    private var mProgress: MaterialProgressDrawable? = null

    private var mScaleAnimation: Animation? = null

    private var mScaleDownAnimation: Animation? = null

    private var mAlphaStartAnimation: Animation? = null

    private var mAlphaMaxAnimation: Animation? = null

    private var mScaleDownToStartAnimation: Animation? = null

    private var mSpinnerFinalOffset: Float = 0f

    private var mNotify: Boolean = false

    private var mCircleWidth: Int = 0

    private var mCircleHeight: Int = 0

    // Whether the client has set a custom starting position;
    private val mUsingCustomStart: Boolean = false

    private val mRefreshListener = object : Animation.AnimationListener {
        override fun onAnimationStart(animation: Animation) {}

        override fun onAnimationRepeat(animation: Animation) {}

        override fun onAnimationEnd(animation: Animation) {
            if (mRefreshing) {
                // Make sure the progress view is fully visible
                mProgress!!.alpha = MAX_ALPHA
                mProgress!!.start()
                if (mNotify) {
                    if (mListener != null) {
                        mListener!!.onRefresh(mDirection)
                    }
                }
            } else {
                mProgress!!.stop()
                circleView!!.visibility = View.GONE
                setColorViewAlpha(MAX_ALPHA)
                // Return the circle to its start position
                if (mScale) {
                    setAnimationProgress(0f /* animation complete and view is hidden */)
                } else {
                    setTargetOffsetTopAndBottom(
                        mOriginalOffsetTop - mCurrentTargetOffsetTop,
                        true /* requires update */
                    )
                }
            }
            mCurrentTargetOffsetTop = circleView!!.top
        }
    }

    /**
     * Pre API 11, alpha is used to make the progress circle appear instead of scale.
     */
    private val isAlphaUsedForScale: Boolean
        get() = android.os.Build.VERSION.SDK_INT < 11

    /**
     * @return Whether the SwipeRefreshWidget is actively showing refresh
     * progress.
     */
    /**
     * Notify the widget that refresh state has changed. Do not call this when
     * refresh is triggered by a swipe gesture.
     *
     * @param refreshing Whether or not the view should show refresh progress.
     */
    // scale and show
    /* requires update *//* notify */ var isRefreshing: Boolean
        get() = mRefreshing
        set(refreshing) = if (refreshing && mRefreshing != refreshing) {
            mRefreshing = refreshing
            var endTarget = 0
            if (!mUsingCustomStart) {
                when (mDirection) {
                    RefreshLayoutDirection.BOTTOM -> endTarget = measuredHeight - mSpinnerFinalOffset.toInt()
                    RefreshLayoutDirection.TOP -> endTarget =
                            (mSpinnerFinalOffset - Math.abs(mOriginalOffsetTop)).toInt()
                    else -> endTarget = (mSpinnerFinalOffset - Math.abs(mOriginalOffsetTop)).toInt()
                }
            } else {
                endTarget = mSpinnerFinalOffset.toInt()
            }
            setTargetOffsetTopAndBottom(
                endTarget - mCurrentTargetOffsetTop,
                true
            )
            mNotify = false
            startScaleUpAnimation(mRefreshListener)
        } else {
            setRefreshing(refreshing, false)
        }

    private val mAnimateToCorrectPosition = object : Animation() {
        public override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            var targetTop = 0
            var endTarget = 0
            if (!mUsingCustomStart) {
                when (mDirection) {
                    RefreshLayoutDirection.BOTTOM -> endTarget = measuredHeight - mSpinnerFinalOffset.toInt()
                    RefreshLayoutDirection.TOP -> endTarget =
                            (mSpinnerFinalOffset - Math.abs(mOriginalOffsetTop)).toInt()
                    else -> endTarget = (mSpinnerFinalOffset - Math.abs(mOriginalOffsetTop)).toInt()
                }
            } else {
                endTarget = mSpinnerFinalOffset.toInt()
            }
            targetTop = mFrom + ((endTarget - mFrom) * interpolatedTime).toInt()
            val offset = targetTop - circleView!!.top
            setTargetOffsetTopAndBottom(offset, false /* requires update */)
        }
    }

    private val mAnimateToStartPosition = object : Animation() {
        public override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            moveToStart(interpolatedTime)
        }
    }

    var direction: RefreshLayoutDirection?
        get() = if (mBothDirection) RefreshLayoutDirection.BOTH else mDirection
        set(direction) {
            if (direction == RefreshLayoutDirection.BOTH) {
                mBothDirection = true
            } else {
                mBothDirection = false
                mDirection = direction
            }

            when (mDirection) {
                RefreshLayoutDirection.BOTTOM -> {
                    mOriginalOffsetTop = measuredHeight
                    mCurrentTargetOffsetTop = mOriginalOffsetTop
                }
                else -> {
                    mOriginalOffsetTop = -circleView!!.measuredHeight
                    mCurrentTargetOffsetTop = mOriginalOffsetTop
                }
            }
        }

    private fun setColorViewAlpha(targetAlpha: Int) {
        circleView!!.background.alpha = targetAlpha
        mProgress!!.alpha = targetAlpha
    }

    /**
     * The refresh indicator starting and resting position is always positioned
     * near the top of the refreshing content. This position is a consistent
     * location, but can be adjusted in either direction based on whether or not
     * there is a toolbar or actionbar present.
     *
     * @param scale Set to true if there is no view at a higher z-order than
     * where the progress spinner is set to appear.
     * @param start The offset in pixels from the top of this view at which the
     * progress spinner should appear.
     * @param end The offset in pixels from the top of this view at which the
     * progress spinner should come to rest after a successful swipe
     * gesture.
     */
    /*
    public void setProgressViewOffset(boolean scale, int start, int end) {
        mScale = scale;
        mCircleView.setVisibility(View.GONE);
        mOriginalOffsetTop = mCurrentTargetOffsetTop = start;
        mSpinnerFinalOffset = end;
        mUsingCustomStart = true;
        mCircleView.invalidate();
    }*/

    /**
     * The refresh indicator resting position is always positioned near the top
     * of the refreshing content. This position is a consistent location, but
     * can be adjusted in either direction based on whether or not there is a
     * toolbar or actionbar present.
     *
     * @param scale Set to true if there is no view at a higher z-order than
     * where the progress spinner is set to appear.
     * @param end The offset in pixels from the top of this view at which the
     * progress spinner should come to rest after a successful swipe
     * gesture.
     */
    /*
    public void setProgressViewEndTarget(boolean scale, int end) {
        mSpinnerFinalOffset = end;
        mScale = scale;
        mCircleView.invalidate();
    }*/

    /**
     * One of DEFAULT, or LARGE.
     */
    fun setSize(size: Int) {
        if (size != MaterialProgressDrawable.LARGE && size != MaterialProgressDrawable.DEFAULT) {
            return
        }
        val metrics = resources.displayMetrics
        if (size == MaterialProgressDrawable.LARGE) {
            mCircleWidth = (CIRCLE_DIAMETER_LARGE * metrics.density).toInt()
            mCircleHeight = mCircleWidth
        } else {
            mCircleWidth = (CIRCLE_DIAMETER * metrics.density).toInt()
            mCircleHeight = mCircleWidth
        }
        // force the bounds of the progress circle inside the circle view to
        // update by setting it to null before updating its size and then
        // re-setting it
        circleView!!.setImageDrawable(null)
        mProgress!!.updateSizes(size)
        circleView!!.setImageDrawable(mProgress)
    }

    init {

        mTouchSlop = ViewConfiguration.get(context).scaledTouchSlop

        mMediumAnimationDuration = resources.getInteger(
            android.R.integer.config_mediumAnimTime
        )

        setWillNotDraw(false)
        mDecelerateInterpolator = DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR)

        val a = context.obtainStyledAttributes(attrs, LAYOUT_ATTRS)
        isEnabled = a.getBoolean(0, true)
        a.recycle()

        val a2 = context.obtainStyledAttributes(attrs, R.styleable.DirectedSwipeRefresh)
        val direction = RefreshLayoutDirection.getFromInt(a2.getInt(R.styleable.DirectedSwipeRefresh_swipeDirection, 0))
        if (direction != RefreshLayoutDirection.BOTH) {
            mDirection = direction
            mBothDirection = false
        } else {
            mDirection = RefreshLayoutDirection.TOP
            mBothDirection = true
        }
        mChildHasOwnDimensions = a2.getBoolean(R.styleable.DirectedSwipeRefresh_allowChildHasOwnDimensions, false)
        a2.recycle()

        val metrics = resources.displayMetrics
        mCircleWidth = (CIRCLE_DIAMETER * metrics.density).toInt()
        mCircleHeight = (CIRCLE_DIAMETER * metrics.density).toInt()

        createProgressView()
        ViewCompat.setChildrenDrawingOrderEnabled(this, true)
        // the absolute offset has to take into account that the circle starts at an offset
        mSpinnerFinalOffset = DEFAULT_CIRCLE_TARGET * metrics.density
    }

    override fun getChildDrawingOrder(childCount: Int, i: Int): Int {
        return if (mCircleViewIndex < 0) {
            i
        } else if (i == childCount - 1) {
            // Draw the selected child last
            mCircleViewIndex
        } else if (i >= mCircleViewIndex) {
            // Move the children after the selected child earlier one
            i + 1
        } else {
            // Keep the children before the selected child the same
            i
        }
    }

    private fun createProgressView() {
        circleView = CircleImageView(context, CIRCLE_BG_LIGHT, (CIRCLE_DIAMETER / 2).toFloat())
        mProgress = MaterialProgressDrawable(context, this)
        mProgress!!.setBackgroundColor(CIRCLE_BG_LIGHT)
        circleView!!.setImageDrawable(mProgress)
        circleView!!.visibility = View.GONE
        addView(circleView)
    }

    /**
     * Set the listener to be notified when a refresh is triggered via the swipe
     * gesture.
     */
    fun setOnRefreshListener(listener: OnRefreshListener) {
        mListener = listener
    }

    private fun startScaleUpAnimation(listener: Animation.AnimationListener?) {
        circleView!!.visibility = View.VISIBLE
        if (android.os.Build.VERSION.SDK_INT >= 11) {
            // Pre API 11, alpha is used in place of scale up to show the
            // progress circle appearing.
            // Don't adjust the alpha during appearance otherwise.
            mProgress!!.alpha = MAX_ALPHA
        }
        mScaleAnimation = object : Animation() {
            public override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                setAnimationProgress(interpolatedTime)
            }
        }
        mScaleAnimation!!.duration = mMediumAnimationDuration.toLong()
        if (listener != null) {
            circleView!!.setAnimationListener(listener)
        }
        circleView!!.clearAnimation()
        circleView!!.startAnimation(mScaleAnimation)
    }

    /**
     * Pre API 11, this does an alpha animation.
     *
     * @param progress
     */
    private fun setAnimationProgress(progress: Float) {
        if (isAlphaUsedForScale) {
            setColorViewAlpha((progress * MAX_ALPHA).toInt())
        } else {
            ViewCompat.setScaleX(circleView!!, progress)
            ViewCompat.setScaleY(circleView!!, progress)
        }
    }

    private fun setRefreshing(refreshing: Boolean, notify: Boolean) {
        if (mRefreshing != refreshing) {
            mNotify = notify
            ensureTarget()
            mRefreshing = refreshing
            if (mRefreshing) {
                animateOffsetToCorrectPosition(mCurrentTargetOffsetTop, mRefreshListener)
            } else {
                startScaleDownAnimation(mRefreshListener)
            }
        }
    }

    private fun startScaleDownAnimation(listener: Animation.AnimationListener?) {
        mScaleDownAnimation = object : Animation() {
            public override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                setAnimationProgress(1 - interpolatedTime)
            }
        }
        mScaleDownAnimation!!.duration = SCALE_DOWN_DURATION.toLong()
        circleView!!.setAnimationListener(listener!!)
        circleView!!.clearAnimation()
        circleView!!.startAnimation(mScaleDownAnimation)
    }

    private fun startProgressAlphaStartAnimation() {
        mAlphaStartAnimation = startAlphaAnimation(mProgress!!.alpha, STARTING_PROGRESS_ALPHA)
    }

    private fun startProgressAlphaMaxAnimation() {
        mAlphaMaxAnimation = startAlphaAnimation(mProgress!!.alpha, MAX_ALPHA)
    }

    private fun startAlphaAnimation(startingAlpha: Int, endingAlpha: Int): Animation? {
        // Pre API 11, alpha is used in place of scale. Don't also use it to
        // show the trigger point.
        if (mScale && isAlphaUsedForScale) {
            return null
        }
        val alpha = object : Animation() {
            public override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                mProgress!!.alpha = (startingAlpha + (endingAlpha - startingAlpha) * interpolatedTime).toInt()
            }
        }
        alpha.duration = ALPHA_ANIMATION_DURATION.toLong()
        // Clear out the previous animation listeners.
        circleView!!.setAnimationListener(null!!)
        circleView!!.clearAnimation()
        circleView!!.startAnimation(alpha)
        return alpha
    }

    /**
     * Set the background color of the progress spinner disc.
     *
     * @param colorRes Resource id of the color.
     */
    fun setProgressBackgroundColor(colorRes: Int) {
        circleView!!.setBackgroundColor(colorRes)
        mProgress!!.setBackgroundColor(resources.getColor(colorRes))
    }


    @Deprecated("Use {@link #setColorSchemeResources(int...)}")
    fun setColorScheme(vararg colors: Int) {
        setColorSchemeResources(*colors)
    }

    /**
     * Set the color resources used in the progress animation from color resources.
     * The first color will also be the color of the bar that grows in response
     * to a user swipe gesture.
     *
     * @param colorResIds
     */
    fun setColorSchemeResources(vararg colorResIds: Int) {
        val res = resources
        val colorRes = IntArray(colorResIds.size)
        for (i in colorResIds.indices) {
            colorRes[i] = res.getColor(colorResIds[i])
        }
        setColorSchemeColors(*colorRes)
    }

    /**
     * Set the colors used in the progress animation. The first
     * color will also be the color of the bar that grows in response to a user
     * swipe gesture.
     *
     * @param colors
     */
    fun setColorSchemeColors(vararg colors: Int) {
        ensureTarget()
        mProgress!!.setColorSchemeColors(*colors)
    }

    private fun ensureTarget() {
        // Don't bother getting the parent height if the parent hasn't been laid
        // out yet.
        if (mTarget == null) {
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (child != circleView) {
                    mTarget = child
                    break
                }
            }
        }
        if (mTotalDragDistance == -1f) {
            if (parent != null && (parent as View).height > 0) {
                val metrics = resources.displayMetrics
                mTotalDragDistance = Math.min(
                    (parent as View).height * MAX_SWIPE_DISTANCE_FACTOR,
                    REFRESH_TRIGGER_DISTANCE * metrics.density
                ).toInt().toFloat()
            }
        }
    }

    /**
     * Set the distance to trigger a sync in dips
     *
     * @param distance
     */
    fun setDistanceToTriggerSync(distance: Int) {
        mTotalDragDistance = distance.toFloat()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {

        if (childCount == 0) {
            return
        }
        if (mTarget == null) {
            ensureTarget()
        }
        if (mTarget == null) {
            return
        }
        val width =
            if (mChildHasOwnDimensions && mTarget!!.measuredWidth > 0) mTarget!!.measuredWidth else measuredWidth
        val height =
            if (mChildHasOwnDimensions && mTarget!!.measuredHeight > 0) mTarget!!.measuredHeight else measuredHeight
        val child = mTarget
        val childLeft = paddingLeft
        val childTop = paddingTop
        val childWidth = width - paddingLeft - paddingRight
        val childHeight = height - paddingTop - paddingBottom
        child!!.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight)
        val circleWidth = circleView!!.measuredWidth
        val circleHeight = circleView!!.measuredHeight
        circleView!!.layout(
            width / 2 - circleWidth / 2, mCurrentTargetOffsetTop,
            width / 2 + circleWidth / 2, mCurrentTargetOffsetTop + circleHeight
        )
    }

    public override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (mTarget == null) {
            ensureTarget()
        }
        if (mTarget == null) {
            return
        }

        val childWidth = View.MeasureSpec.makeMeasureSpec(
            measuredWidth - paddingLeft - paddingRight, View.MeasureSpec.EXACTLY
        )
        val childHeight = View.MeasureSpec.makeMeasureSpec(
            measuredHeight - paddingTop - paddingBottom, View.MeasureSpec.EXACTLY
        )
        measureChild(mTarget, childWidth, childHeight)
        circleView!!.measure(
            View.MeasureSpec.makeMeasureSpec(mCircleWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(mCircleHeight, View.MeasureSpec.EXACTLY)
        )
        if (!mUsingCustomStart && !mOriginalOffsetCalculated) {
            mOriginalOffsetCalculated = true

            when (mDirection) {
                RefreshLayoutDirection.BOTTOM -> {
                    mOriginalOffsetTop = measuredHeight
                    mCurrentTargetOffsetTop = mOriginalOffsetTop
                }
                else -> {
                    mOriginalOffsetTop = -circleView!!.measuredHeight
                    mCurrentTargetOffsetTop = mOriginalOffsetTop
                }
            }
        }
        mCircleViewIndex = -1
        // Get the index of the circleview.
        for (index in 0 until childCount) {
            if (getChildAt(index) === circleView) {
                mCircleViewIndex = index
                break
            }
        }
    }

    /**
     * @return Whether it is possible for the child view of this layout to
     * scroll up. Override this if the child view is a custom view.
     */
    fun canChildScrollUp(): Boolean {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (mTarget is AbsListView) {
                val absListView = mTarget as AbsListView?
                return absListView!!.childCount > 0 && (absListView.firstVisiblePosition > 0 || absListView.getChildAt(0)
                    .top < absListView.paddingTop)
            } else {
                return mTarget!!.scrollY > 0
            }
        } else {
            return ViewCompat.canScrollVertically(mTarget!!, -1)
        }
    }
    //    public boolean canChildScrollUp() {
    //        if (android.os.Build.VERSION.SDK_INT < 14) {
    //            if (mTarget instanceof AbsListView) {
    //                final AbsListView absListView = (AbsListView) mTarget;
    //                if (absListView.getLastVisiblePosition() + 1 == absListView.getCount()) {
    //                    int lastIndex = absListView.getLastVisiblePosition() - absListView.getFirstVisiblePosition();
    //
    //                    boolean res = absListView.getChildAt(lastIndex).getBottom() == absListView.getPaddingBottom();
    //
    //                    return res;
    //                }
    //                return true;
    //            } else {
    //                return mTarget.getScrollY() > 0;
    //            }
    //        } else {
    //            return ViewCompat.canScrollVertically(mTarget, 1);
    //        }
    //    }


    fun canChildScrollDown(): Boolean {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (mTarget is AbsListView) {
                val absListView = mTarget as AbsListView?
                try {
                    if (absListView!!.count > 0) {
                        if (absListView.lastVisiblePosition + 1 == absListView.count) {
                            val lastIndex = absListView.lastVisiblePosition - absListView.firstVisiblePosition
                            return absListView.getChildAt(lastIndex).bottom == absListView.paddingBottom
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                return true
            } else {
                return true
            }
        } else {
            return ViewCompat.canScrollVertically(mTarget!!, 1)
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        ensureTarget()

        val action = MotionEventCompat.getActionMasked(ev)

        if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
            mReturningToStart = false
        }

        when (mDirection) {
            RefreshLayoutDirection.BOTTOM -> if (!isEnabled || mReturningToStart || !mBothDirection && canChildScrollDown() || mRefreshing) {
                // Fail fast if we're not in a state where a swipe is possible
                return false
            }
            RefreshLayoutDirection.TOP -> if (!isEnabled || mReturningToStart || !mBothDirection && canChildScrollUp() || mRefreshing) {
                // Fail fast if we're not in a state where a swipe is possible
                return false
            }
            else -> if (!isEnabled || mReturningToStart || !mBothDirection && canChildScrollUp() || mRefreshing) {
                return false
            }
        }

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                setTargetOffsetTopAndBottom(mOriginalOffsetTop - circleView!!.top, true)
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0)
                mIsBeingDragged = false
                mIsHorizontalDrag = false
                val initialDownX = getMotionEventX(ev, mActivePointerId)
                val initialDownY = getMotionEventY(ev, mActivePointerId)
                if (initialDownY == -1f) {
                    return false
                }
                mInitialDownX = initialDownX
                mInitialDownY = initialDownY
                if (mActivePointerId == INVALID_POINTER) {
                    return false
                }

                val x = getMotionEventX(ev, mActivePointerId)
                val y = getMotionEventY(ev, mActivePointerId)
                if (y == -1f) {
                    return false
                }
                if (mBothDirection) {
                    if (y > mInitialDownY) {
                        setRawDirection(RefreshLayoutDirection.TOP)
                    } else if (y < mInitialDownY) {
                        setRawDirection(RefreshLayoutDirection.BOTTOM)
                    }
                    if (mDirection == RefreshLayoutDirection.BOTTOM && canChildScrollDown() || mDirection == RefreshLayoutDirection.TOP && canChildScrollUp()) {
                        mInitialDownY = y
                        return false
                    }
                }
                val xDiff: Float
                xDiff = Math.abs(mInitialDownX - x)
                val yDiff: Float
                when (mDirection) {
                    RefreshLayoutDirection.BOTTOM -> yDiff = mInitialDownY - y
                    RefreshLayoutDirection.TOP -> yDiff = y - mInitialDownY
                    else -> yDiff = y - mInitialDownY
                }
                if (yDiff < xDiff) {
                    if (xDiff > mTouchSlop) {
                        mIsHorizontalDrag = true
                    }
                } else if (yDiff > mTouchSlop && !mIsBeingDragged) {
                    when (mDirection) {
                        RefreshLayoutDirection.BOTTOM -> mInitialMotionY = mInitialDownY - mTouchSlop
                        RefreshLayoutDirection.TOP -> mInitialMotionY = mInitialDownY + mTouchSlop
                        else -> mInitialMotionY = mInitialDownY + mTouchSlop
                    }
                    mIsBeingDragged = true
                    mProgress!!.alpha = STARTING_PROGRESS_ALPHA
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (mActivePointerId == INVALID_POINTER) {
                    return false
                }
                val x = getMotionEventX(ev, mActivePointerId)
                val y = getMotionEventY(ev, mActivePointerId)
                if (y == -1f) {
                    return false
                }
                if (mBothDirection) {
                    if (y > mInitialDownY) {
                        setRawDirection(RefreshLayoutDirection.TOP)
                    } else if (y < mInitialDownY) {
                        setRawDirection(RefreshLayoutDirection.BOTTOM)
                    }
                    if (mDirection == RefreshLayoutDirection.BOTTOM && canChildScrollDown() || mDirection == RefreshLayoutDirection.TOP && canChildScrollUp()) {
                        mInitialDownY = y
                        return false
                    }
                }
                val xDiff: Float
                xDiff = Math.abs(mInitialDownX - x)
                val yDiff: Float
                when (mDirection) {
                    RefreshLayoutDirection.BOTTOM -> yDiff = mInitialDownY - y
                    RefreshLayoutDirection.TOP -> yDiff = y - mInitialDownY
                    else -> yDiff = y - mInitialDownY
                }
                if (yDiff < xDiff) {
                    if (xDiff > mTouchSlop) {
                        mIsHorizontalDrag = true
                    }
                } else if (yDiff > mTouchSlop && !mIsBeingDragged) {
                    when (mDirection) {
                        RefreshLayoutDirection.BOTTOM -> mInitialMotionY = mInitialDownY - mTouchSlop
                        RefreshLayoutDirection.TOP -> mInitialMotionY = mInitialDownY + mTouchSlop
                        else -> mInitialMotionY = mInitialDownY + mTouchSlop
                    }
                    mIsBeingDragged = true
                    mProgress!!.alpha = STARTING_PROGRESS_ALPHA
                }
            }

            MotionEventCompat.ACTION_POINTER_UP -> onSecondaryPointerUp(ev)

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mIsBeingDragged = false
                mIsHorizontalDrag = false
                mActivePointerId = INVALID_POINTER
            }
        }

        return mIsBeingDragged && !mIsHorizontalDrag
    }

    private fun getMotionEventX(ev: MotionEvent, activePointerId: Int): Float {
        val index = MotionEventCompat.findPointerIndex(ev, activePointerId)
        return if (index < 0) {
            -1f
        } else MotionEventCompat.getX(ev, index)
    }

    private fun getMotionEventY(ev: MotionEvent, activePointerId: Int): Float {
        val index = MotionEventCompat.findPointerIndex(ev, activePointerId)
        return if (index < 0) {
            -1f
        } else MotionEventCompat.getY(ev, index)
    }

    override fun requestDisallowInterceptTouchEvent(b: Boolean) {
        // Nope.
    }

    private fun isAnimationRunning(animation: Animation?): Boolean {
        return animation != null && animation.hasStarted() && !animation.hasEnded()
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        try {
            val action = MotionEventCompat.getActionMasked(ev)

            if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
                mReturningToStart = false
            }

            when (mDirection) {
                RefreshLayoutDirection.BOTTOM -> if (!isEnabled || mReturningToStart || canChildScrollDown() || mRefreshing) {
                    // Fail fast if we're not in a state where a swipe is possible
                    return false
                }
                RefreshLayoutDirection.TOP -> if (!isEnabled || mReturningToStart || canChildScrollUp() || mRefreshing) {
                    // Fail fast if we're not in a state where a swipe is possible
                    return false
                }
                else -> if (!isEnabled || mReturningToStart || canChildScrollUp() || mRefreshing) {
                    return false
                }
            }

            when (action) {
                MotionEvent.ACTION_DOWN -> {
                    mActivePointerId = MotionEventCompat.getPointerId(ev, 0)
                    mIsBeingDragged = false
                    mIsHorizontalDrag = false
                }

                MotionEvent.ACTION_MOVE -> {
                    val pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId)
                    if (pointerIndex < 0) {
                        return false
                    }

                    val y = MotionEventCompat.getY(ev, pointerIndex)

                    val overscrollTop: Float
                    when (mDirection) {
                        RefreshLayoutDirection.BOTTOM -> overscrollTop = (mInitialMotionY - y) * DRAG_RATE
                        RefreshLayoutDirection.TOP -> overscrollTop = (y - mInitialMotionY) * DRAG_RATE
                        else -> overscrollTop = (y - mInitialMotionY) * DRAG_RATE
                    }
                    if (mIsBeingDragged) {
                        mProgress!!.showArrow(true)
                        val originalDragPercent = overscrollTop / mTotalDragDistance
                        if (originalDragPercent < 0) {
                            return false
                        }
                        val dragPercent = Math.min(1f, Math.abs(originalDragPercent))
                        val adjustedPercent = Math.max(dragPercent - .4, 0.0).toFloat() * 5 / 3
                        val extraOS = Math.abs(overscrollTop) - mTotalDragDistance
                        val slingshotDist = if (mUsingCustomStart)
                            mSpinnerFinalOffset - mOriginalOffsetTop
                        else
                            mSpinnerFinalOffset
                        val tensionSlingshotPercent = Math.max(
                            0f,
                            Math.min(extraOS, slingshotDist * 2) / slingshotDist
                        )
                        val tensionPercent = (tensionSlingshotPercent / 4 - Math.pow(
                            (tensionSlingshotPercent / 4).toDouble(), 2.0
                        )).toFloat() * 2f
                        val extraMove = slingshotDist * tensionPercent * 2f

                        // int targetY = mOriginalOffsetTop + (int) ((slingshotDist * dragPercent) + extraMove);
                        val targetY: Int
                        if (mDirection == RefreshLayoutDirection.TOP) {
                            targetY = mOriginalOffsetTop + (slingshotDist * dragPercent + extraMove).toInt()
                        } else {
                            targetY = mOriginalOffsetTop - (slingshotDist * dragPercent + extraMove).toInt()
                        }
                        // where 1.0f is a full circle
                        if (circleView!!.visibility != View.VISIBLE) {
                            circleView!!.visibility = View.VISIBLE
                        }
                        if (!mScale) {
                            ViewCompat.setScaleX(circleView!!, 1f)
                            ViewCompat.setScaleY(circleView!!, 1f)
                        }
                        if (overscrollTop < mTotalDragDistance) {
                            if (mScale) {
                                setAnimationProgress(overscrollTop / mTotalDragDistance)
                            }
                            if (mProgress!!.alpha > STARTING_PROGRESS_ALPHA && !isAnimationRunning(mAlphaStartAnimation)) {
                                // Animate the alpha
                                startProgressAlphaStartAnimation()
                            }
                            val strokeStart = adjustedPercent * .8f
                            mProgress!!.setStartEndTrim(0f, Math.min(MAX_PROGRESS_ANGLE, strokeStart))
                            mProgress!!.setArrowScale(Math.min(1f, adjustedPercent))
                        } else {
                            if (mProgress!!.alpha < MAX_ALPHA && !isAnimationRunning(mAlphaMaxAnimation)) {
                                // Animate the alpha
                                startProgressAlphaMaxAnimation()
                            }
                        }
                        val rotation = (-0.25f + .4f * adjustedPercent + tensionPercent * 2) * .5f
                        mProgress!!.setProgressRotation(rotation)
                        setTargetOffsetTopAndBottom(
                            targetY - mCurrentTargetOffsetTop,
                            true /* requires update */
                        )
                    }
                }
                MotionEventCompat.ACTION_POINTER_DOWN -> {
                    val index = MotionEventCompat.getActionIndex(ev)
                    mActivePointerId = MotionEventCompat.getPointerId(ev, index)
                }

                MotionEventCompat.ACTION_POINTER_UP -> onSecondaryPointerUp(ev)

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (mActivePointerId == INVALID_POINTER) {
                        if (action == MotionEvent.ACTION_UP) {
                        }
                        return false
                    }
                    val pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId)
                    val y = MotionEventCompat.getY(ev, pointerIndex)

                    val overscrollTop: Float
                    when (mDirection) {
                        RefreshLayoutDirection.BOTTOM -> overscrollTop = (mInitialMotionY - y) * DRAG_RATE
                        RefreshLayoutDirection.TOP -> overscrollTop = (y - mInitialMotionY) * DRAG_RATE
                        else -> overscrollTop = (y - mInitialMotionY) * DRAG_RATE
                    }
                    mIsBeingDragged = false
                    mIsHorizontalDrag = false
                    if (overscrollTop > mTotalDragDistance) {
                        setRefreshing(true, true /* notify */)
                    } else {
                        // cancel refresh
                        mRefreshing = false
                        mProgress!!.setStartEndTrim(0f, 0f)
                        var listener: Animation.AnimationListener? = null
                        if (!mScale) {
                            listener = object : Animation.AnimationListener {

                                override fun onAnimationStart(animation: Animation) {}

                                override fun onAnimationEnd(animation: Animation) {
                                    if (!mScale) {
                                        startScaleDownAnimation(null)
                                    }
                                }

                                override fun onAnimationRepeat(animation: Animation) {}

                            }
                        }
                        animateOffsetToStartPosition(mCurrentTargetOffsetTop, listener)
                        mProgress!!.showArrow(false)
                    }
                    mActivePointerId = INVALID_POINTER
                    return false
                }
            }
        } catch (e: Exception) {
            Log.d(TAG,"An exception occured during BidirectionalSwipeRefresh onTouchEvent " + e.toString())
        }

        return true
    }

    private fun animateOffsetToCorrectPosition(from: Int, listener: Animation.AnimationListener?) {
        mFrom = from
        mAnimateToCorrectPosition.reset()
        mAnimateToCorrectPosition.duration = ANIMATE_TO_TRIGGER_DURATION.toLong()
        mAnimateToCorrectPosition.interpolator = mDecelerateInterpolator
        if (listener != null) {
            circleView!!.setAnimationListener(listener)
        }
        circleView!!.clearAnimation()
        circleView!!.startAnimation(mAnimateToCorrectPosition)
    }

    private fun animateOffsetToStartPosition(from: Int, listener: Animation.AnimationListener?) {
        if (mScale) {
            // Scale the item back down
            startScaleDownReturnToStartAnimation(from, listener)
        } else {
            mFrom = from
            mAnimateToStartPosition.reset()
            mAnimateToStartPosition.duration = ANIMATE_TO_START_DURATION.toLong()
            mAnimateToStartPosition.interpolator = mDecelerateInterpolator
            if (listener != null) {
                circleView!!.setAnimationListener(listener)
            }
            circleView!!.clearAnimation()
            circleView!!.startAnimation(mAnimateToStartPosition)
        }
    }

    private fun moveToStart(interpolatedTime: Float) {
        var targetTop = 0
        targetTop = mFrom + ((mOriginalOffsetTop - mFrom) * interpolatedTime).toInt()
        val offset = targetTop - circleView!!.top
        setTargetOffsetTopAndBottom(offset, false /* requires update */)
    }

    private fun startScaleDownReturnToStartAnimation(
        from: Int,
        listener: Animation.AnimationListener?
    ) {
        mFrom = from
        if (isAlphaUsedForScale) {
            mStartingScale = mProgress!!.alpha.toFloat()
        } else {
            mStartingScale = ViewCompat.getScaleX(circleView!!)
        }
        mScaleDownToStartAnimation = object : Animation() {
            public override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                val targetScale = mStartingScale + -mStartingScale * interpolatedTime
                setAnimationProgress(targetScale)
                moveToStart(interpolatedTime)
            }
        }
        mScaleDownToStartAnimation!!.duration = SCALE_DOWN_DURATION.toLong()
        if (listener != null) {
            circleView!!.setAnimationListener(listener)
        }
        circleView!!.clearAnimation()
        circleView!!.startAnimation(mScaleDownToStartAnimation)
    }

    private fun setTargetOffsetTopAndBottom(offset: Int, requiresUpdate: Boolean) {
        circleView!!.bringToFront()
        circleView!!.offsetTopAndBottom(offset)

        //        switch (mDirection) {
        //            case BOTTOM:
        //                mCurrentTargetOffsetTop = getMeasuredHeight() - mCircleView.getMeasuredHeight();
        //                break;
        //            case TOP:
        //            default:
        //                mCurrentTargetOffsetTop  = mCircleView.getTop();
        //                break;
        //        }
        mCurrentTargetOffsetTop = circleView!!.top
        if (requiresUpdate && android.os.Build.VERSION.SDK_INT < 11) {
            invalidate()
        }
    }

    private fun onSecondaryPointerUp(ev: MotionEvent) {
        val pointerIndex = MotionEventCompat.getActionIndex(ev)
        val pointerId = MotionEventCompat.getPointerId(ev, pointerIndex)
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            val newPointerIndex = if (pointerIndex == 0) 1 else 0
            mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex)
        }
    }

    /**
     * Classes that wish to be notified when the swipe gesture correctly
     * triggers a refresh should implement this interface.
     */
    interface OnRefreshListener {
        fun onRefresh(direction: RefreshLayoutDirection?)
    }

    // only TOP or Bottom
    private fun setRawDirection(direction: RefreshLayoutDirection) {
        if (mDirection == direction) {
            return
        }

        mDirection = direction
        when (mDirection) {
            RefreshLayoutDirection.BOTTOM -> {
                mOriginalOffsetTop = measuredHeight
                mCurrentTargetOffsetTop = mOriginalOffsetTop
            }
            else -> {
                mOriginalOffsetTop = -circleView!!.measuredHeight
                mCurrentTargetOffsetTop = mOriginalOffsetTop
            }
        }
    }

    companion object {
        val TAG = "DirectedSwipeRefresh"

        private val MAX_SWIPE_DISTANCE_FACTOR = .6f
        private val REFRESH_TRIGGER_DISTANCE = 100

        // Maps to ProgressBar.Large style
        val LARGE = MaterialProgressDrawable.LARGE
        // Maps to ProgressBar default style
        val DEFAULT = MaterialProgressDrawable.DEFAULT

        private val LOG_TAG = DirectedSwipeRefresh::class.java.simpleName

        private val MAX_ALPHA = 255
        private val STARTING_PROGRESS_ALPHA = (.3f * MAX_ALPHA).toInt()

        private val CIRCLE_DIAMETER = 40
        private val CIRCLE_DIAMETER_LARGE = 56

        private val DECELERATE_INTERPOLATION_FACTOR = 2f
        private val INVALID_POINTER = -1
        private val DRAG_RATE = .5f

        // Max amount of circle that can be filled by progress during swipe gesture,
        // where 1.0 is a full circle
        private val MAX_PROGRESS_ANGLE = .8f

        private val SCALE_DOWN_DURATION = 150

        private val ALPHA_ANIMATION_DURATION = 300

        private val ANIMATE_TO_TRIGGER_DURATION = 200

        private val ANIMATE_TO_START_DURATION = 200

        // Default background for the progress spinner
        private val CIRCLE_BG_LIGHT = -0x50506
        // Default offset in dips from the top of the view to where the progress spinner should stop
        private val DEFAULT_CIRCLE_TARGET = 64
        private val LAYOUT_ATTRS = intArrayOf(android.R.attr.enabled)
    }
}
/**
 * Simple constructor to use when creating a SwipeRefreshLayout from code.
 *
 * @param context
 */