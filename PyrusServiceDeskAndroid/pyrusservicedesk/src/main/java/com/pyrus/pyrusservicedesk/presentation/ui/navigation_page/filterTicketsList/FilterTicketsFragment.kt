package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.filterTicketsList

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.TicketsListViewModel
import com.pyrus.pyrusservicedesk.utils.getViewModelWithActivityScope

class FilterTicketsFragment: BottomSheetDialogFragment() {

    private val viewModel: TicketsListViewModel by getViewModelWithActivityScope(TicketsListViewModel::class.java)

    override fun getTheme() = R.style.PsdAppBottomSheetDialogTheme

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.add_ticket_fragment, null, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val chosenUserId = arguments?.getString(KEY_CHOSEN_USER_ID, KEY_DEFAULT_USER_ID)

        val title: TextView = view.findViewById(R.id.titleTextView)
        title.text = "Фильтры"
        val recyclerview: RecyclerView = view.findViewById(R.id.usersRv)
        recyclerview.layoutManager = LinearLayoutManager(view.context)

        val userIds = viewModel.getUsersId() ?: emptyList()


        val adapter = FilterTicketsAdapter(users = getUsersName(),
            userIds,
            chosenUserId ?: KEY_DEFAULT_USER_ID,
            onItemClick = { position ->
                sendDataBack(userIds[position], getUsersName().get(position))
                dismiss()
            })
        recyclerview.adapter = adapter
    }

    private var callback: CallbackForFilter? = null

    interface CallbackForFilter {
        fun onDataSentBack(userId: String, userName: String)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is CallbackForFilter) {
            callback = context
        } else {
            throw ClassCastException("$context must implement CallbackForFilter")
        }
    }

    private fun sendDataBack(userId: String, userName: String) {
        callback?.onDataSentBack(userId, userName)
        dismiss()
    }

    private fun getUsersName(): List<String> {
        return viewModel.getUsersName() ?: emptyList()
    }

    companion object {
        private const val KEY_CHOSEN_USER_ID = "KEY_CHOSEN_USER_ID"
        private const val KEY_DEFAULT_USER_ID = "0"

        fun newInstance(data: String): FilterTicketsFragment {
            val fragment = FilterTicketsFragment()
            val args = Bundle()
            args.putString(KEY_CHOSEN_USER_ID, data)
            fragment.arguments = args
            return fragment
        }
    }
}