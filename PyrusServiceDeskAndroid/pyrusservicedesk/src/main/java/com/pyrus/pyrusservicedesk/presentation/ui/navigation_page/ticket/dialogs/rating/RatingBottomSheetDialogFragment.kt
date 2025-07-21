package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.ticket.dialogs.rating

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.os.bundleOf
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.databinding.PsdFragmentRateUsBinding
import com.pyrus.pyrusservicedesk.utils.ConfigUtils

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
            parentFragmentManager.setFragmentResult(RATING_COMMENT_KEY, bundleOf(RATING_COMMENT_KEY to binding.input.text.toString()))
            dismiss()
        }

        binding.closeBtn.setOnClickListener { dismiss() }
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