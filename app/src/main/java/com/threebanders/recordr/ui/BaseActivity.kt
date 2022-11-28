package com.threebanders.recordr.ui

import android.content.SharedPreferences
import android.view.View
import androidx.fragment.app.Fragment
import com.threebanders.recordr.CrApp
import com.threebanders.recordr.R
import com.threebanders.recordr.sync.util.RunTimePermission
import com.threebanders.recordr.ui.settings.SettingsFragment

abstract class BaseActivity : RunTimePermission() {
    var settledTheme: String? = null
        private set

    protected abstract fun createFragment(): Fragment?
    enum class LayoutType {
        SINGLE_PANE, DOUBLE_PANE
    }

    protected fun insertFragment(fragmentId: Int) {
        val fm = supportFragmentManager
        var fragment = fm.findFragmentById(fragmentId)
        if (fragment == null) {
            fragment = createFragment()
            fm.beginTransaction().add(fragmentId, fragment!!).commit()
        }
    }

    protected fun setTheme() {
        val settings = prefs
        if (settings.getString(SettingsFragment.APP_THEME, LIGHT_THEME) == LIGHT_THEME) {
            settledTheme = LIGHT_THEME
            setTheme(R.style.AppThemeLight)
        } else {
            settledTheme = DARK_THEME
            setTheme(R.style.AppThemeDark)
        }
    }

    protected fun checkIfThemeChanged() {
        val settings = prefs
        if (settings.getString(SettingsFragment.APP_THEME, LIGHT_THEME) != settledTheme) {
            setTheme()
            recreate()
        }
    }

    val prefs: SharedPreferences
        get() = (application as CrApp).core.prefs
    val layoutType: LayoutType
        get() {
            val listBoxExists = findViewById<View?>(R.id.contacts_list_fragment_container) != null
            val detailBoxExists =
                findViewById<View?>(R.id.contact_detail_fragment_container) != null
            return if (listBoxExists && detailBoxExists) LayoutType.DOUBLE_PANE else LayoutType.SINGLE_PANE
        }

    companion object {
        const val LIGHT_THEME = "light_theme"
        const val DARK_THEME = "dark_theme"
    }
}