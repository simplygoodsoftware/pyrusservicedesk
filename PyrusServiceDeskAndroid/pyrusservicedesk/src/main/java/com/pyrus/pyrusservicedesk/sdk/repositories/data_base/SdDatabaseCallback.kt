package com.pyrus.pyrusservicedesk.sdk.repositories.data_base

import android.content.Context
import android.util.Log
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pyrus.pyrusservicedesk.sdk.updates.PreferencesManager

internal class SdDatabaseCallback(
    private val context: Context,
    private val preferencesManager: PreferencesManager,
) : RoomDatabase.Callback() {

    private var destructiveMigrationHappened = false

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        Log.d(TAG, "Database created")
        destructiveMigrationHappened = true
    }

    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        val currentVersion = db.version
        Log.d(TAG, "Database opened destructiveMigrationHappened: $destructiveMigrationHappened")
        preferencesManager.saveDbVersion(currentVersion, destructiveMigrationHappened)
    }

    companion object {
        private const val TAG = "SdDatabaseCallback"
    }
}