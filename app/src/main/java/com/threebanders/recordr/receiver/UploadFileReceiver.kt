package com.threebanders.recordr.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.telephony.TelephonyManager
import android.view.View
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.threebanders.recordr.CrApp
import com.threebanders.recordr.R
import com.threebanders.recordr.services.RecordUploadService
import com.threebanders.recordr.ui.BaseActivity
import com.threebanders.recordr.ui.MainViewModel
import com.threebanders.recordr.ui.settings.SettingsFragment
import core.threebanders.recordr.data.Recording
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.withTestContext
import java.io.File
import java.lang.reflect.Type


class UploadFileReceiver : BroadcastReceiver() {
    var prevState = TelephonyManager.EXTRA_STATE_IDLE
    private var mainViewModel = MainViewModel()
    override fun onReceive(context: Context?, intent: Intent?) {

        // check if call end
        val bundle = intent!!.extras ?: return
        val state = bundle.getString(TelephonyManager.EXTRA_STATE)


        if (state == TelephonyManager.EXTRA_STATE_IDLE && prevState == TelephonyManager.EXTRA_STATE_IDLE) {
            // call ended
            val list = getDataFromSharedPreferences(context)
            mainViewModel.loadRecordings()
            mainViewModel.records.observeForever { recordings ->
               if (list?.size != recordings?.size){
                  CoroutineScope(Dispatchers.Main).launch {
                      val serviceIntent  = Intent(context,RecordUploadService::class.java)
                      intent.putExtra("recording", recordings[0].path)
                      if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                          context!!.startForegroundService(serviceIntent)
                      } else {
                          context!!.startService(serviceIntent)
                      }
                  }
               }
            }

        }

        prevState = state


    }

    private fun getDataFromSharedPreferences(context: Context?): List<Recording?>? {
        val gson = Gson()
        val productFromShared: List<Recording?>?
        val sharedPref: SharedPreferences? =
            context?.getSharedPreferences("PREFS_TAG", Context.MODE_PRIVATE)
        val jsonPreferences = sharedPref?.getString("PRODUCT_TAG", "")
        val type: Type = object : TypeToken<List<Recording?>?>() {}.type
        productFromShared = gson.fromJson<List<Recording?>>(jsonPreferences, type)
        return productFromShared
    }
}