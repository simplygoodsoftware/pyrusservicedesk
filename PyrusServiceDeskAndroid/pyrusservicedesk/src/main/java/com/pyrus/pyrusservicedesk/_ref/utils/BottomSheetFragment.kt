package com.pyrus.pyrusservicedesk._ref.utils

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.databinding.BottomSheetContainerBinding


abstract class BottomSheetFragment: Fragment() {

    private lateinit var containerBinding: BottomSheetContainerBinding
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    // A flag that the fragment should be in a hidden state at startup. Required for correct
    // processing of the hidden state of the dialog when changing the configuration.
    protected var hideOnStart = false

    private inner class BottomSheetCallback : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                disposeFragment()
            }
        }
        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            val alpha = if (slideOffset < 0) 0f else if (slideOffset > 1) 1f else slideOffset
            containerBinding.backgroundView.alpha = alpha
        }
    }

    /**
     * Method for dialog disposal. Can be overridden in the children if you want the dialog not
     * to be destroyed when going to the hidden state.
     */
    open fun disposeFragment() {
        parentFragmentManager.popBackStack()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        containerBinding = BottomSheetContainerBinding.inflate(inflater, container, false)

        val view = onCreateBottomSheetView(inflater, containerBinding.root, savedInstanceState)

        val lp = view.layoutParams as CoordinatorLayout.LayoutParams
        bottomSheetBehavior = BottomSheetBehavior<View>(view.context, null)
        bottomSheetBehavior.peekHeight = 0
        bottomSheetBehavior.isHideable = true
        bottomSheetBehavior.isFitToContents = true
        bottomSheetBehavior.skipCollapsed = true
        bottomSheetBehavior.halfExpandedRatio = 0.0001f // skip half expanded
        bottomSheetBehavior.expandedOffset = resources.getDimension(R.dimen.psd_bottomSheet_offset).toInt()
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior.addBottomSheetCallback(BottomSheetCallback())
        lp.behavior = bottomSheetBehavior
//        view.elevation = requireContext().resources.getDimension(R.dimen.elevation_4)
//        view.isClickable = true

        if(!hideOnStart){
            view.post {
                if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
        }

        containerBinding.root.addView(view)

        return containerBinding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        containerBinding.backgroundView.setOnClickListener {
            closeFragment()
        }
    }

//    override fun onBackPressed(): Boolean {
//        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
//        return true
//    }

    fun closeFragment() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    abstract fun onCreateBottomSheetView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View

}