package com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.tabs.TabLayoutMediator
import com.pyrus.pyrusservicedesk.PyrusServiceDesk
import com.pyrus.pyrusservicedesk.R
import com.pyrus.pyrusservicedesk.databinding.PsdTicketsListBinding
import com.pyrus.pyrusservicedesk.presentation.ConnectionActivityBase
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.addTicket.AddTicketFragment
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.filterTicketsList.FilterTicketsFragment
import com.pyrus.pyrusservicedesk.presentation.ui.navigation_page.tickets_list.adapters.ViewPagerAdapter
import com.pyrus.pyrusservicedesk.sdk.data.Application
import com.pyrus.pyrusservicedesk.sdk.data.Ticket
import com.pyrus.pyrusservicedesk.sdk.web.retrofit.SyncRepository
import com.pyrus.pyrusservicedesk.utils.CIRCLE_TRANSFORMATION
import com.pyrus.pyrusservicedesk.utils.RequestUtils.Companion.getOrganisationLogoUrl
import kotlinx.android.synthetic.main.psd_empty_tickets_list.view.createTicketTv
import kotlinx.android.synthetic.main.psd_toolbar.view.psd_toolbar_filter_ib
import kotlinx.android.synthetic.main.psd_toolbar.view.psd_toolbar_qr_ib
import kotlinx.android.synthetic.main.psd_toolbar.view.psd_toolbar_settings_ib
import kotlinx.android.synthetic.main.psd_toolbar.view.psd_toolbar_vendor_iv
import kotlinx.android.synthetic.main.psd_toolbar.view.psd_toolbar_vendor_name_tv
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


/**
 * Activity for rendering ticket/feed comments.
 */
