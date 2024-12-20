package com.pyrus.pyrusservicedesk._ref.ui_domain.screens.filterTicketsList

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.pyrus.pyrusservicedesk.PyrusServiceDesk.Companion.injector
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.databinding.AddTicketFragmentBinding

class FilterTicketsFragment: BottomSheetDialogFragment() {

    private lateinit var binding: AddTicketFragmentBinding

    private var selectedUserIds: List<String> = emptyList()
    private var selectedUserNames: List<String> = emptyList()

    override fun getTheme() = R.style.PsdAppBottomSheetDialogTheme

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = AddTicketFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val chosenUserId = arguments?.getString(KEY_SELECTED_USER_ID, KEY_DEFAULT_USER_ID)
        val appId = arguments?.getString(KEY_APP_ID)
        updateSelectedUsers(appId)

        val title: TextView = view.findViewById(R.id.titleTextView)
        title.text = "Фильтры"
        //val recyclerview: RecyclerView = view.findViewById(R.id.usersRv)
        binding.usersRv.layoutManager = LinearLayoutManager(context)
        val adapter = FilterTicketsAdapter(
            users = selectedUserNames,
            userIds = selectedUserIds,
            chosenUserId ?: KEY_DEFAULT_USER_ID,
            onItemClick = { position ->
                parentFragmentManager.setFragmentResult(
                    KEY_FILTER_RESULT, bundleOf(
                        KEY_SELECTED_USER_ID to selectedUserIds[position]))
                dismiss()
            })
        binding.usersRv.adapter = adapter
    }

    private fun updateSelectedUsers(appId: String?) {
        if (appId == null)
            return

        val selectedUsers = injector().usersAccount?.users?.filter { it.appId == appId }
        selectedUserIds = selectedUsers?.map { it.userId } ?: emptyList()
        selectedUserNames = selectedUsers?.map { it.userName } ?: emptyList()
    }

    companion object {

        const val KEY_SELECTED_USER_ID = "KEY_SELECTED_USER_ID"
        const val KEY_FILTER_RESULT = "KEY_FILTER_RESULT"
        private const val KEY_APP_ID = "KEY_APP_ID"
        private const val KEY_DEFAULT_USER_ID = "0"

        fun newInstance(appId: String , selectedUserId: String): FilterTicketsFragment {
            val fragment = FilterTicketsFragment()
            val args = Bundle()
            args.putString(KEY_SELECTED_USER_ID, selectedUserId)
            args.putString(KEY_APP_ID, appId)
            fragment.arguments = args
            return fragment
        }
    }
}