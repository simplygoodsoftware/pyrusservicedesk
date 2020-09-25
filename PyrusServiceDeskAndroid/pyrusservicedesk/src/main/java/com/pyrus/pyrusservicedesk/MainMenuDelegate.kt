package com.pyrus.pyrusservicedesk

import android.app.Activity
import android.view.Menu
import android.view.MenuItem

interface MainMenuDelegate {

    fun onCreateOptionsMenu(menu: Menu, activity: Activity): Boolean

    fun onOptionsItemSelected(item: MenuItem, activity: Activity): Boolean
}