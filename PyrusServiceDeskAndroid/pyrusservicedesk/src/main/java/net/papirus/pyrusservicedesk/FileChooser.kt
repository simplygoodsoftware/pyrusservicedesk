package net.papirus.pyrusservicedesk

import android.content.Intent

interface FileChooser {
    fun getLabel(): String
    fun getIntent(): Intent
}