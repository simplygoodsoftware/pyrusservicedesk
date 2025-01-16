package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.add_ticket

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.injector
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.User
import com.pyrus.pyrusservicedesk._ref.Screens
import com.pyrus.pyrusservicedesk._ref.utils.navigation.setSlideRightAnimation
import com.pyrus.pyrusservicedesk.databinding.AddTicketFragmentBinding
import com.pyrus.pyrusservicedesk.sdk.repositories.UserInternal

class AddTicketBottomSheetFragment: BottomSheetDialogFragment() {

    private lateinit var binding: AddTicketFragmentBinding
    private var selectedUsers: List<User> = emptyList()
    private var selectedUserNames: List<String> = emptyList()
    override fun getTheme() = R.style.PsdAppBottomSheetDialogTheme

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = AddTicketFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.usersRv.layoutManager = LinearLayoutManager(view.context)

        val appId = arguments?.getString(KEY_APP_ID)
        selectedUsers = arguments?.getParcelableArrayList(KEY_USERS) ?: emptyList()
        updateSelectedUsers(appId)
        val adapter = AddTicketAdapter(users = selectedUserNames,
            onItemClick = { position ->
                val user = selectedUsers[position]
                injector().router.navigateTo(
                    Screens.TicketScreen(
                        injector().localCommandsStore.getNextLocalId(),
                        UserInternal(user.userId, user.appId)
                    ).setSlideRightAnimation()
                )
                dismiss()
            })
        binding.usersRv.adapter = adapter

        val slideIn = AnimationUtils.loadAnimation(context, R.anim.psd_animation_window_exit)
        view.startAnimation(slideIn)
    }

    private fun updateSelectedUsers(appId: String?) {
        if (appId == null)
            return
        selectedUserNames = selectedUsers.filter { it.appId == appId }.map { it.userName }
    }

    override fun onDestroyView() {
        val slideOut = AnimationUtils.loadAnimation(context, R.anim.psd_animation_window_enter)
        view?.startAnimation(slideOut)
        super.onDestroyView()
    }

    companion object {
        private const val KEY_APP_ID = "KEY_APP_ID"
        private const val KEY_USERS = "KEY_USERS"

        fun newInstance(appId: String, users: List<User>): AddTicketBottomSheetFragment {
            val fragment = AddTicketBottomSheetFragment()
            val args = Bundle()
            args.putString(KEY_APP_ID, appId)
            args.putParcelableArrayList(KEY_USERS, ArrayList(users))
            fragment.arguments = args
            return fragment
        }
    }
}