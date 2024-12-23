package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.add_ticket

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.injector
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk._ref.Screens
import com.pyrus.pyrusservicedesk.databinding.AddTicketFragmentBinding
import com.pyrus.pyrusservicedesk.sdk.data.User

class AddTicketFragment: BottomSheetDialogFragment() {

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
        updateSelectedUsers(appId)
        val adapter = AddTicketAdapter(users = selectedUserNames,
            onItemClick = { position ->
                injector().router.navigateTo(Screens.TicketScreen(null, selectedUsers[position].userId))
                //startActivity(TicketActivity.getLaunchIntent(userId = selectedUsers[position].userId))
                dismiss()
            })
        binding.usersRv.adapter = adapter
    }

    private fun updateSelectedUsers(appId: String?) {
        if (appId == null)
            return
        selectedUsers = injector().usersAccount?.users?.filter { it.appId == appId } ?: emptyList()
        selectedUserNames = selectedUsers.map { it.userName }
    }

    companion object {
        private const val KEY_APP_ID = "KEY_APP_ID"

        fun newInstance(appId: String): AddTicketFragment {
            val fragment = AddTicketFragment()
            val args = Bundle()
            args.putString(KEY_APP_ID, appId)
            fragment.arguments = args
            return fragment
        }
    }
}