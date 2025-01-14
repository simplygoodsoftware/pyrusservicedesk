package com.pyrus.pyrusservicedesk._ref.utils.navigation

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import com.github.terrakok.cicerone.androidx.Creator
import com.github.terrakok.cicerone.androidx.FragmentScreen
import com.pyrus.pyrusservicedesk.R


interface AnimatedFragmentScreen: FragmentScreen {

    val animEnter: Int
    val animExit: Int
    val animPopEnter: Int
    val animPopExit: Int

    companion object {
        operator fun invoke(
            key: String? = null,
            animEnter: Int = 0,
            animExit: Int = 0,
            animPopEnter: Int = 0,
            animPopExit: Int = 0,
            clearContainer: Boolean = true,
            fragmentCreator: Creator<FragmentFactory, Fragment>
        ) = object : AnimatedFragmentScreen {
            override val screenKey = key ?: fragmentCreator::class.java.name
            override val animEnter: Int = animEnter
            override val animPopEnter: Int = animPopEnter
            override val animPopExit: Int = animPopExit
            override val animExit: Int = animExit
            override val clearContainer = clearContainer
            override fun createFragment(factory: FragmentFactory) = fragmentCreator.create(factory)
        }

    }

}

/**
 * Set specific animation resources to run for the fragments that are
 * entering and exiting in this transaction. The [popEnter]
 * and [popExit] animations will be played for enter/exit
 * operations specifically when popping the back stack.
 *
 *  @param enter An animation or animator resource ID used for the enter animation on the
 *               view of the fragment being added or attached.
 *
 *  @param exit An animation or animator resource ID used for the exit animation on the
 *              view of the fragment being removed or detached.
 *
 * @param popEnter An animation or animator resource ID used for the enter animation on the
 *                 view of the fragment being readded or reattached caused by
 *                 FragmentManager.popBackStack() or similar methods.
 *
 * @param popExit An animation or animator resource ID used for the enter animation on the
 *                view of the fragment being removed or detached caused by
 *                FragmentManager.popBackStack() or similar methods.
 */
fun FragmentScreen.setAnimation(enter: Int, exit: Int, popEnter: Int, popExit: Int): AnimatedFragmentScreen {
    val screenKey = screenKey
    val clearContainer = clearContainer
    val createFragment = ::createFragment
    return object : AnimatedFragmentScreen {
        override val screenKey = screenKey
        override val animEnter = enter
        override val animExit = exit
        override val animPopEnter: Int = popEnter
        override val animPopExit: Int = popExit
        override val clearContainer = clearContainer
        override fun createFragment(factory: FragmentFactory) = createFragment(factory)
    }
}

/**
 * Set specific animation resources to run for the fragments that are
 * entering and exiting in this transaction. These animations will not be
 * played when popping the back stack.
 *
 *  @param enter An animation or animator resource ID used for the enter animation on the
 *               view of the fragment being added or attached.
 *
 *  @param exit An animation or animator resource ID used for the exit animation on the
 *              view of the fragment being removed or detached.
 */
fun FragmentScreen.setAnimation(enter: Int, exit: Int) = setAnimation(enter, exit, 0, 0)

fun FragmentScreen.setSlideRightAnimation() = setAnimation(
    enter = R.anim.slide_in_right,
    exit = R.anim.fade_out_with_scale,
    popEnter = R.anim.fade_in_with_scale,
    popExit = R.anim.slide_out_right
)

fun FragmentScreen.setFadeAnimation() = setAnimation(
    enter = R.anim.fade_in,
    exit = R.anim.fade_out_with_scale,
    popEnter = R.anim.fade_in_with_scale,
    popExit = R.anim.fade_out
)
