package com.threebanders.recordr.ui.setup

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.threebanders.recordr.BuildConfig
import com.threebanders.recordr.CrApp
import com.threebanders.recordr.R
import com.threebanders.recordr.ui.contact.ContactsListActivityMain

class SetupEulaFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.setup_eula_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val parentActivity = activity as SetupActivity?
        val checkResult = parentActivity!!.checkResult
        val version = parentActivity.findViewById<TextView>(R.id.app_version)
        version.text = String.format(
            parentActivity.resources.getString(R.string.version_eula_screen),
            BuildConfig.VERSION_NAME
        )
        val showEula = parentActivity.findViewById<Button>(R.id.show_eula)
        showEula.setOnClickListener {
            startActivity(
                Intent(
                    activity, ShowEulaActivity::class.java
                )
            )
        }
        val cancelButton = parentActivity.findViewById<Button>(R.id.setup_confirm_cancel)
        cancelButton.setOnClickListener { parentActivity.cancelSetup() }
        val nextButton = parentActivity.findViewById<Button>(R.id.setup_confirm_next)
        nextButton.setOnClickListener(View.OnClickListener {
            val hasAccepted = parentActivity.findViewById<CheckBox>(R.id.has_accepted)
            if (!hasAccepted.isChecked || activity == null) return@OnClickListener
            val settings = (requireActivity().application as CrApp).core.prefs
            val editor = settings.edit()
            editor.putBoolean(ContactsListActivityMain.HAS_ACCEPTED_EULA, true)
            editor.apply()
            if (checkResult and ContactsListActivityMain.PERMS_NOT_GRANTED != 0) {
                val permissionsFragment = SetupPermissionsFragment()
                parentActivity.supportFragmentManager.beginTransaction()
                    .replace(R.id.setup_fragment_container, permissionsFragment)
                    .commitAllowingStateLoss()
            } else {
                val powerFragment = SetupPowerFragment()
                parentActivity.supportFragmentManager.beginTransaction()
                    .replace(R.id.setup_fragment_container, powerFragment)
                    .commitAllowingStateLoss()
            }
        })
    }
}