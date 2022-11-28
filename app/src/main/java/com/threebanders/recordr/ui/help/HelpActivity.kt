package com.threebanders.recordr.ui.help

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.tabs.TabLayout
import com.threebanders.recordr.BuildConfig
import com.threebanders.recordr.R
import com.threebanders.recordr.ui.BaseActivity
import core.threebanders.recordr.CoreUtil
import core.threebanders.recordr.CrLog

class HelpActivity : BaseActivity() {
    var pager: ViewPager? = null
    var adapter: HelpPagerAdapter? = null

    //am folosit R.raw pentru posibilitatea traducerii: res/raw-de/ for german
    override fun createFragment(): Fragment? {
        return null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme()
        val res = resources
        content[0] = CoreUtil.rawHtmlToString(R.raw.help_recording_calls, this)
        content[1] = CoreUtil.rawHtmlToString(R.raw.help_playing_recordings, this)
        content[2] = CoreUtil.rawHtmlToString(R.raw.help_managing_recordings, this)
        content[3] = CoreUtil.rawHtmlToString(R.raw.help_about, this)
        content[3] = String.format(
            content[3]!!, BuildConfig.VERSION_NAME,
            res.getString(R.string.dev_email), res.getString(R.string.dev_email),
            res.getString(R.string.send_devs)
        )
        content[4] = CoreUtil.rawHtmlToString(R.raw.eula, this)
        content[5] = CoreUtil.rawHtmlToString(R.raw.help_licences, this)
        for (i in content.indices) content[i] =
            content[i]?.replace(APP_NAME_PLACEHOLDER, res.getString(R.string.app_name))
        if (settledTheme == BaseActivity.Companion.DARK_THEME) {
            for (i in content.indices) content[i] = content[i]?.replace("light", "dark")
        }
        contentTitles[0] = res.getString(R.string.help_title2)
        contentTitles[1] = res.getString(R.string.help_title3)
        contentTitles[2] = res.getString(R.string.help_title4)
        contentTitles[3] = res.getString(R.string.about_name)
        contentTitles[4] = res.getString(R.string.help_title5)
        contentTitles[5] = res.getString(R.string.help_title7)
        setContentView(R.layout.help_activity)
        pager = findViewById(R.id.help_pager)
        adapter = HelpPagerAdapter(supportFragmentManager)
        pager!!.setAdapter(adapter)
        val tabLayout = findViewById<TabLayout>(R.id.help_tab_layout)
        tabLayout.setupWithViewPager(pager)
        val toolbar = findViewById<Toolbar>(R.id.toolbar_help)
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class HelpPagerAdapter(fm: FragmentManager?) : FragmentPagerAdapter(
        fm!!, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
    ) {
        override fun getCount(): Int {
            return NUM_PAGES
        }

        override fun getItem(position: Int): Fragment {
            return HelpFragment.newInstance(position)
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return contentTitles[position]
        }
    }

    class HelpFragment : Fragment() {
        var position = 0
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            position = if (arguments != null) requireArguments().getInt(ARG_POSITION) else 0
        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val view = inflater.inflate(R.layout.help_fragment, container, false)
            val htmlText = view.findViewById<WebView>(R.id.help_fragment_text)
            htmlText.settings.javaScriptEnabled = true
            htmlText.addJavascriptInterface(object : Any() {
                @JavascriptInterface
                fun sendLogs() {
                    MaterialDialog.Builder(activity!!)
                        .content(R.string.send_devs_question)
                        .positiveText(android.R.string.ok)
                        .negativeText(android.R.string.cancel)
                        .onPositive { dialog: MaterialDialog, which: DialogAction ->
                            CrLog.sendLogs(
                                activity as AppCompatActivity?,
                                "synapticwebb@gmail.com",
                                getString(R.string.app_name)
                            )
                        }
                        .show()
                }
            }, "SendLogsWrapper")
            //am pus imaginile și style-urile în main/assets. Ca urmare am setat base url la file:///android_asset/ și sursele
            //sunt doar numele fișierelor.
            htmlText.loadDataWithBaseURL(
                "file:///android_asset/",
                content[position]!!,
                "text/html",
                null,
                null
            )
            return view
        }

        companion object {
            const val ARG_POSITION = "arg_pos"
            fun newInstance(position: Int): HelpFragment {
                val fragment = HelpFragment()
                val args = Bundle()
                args.putInt(ARG_POSITION, position)
                fragment.arguments = args
                return fragment
            }
        }
    }

    companion object {
        const val NUM_PAGES = 6
        const val APP_NAME_PLACEHOLDER = "APP_NAME"
        var content = arrayOfNulls<String>(NUM_PAGES)
        var contentTitles = arrayOfNulls<String>(NUM_PAGES)
    }
}