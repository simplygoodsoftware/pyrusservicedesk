package net.papirus.pyrusservicedesk.utils

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProviders
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import net.papirus.pyrusservicedesk.presentation.viewmodel.ViewModelFactory

internal fun <T : ViewModel> FragmentActivity.getViewModel(viewModelClass: Class<T>): Lazy<T> {

    return lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProviders.of(
            this,
            ViewModelFactory(intent)
        ).get(viewModelClass)
    }
}

internal fun <T : ViewModel> Fragment.getViewModelWithActivityScope(viewModelClass: Class<T>): Lazy<T> {
    return lazy(LazyThreadSafetyMode.NONE) { activity!!.getViewModel(viewModelClass).value }
}