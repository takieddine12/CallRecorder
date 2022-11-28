package com.threebanders.recordr.ui.setup

import android.os.Bundle
import android.view.MenuItem
import android.webkit.WebView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.threebanders.recordr.R
import com.threebanders.recordr.ui.BaseActivity
import com.threebanders.recordr.ui.help.HelpActivity
import core.threebanders.recordr.CoreUtil

class ShowEulaActivity : BaseActivity() {
    override fun createFragment(): Fragment? {
        return null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.setup_show_eula_activity)
        val toolbar = findViewById<Toolbar>(R.id.toolbar_show_eula)
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        var html = CoreUtil.rawHtmlToString(R.raw.eula, this)
        html = html.replace(
            HelpActivity.APP_NAME_PLACEHOLDER,
            resources.getString(R.string.app_name)
        )
        val eulaHtml = findViewById<WebView>(R.id.eula_hmtl)
        eulaHtml.loadDataWithBaseURL(
            "file:///android_asset/",
            html, "text/html", null, null
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }

        return true
    }
}