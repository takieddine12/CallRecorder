package com.threebanders.recordr.ui.setup

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.MaterialDialog
import com.threebanders.recordr.R
import com.threebanders.recordr.ui.contact.ContactsListActivityMain

class SetupPowerFragment : Fragment() {
    private var parentActivity: SetupActivity? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.setup_power_fragment, container, false)
    }

    @SuppressLint("NewApi")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        parentActivity = activity as SetupActivity?
        val res = resources
        val dozeInfoText = parentActivity!!.findViewById<TextView>(R.id.doze_info_text)
        dozeInfoText.text =
            String.format(res.getString(R.string.doze_info), res.getString(R.string.app_name))
        val otherOptimizations =
            parentActivity!!.findViewById<TextView>(R.id.other_power_optimizations)
        otherOptimizations.text = String.format(
            res.getString(R.string.other_power_optimizations),
            res.getString(R.string.app_name)
        )
        val dozeInfo = parentActivity!!.findViewById<LinearLayout>(R.id.doze_info)
        if (parentActivity!!.checkResult and ContactsListActivityMain.Companion.POWER_OPTIMIZED != 0) {
            dozeInfo.visibility = View.VISIBLE
            val turnOffDoze = parentActivity!!.findViewById<Button>(R.id.turn_off_doze)
            turnOffDoze.setOnClickListener { //pentru a rezolva crash-ul e86a71db-dca0-4064-9bb5-466c6fd9dfce
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                val pm = requireActivity().packageManager
                if (intent.resolveActivity(pm) != null) startActivity(intent)
            }
        }
        val finish = parentActivity!!.findViewById<Button>(R.id.setup_power_finish)
        finish.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val pm = parentActivity!!.getSystemService(Context.POWER_SERVICE) as PowerManager
                if (pm != null && !pm.isIgnoringBatteryOptimizations(parentActivity!!.packageName)) MaterialDialog.Builder(
                    parentActivity!!
                )
                    .title(R.string.warning_title)
                    .content(R.string.optimization_still_active)
                    .positiveText(android.R.string.ok)
                    .icon(resources.getDrawable(R.drawable.warning))
                    .onPositive { dialog, which -> parentActivity!!.finish() }
                    .show() else parentActivity!!.finish()
            } else parentActivity!!.finish()
        }
    }
}
