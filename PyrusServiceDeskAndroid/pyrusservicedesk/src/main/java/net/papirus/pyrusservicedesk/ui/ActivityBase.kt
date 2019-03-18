package net.papirus.pyrusservicedesk.ui

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.example.pyrusservicedesk.R
import net.papirus.pyrusservicedesk.ServiceDeskActivity
import net.papirus.pyrusservicedesk.ui.viewmodel.SharedViewModel
import net.papirus.pyrusservicedesk.utils.getViewModel


internal abstract class ActivityBase: AppCompatActivity() {

    abstract val layoutResId: Int
    abstract val toolbarViewId: Int

    protected val sharedViewModel: SharedViewModel by getViewModel(SharedViewModel::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val theme = when{
            ServiceDeskActivity.getStyle().isDialogStyle -> R.style.PyrusServiceDesk_Dialog
            else -> R.style.PyrusServiceDesk
        }
        setTheme(theme)
        setContentView(layoutResId)
        setSupportActionBar(findViewById(toolbarViewId))
        findViewById<View>(android.R.id.content).apply {
            viewTreeObserver.addOnGlobalLayoutListener {
                val heightDiff = this.rootView.height - this.height
                onViewHeightChanged(heightDiff)
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        observeData()
    }

    protected open fun onViewHeightChanged(changedBy: Int) {}

    protected open fun observeData() {
        sharedViewModel.getQuitServiceDeskLiveData().observe(
            this,
            Observer { quit -> quit?.let { if(it) finish() } }
        )
    }
}
