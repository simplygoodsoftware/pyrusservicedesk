package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.dialogs.rating

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.utils.ConfigUtils
import com.pyrus.pyrusservicedesk._ref.utils.insets.RootViewDeferringInsetsCallback
import com.pyrus.pyrusservicedesk.databinding.PsdFragmentRateUsBinding

class RatingBottomSheetDialogFragment: BottomSheetDialogFragment() {
    private lateinit var binding: PsdFragmentRateUsBinding


    override fun getTheme() = R.style.PsdAppBottomSheetDialogTheme

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = PsdFragmentRateUsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val accentColor = ConfigUtils.getAccentColor(requireContext())

        binding.sendBtn.backgroundTintList = ColorStateList.valueOf(accentColor)
        binding.closeBtn.setTextColor(ColorStateList.valueOf(accentColor))

        binding.rateUs.text = arguments?.getString(RATE_US_TEXT_COMMENT)

        binding.sendBtn.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                RATING_COMMENT_KEY,
                bundleOf(RATING_COMMENT_KEY to binding.input.text.toString())
            )
            dismiss()
        }

        val rootInsetsListener = RootViewDeferringInsetsCallback(
            persistentInsetTypes = WindowInsetsCompat.Type.captionBar() or WindowInsetsCompat.Type.statusBars(),
            deferredInsetTypes = WindowInsetsCompat.Type.ime(),
        )
        ViewCompat.setOnApplyWindowInsetsListener(binding.root, rootInsetsListener)

        binding.closeBtn.setOnClickListener { dismiss() }
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog
        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)

        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            val displayMetrics = resources.displayMetrics
            behavior.peekHeight = displayMetrics.heightPixels
            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    companion object {
        const val RATING_COMMENT_KEY = "RATING_COMMENT_KEY"
        private const val RATE_US_TEXT_COMMENT = "RATE_US_TEXT_COMMENT"

        fun newInstance(rateUsText: String?): RatingBottomSheetDialogFragment {
            val fragment = RatingBottomSheetDialogFragment()
            val args = Bundle()
            args.putString(RATE_US_TEXT_COMMENT, rateUsText)
            fragment.arguments = args
            return fragment
        }
    }
}