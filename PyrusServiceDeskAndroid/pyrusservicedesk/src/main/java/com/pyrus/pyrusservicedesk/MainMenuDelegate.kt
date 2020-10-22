package com.pyrus.pyrusservicedesk

import android.app.Activity
import android.view.Menu
import android.view.MenuItem

interface MainMenuDelegate {

    /**
     * Called when the content of the Activity's standard options menu is initialized.
     *
     * @param menu The options menu in which you place your items.
     * @param activity Chat activity.
     */
    fun onCreateOptionsMenu(menu: Menu, activity: Activity): Boolean

    /**
     * Called when user click on options menu item.
     *
     * @param item {@link MenuItem} that was clicked.
     * @param activity Chat activity.
     */
    fun onOptionsItemSelected(item: MenuItem, activity: Activity): Boolean
}