internal class TicketListActivity :
    ConnectionActivityBase<TicketsListViewModel>(TicketsListViewModel::class.java),
    FilterTicketsFragment.CallbackForFilter {

    private var selectedUserIdFilter: String = KEY_DEFAULT_USER_ID

    override val layoutResId = R.layout.psd_tickets_list
    override val toolbarViewId = R.id.toolbar_tickets_list
    override val refresherViewId = View.NO_ID
    override val progressBarViewId: Int = View.NO_ID

    private lateinit var viewPagerAdapter: ViewPagerAdapter
    private lateinit var binding: PsdTicketsListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = PsdTicketsListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbarTicketsList.psd_toolbar_filter_ib .setOnClickListener {
            //val bottomSheet = FilterTicketsFragment.newInstance(selectedUserIdFilter)
            //bottomSheet.show(supportFragmentManager, bottomSheet.tag)
            Toast.makeText(applicationContext, "фильтры", Toast.LENGTH_SHORT).show()
        }

        binding.deleteFilterIv.setOnClickListener { onDataSentBack(KEY_DEFAULT_USER_ID, "all") }


        binding.psdEmptyTicketsListLl.createTicketTv.setOnClickListener {
            //TODO
            AddTicketFragment().show(supportFragmentManager, "")
        }

        val syncRepository = SyncRepository()


        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                syncRepository.ticketsListStateFlow.collect { value ->

//                    value.let {
//                        binding.toolbarTicketsList.psd_toolbar_vendor_name_tv.text = it
//                    }
                    if (value.tickets.applications?.isEmpty() == false) {

                        value.tickets.applications[0].orgLogoUrl?.let {
                                PyrusServiceDesk.get().picasso
                                    .load(
                                        getOrganisationLogoUrl(
                                            it, PyrusServiceDesk.get().domain
                                        )
                                    )
                                    .transform(CIRCLE_TRANSFORMATION)
                                    .into(binding.toolbarTicketsList.psd_toolbar_vendor_iv)
                            }


                        value.tickets.applications[0].orgName.let {
                            binding.toolbarTicketsList.psd_toolbar_vendor_name_tv.text = it
                        }

                        //TODO appCount, appName
                        val appSize = value.tickets.applications.size
                        viewPagerAdapter = ViewPagerAdapter(appSize, this@TicketListActivity)
                        binding.viewPager.adapter = viewPagerAdapter
                        if (appSize > 1) {
                            binding.tabLayout.visibility = View.VISIBLE
                            TabLayoutMediator(
                                binding.tabLayout,
                                binding.viewPager
                            ) { tab, position ->
                                tab.text =
                                    value.tickets.applications[position].orgName
                            }.attach()
                        } else {
                            binding.tabLayout.visibility = View.GONE
                        }
                    }
                }
            }
        }

        SyncRepository().startSync()

        // Пример обновления значения из ViewModel
        val application: List<Application> = listOf(Application(PyrusServiceDesk.get().appId, "home", null))
        //syncRepository.updateValue(SyncRes(Tickets(false, application, emptyList(), emptyList())))
    }

    private fun getSelectedUserIds(chosenUserId: String): List<Ticket> {
        val allUsersName = viewModel.getTicketsLiveData().value ?: emptyList()
        if (chosenUserId == KEY_DEFAULT_USER_ID)
            return allUsersName

        return allUsersName.filter { it.userId == chosenUserId }
    }

    override fun onDataSentBack(userId: String, userName: String) {
        //adapter.setItems(getSelectedUserIds(userId))
        // TODO
        selectedUserIdFilter = userId
        val toolbarFilter = findViewById<ImageButton>(R.id.psd_toolbar_filter_ib)
        toolbarFilter.setBackgroundResource(if (userId == KEY_DEFAULT_USER_ID) R.drawable.ic_filter else R.drawable.ic_selected_filter)
        binding.filterContextTv.text = userName
        binding.filterFl.visibility = if (userId == KEY_DEFAULT_USER_ID) View.GONE else View.VISIBLE

    }

    override fun startObserveData() {
        super.startObserveData()
        viewModel.getTicketsLiveData().observe(
            this
        ) { list ->
            //TODO
            //refresh.isRefreshing = false
            val visibility = list.isNullOrEmpty()
            binding.toolbarTicketsList.psd_toolbar_filter_ib.visibility = if (!visibility) View.VISIBLE else View.GONE
            binding.toolbarTicketsList.psd_toolbar_qr_ib.visibility =
                if (!visibility) View.VISIBLE else View.GONE
            binding.toolbarTicketsList.psd_toolbar_settings_ib.visibility =
                if (visibility) View.VISIBLE else View.GONE
            binding.psdEmptyTicketsListLl.visibility = if (visibility) View.VISIBLE else View.GONE
            binding.fabAddTicket.visibility = if (!visibility) View.VISIBLE else View.GONE

        }

        /*viewModel.getApplicationsLiveData().observe(
            this
        ) { applications ->
            //TODO several vendors

            applications[0].orgLogoUrl?.let {
                PyrusServiceDesk.get().picasso
                    .load(getOrganisationLogoUrl(it, PyrusServiceDesk.get().domain))
                    .transform(CIRCLE_TRANSFORMATION)
                    .into(binding.toolbarTicketsList.psd_toolbar_vendor_iv)
            }

            applications[0].orgName.let { binding.toolbarTicketsList.psd_toolbar_vendor_name_tv.text = it }

            //TODO appCount, appName
            viewPagerAdapter = ViewPagerAdapter(applications.size, this)
            binding.viewPager.adapter = viewPagerAdapter
            if (applications.size > 1) {
                binding.tabLayout.visibility = View.VISIBLE
                TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
                    tab.text = applications[position].orgName
                }.attach()
            }
            else {
                binding.tabLayout.visibility = View.GONE
            }
        }*/
    }

    companion object {

        private const val KEY_DEFAULT_USER_ID = "0"

        /**
         * Provides intent for launching the screen.
         */
        fun getLaunchIntent(): Intent {
            return Intent(
                PyrusServiceDesk.get().application,
                TicketListActivity::class.java
            )
        }

    }

}