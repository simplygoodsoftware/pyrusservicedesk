package com.pyrus.pyrusservicedesk._ref.utils.navigation

import android.content.ActivityNotFoundException
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.github.terrakok.cicerone.BackTo
import com.github.terrakok.cicerone.Command
import com.github.terrakok.cicerone.Forward
import com.github.terrakok.cicerone.Replace
import com.github.terrakok.cicerone.androidx.AppNavigator
import com.github.terrakok.cicerone.androidx.FragmentScreen
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract


class PyrusNavigator(
    activity: FragmentActivity,
    containerId: Int,
    fragmentManager: FragmentManager = activity.supportFragmentManager,
    fragmentFactory: FragmentFactory = fragmentManager.fragmentFactory
): AppNavigator(activity, containerId, fragmentManager, fragmentFactory) {

    override fun setupFragmentTransaction(
        screen: FragmentScreen,
        fragmentTransaction: FragmentTransaction,
        currentFragment: Fragment?,
        nextFragment: Fragment
    ) {
        if (screen !is AnimatedFragmentScreen) return
        fragmentTransaction.setCustomAnimations(
            screen.animEnter,
            screen.animExit,
            screen.animPopEnter,
            screen.animPopExit
        )
    }

    override fun applyCommand(command: Command) {
        when (command) {
            is NewRootOrUpdate -> replaceOrUpdate(command)
            else -> super.applyCommand(command)
        }
    }

    override fun replace(command: Replace) {
        when(val screen = command.screen) {
            is SelfLunchActivityScreen -> {
                screen.start(activity)
                activity.finish()
            }
            else -> super.replace(command)
        }
    }

    override fun forward(command: Forward) {
        when(val screen = command.screen) {
            is SelfLunchActivityScreen -> startActivity(screen)
            else -> super.forward(command)
        }
    }

    private fun startActivity(screen: SelfLunchActivityScreen) {
        try {
            screen.start(activity)
        } catch (e: ActivityNotFoundException) {
            Log.d(TAG, "startActivity, failed: " + e.message)
        }
    }

    private fun replaceOrUpdate(command: NewRootOrUpdate) {
        val fragment = fragmentManager.findFragmentByTag(command.screen.screenKey)
        if (canUpdateScreen(fragment, command)) {
            fragment.onScreenUpdate(command.payload)
        }
        else {
            backTo(BackTo(null))
            replace(Replace(command.screen))
        }
    }

    @OptIn(ExperimentalContracts::class)
    private fun canUpdateScreen(fragment: Fragment?, command: NewRootOrUpdate): Boolean {

        contract {
            returns(true) implies (fragment is Updatable)
            returns(false) implies (fragment !is Updatable)
        }

        return (fragment != null
            && fragment.isAdded
            && !fragment.isDetached
            && fragment is Updatable
            && fragment.isTheSameScreen(command.screen.screenKey))
    }

    companion object {
        private const val TAG = "PyrusNavigator"
    }

}