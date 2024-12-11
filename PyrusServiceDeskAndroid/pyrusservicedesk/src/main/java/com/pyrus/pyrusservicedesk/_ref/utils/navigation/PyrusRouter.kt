package com.pyrus.pyrusservicedesk._ref.utils.navigation

import com.github.terrakok.cicerone.Back
import com.github.terrakok.cicerone.BackTo
import com.github.terrakok.cicerone.Navigator
import com.github.terrakok.cicerone.ResultListener
import com.github.terrakok.cicerone.ResultListenerHandler
import com.github.terrakok.cicerone.Screen
import com.github.terrakok.cicerone.androidx.FragmentScreen


interface PyrusRouter {

    /**
     * Clear all screens and open new one as root.
     * Or if screen exist, update screen.
     *
     * @param screen screen
     */
    fun newRootScreenOrUpdate(screen: FragmentScreen, payload: Any?)

    /**
     * Open new screen and add it to the screens chain.
     *
     * @param screen screen
     */
    fun navigateTo(screen: Screen)

    /**
     * Clear all screens and open new one as root.
     *
     * @param screen screen
     */
    fun newRootScreen(screen: Screen)

    /**
     * Replace current screen.
     *
     * By replacing the screen, you alters the backstack,
     * so by going fragmentBack you will return to the previous screen
     * and not to the replaced one.
     *
     * @param screen screen
     */
    fun replaceScreen(screen: Screen)

    /**
     * Return fragmentBack to the needed screen from the chain.
     *
     * Behavior in the case when no needed screens found depends on
     * the processing of the [BackTo] command in a [Navigator] implementation.
     *
     * @param screen screen
     */
    fun backTo(screen: Screen?)

    /**
     * Opens several screens inside single transaction.
     *
     * @param screens
     */
    fun newChain(vararg screens: Screen)

    /**
     * Clear current stack and open several screens inside single transaction.
     *
     * @param screens
     */
    fun newRootChain(vararg screens: Screen)

    /**
     * Remove all screens from the chain and exit.
     *
     * It's mostly used to finish the application or close a supplementary navigation chain.
     */
    fun finishChain()

    /**
     * Return to the previous screen in the chain.
     *
     * Behavior in the case when the current screen is the root depends on
     * the processing of the [Back] command in a [Navigator] implementation.
     */
    fun exit()

    /**
     * Sets data listener with given key
     * and returns [ResultListenerHandler] for availability to dispose subscription.
     *
     * After first call listener will be removed.
     */
    fun setResultListener(
        key: String,
        listener: ResultListener
    ): ResultListenerHandler

    /**
     * Sends data to listener with given key.
     */
    fun sendResult(key: String, data: Any)

}