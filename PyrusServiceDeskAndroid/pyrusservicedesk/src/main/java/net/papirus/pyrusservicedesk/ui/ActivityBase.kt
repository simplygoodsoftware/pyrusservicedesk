package net.papirus.pyrusservicedesk.ui

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AppCompatActivity
import com.example.pyrusservicedesk.R
import net.papirus.pyrusservicedesk.ui.viewmodel.SharedViewModel
import net.papirus.pyrusservicedesk.ui.viewmodel.ViewModelFactory


internal abstract class ActivityBase: AppCompatActivity() {

    abstract val layoutResId: Int
    abstract val toolbarViewId: Int

    protected val sharedViewModel: SharedViewModel by getViewModel(SharedViewModel::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.PapirusServiceDesk)
        setContentView(layoutResId)
        setSupportActionBar(findViewById(toolbarViewId))
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        observeData()
    }

    protected fun <T : ViewModel> getViewModel(viewModelClass: Class<T>): Lazy<T> {

        return lazy(LazyThreadSafetyMode.NONE) {
            ViewModelProviders.of(
                    this,
                    ViewModelFactory(intent)).get(viewModelClass)
        }
    }

    protected abstract fun observeData()
}
