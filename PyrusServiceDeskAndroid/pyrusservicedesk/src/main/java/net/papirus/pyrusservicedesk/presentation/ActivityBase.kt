package net.papirus.pyrusservicedesk.presentation

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.example.pyrusservicedesk.R
import net.papirus.pyrusservicedesk.PyrusServiceDesk
import net.papirus.pyrusservicedesk.presentation.viewmodel.QuitViewModel
import net.papirus.pyrusservicedesk.utils.getViewModel


/**
 * Base class for service desk activities.
 */
internal abstract class ActivityBase: AppCompatActivity() {

    /**
     * Implementations should provide layout resource ids to be inflated to content view
     */
    abstract val layoutResId: Int

    /**
     * Implementations should provide id of toolbar view to be used as action bar.
     */
    abstract val toolbarViewId: Int

    /**
     * All activities share the same model to be able to trigger "rage" quit event that will close
     * all service desk activities of the current task.
     */
    protected val quitViewModel: QuitViewModel by getViewModel(QuitViewModel::class.java)

    private var recentHeight = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition()
        val theme = when{
            PyrusServiceDesk.getConfiguration().isDialogTheme -> R.style.PyrusServiceDesk_Dialog
            else -> R.style.PyrusServiceDesk
        }
        setTheme(theme)
        setContentView(layoutResId)
        setSupportActionBar(findViewById(toolbarViewId))
        findViewById<View>(android.R.id.content).apply {
            viewTreeObserver.addOnGlobalLayoutListener {
                val changedHeight = recentHeight - height
                if (changedHeight != 0)
                    onViewHeightChanged(changedHeight)
                recentHeight = height
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        startObserveData()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition()
    }

    /**
     * Callback that notifies that height of the keyboard has been changed.
     */
    protected open fun onViewHeightChanged(changedBy: Int) {}

    /**
     * Extenders can safely start observe view model's data here.
     */
    protected open fun startObserveData() {
        quitViewModel.getQuitServiceDeskLiveData().observe(
            this,
            Observer { quit ->
                quit?.let {
                    if(it)
                        finish()
                }
            }
        )
    }

    private fun overridePendingTransition() {
        val enter = R.anim.psd_animation_window_enter
        val exit = R.anim.psd_animation_window_exit
        super.overridePendingTransition(enter, exit)
    }
}
