package com.jder.data

import java.util.prefs.BackingStoreException
import java.util.prefs.Preferences

object AppPreferences {
    private const val SNAP_TO_GRID_KEY = "snapToGrid"
    private val preferences = Preferences.userNodeForPackage(AppPreferences::class.java)

    fun isSnapToGridEnabled(): Boolean = preferences.getBoolean(SNAP_TO_GRID_KEY, false)

    @Throws(BackingStoreException::class)
    fun setSnapToGridEnabled(enabled: Boolean) {
        preferences.putBoolean(SNAP_TO_GRID_KEY, enabled)
        preferences.flush()
    }
}
