package net.papirus.pyrusservicedesk.ui

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.View.NO_ID
import android.view.ViewGroup
import com.example.pyrusservicedesk.R
import net.papirus.pyrusservicedesk.ServiceDeskActivity
import net.papirus.pyrusservicedesk.ui.viewmodel.SharedViewModel
import net.papirus.pyrusservicedesk.utils.getViewModel


internal abstract class ActivityBase: AppCompatActivity() {

    abstract val layoutResId: Int
    abstract val toolbarViewId: Int

    protected val sharedViewModel: SharedViewModel by getViewModel(SharedViewModel::class.java)
    private var recentKeyboardHeight = 0

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
                val fullHeight = rootView.height
                val systemViewsHeight = with(rootView as ViewGroup) {
                    var result = fullHeight
                    for (i in 0 until childCount) {
                        if (getChildAt(i).id != NO_ID) // view with NO_ID contains is filled by content
                            result += getChildAt(i).height
                    }
                    return@with result
                }
                val kbHeight = fullHeight - systemViewsHeight - height
                onViewHeightChanged(kbHeight - recentKeyboardHeight)
                recentKeyboardHeight = kbHeight
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
