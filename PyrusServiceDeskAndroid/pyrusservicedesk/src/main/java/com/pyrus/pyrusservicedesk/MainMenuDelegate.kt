package com.pyrus.pyrusservicedesk

import android.app.Activity
import android.view.Menu
import android.view.MenuItem

/**
 * Interface to interact with Ticket options menu.
 */
interface MainMenuDelegate {

    /**
     * Called when options menu is created.
     */
    fun onCreateOptionsMenu(menu: Menu, activity: Activity): Boolean

    /**
     * Called when options menu is created.
     */
    fun onOptionsItemSelected(item: MenuItem, activity: Activity): Boolean

}
