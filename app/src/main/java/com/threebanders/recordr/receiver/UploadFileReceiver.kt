package com.threebanders.recordr.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.TelephonyManager
import android.widget.Toast
import com.threebanders.recordr.services.RecordUploadService
import com.threebanders.recordr.ui.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class UploadFileReceiver : BroadcastReceiver() {
    var prevState = TelephonyManager.EXTRA_STATE_IDLE
    private var mainViewModel = MainViewModel()
    override fun onReceive(context: Context?, intent: Intent?) {

        // check if call end
        val bundle = intent!!.extras ?: return
        val state = bundle.getString(TelephonyManager.EXTRA_STATE)

        if (state == TelephonyManager.EXTRA_STATE_IDLE && prevState == TelephonyManager.EXTRA_STATE_IDLE) {
            // call ended
            CoroutineScope(Dispatchers.Main).launch {
                delay(2000)
                mainViewModel.loadRecordings()
                mainViewModel.records.observeForever {
                    val serviceIntent  = Intent(context,RecordUploadService::class.java)
                    intent.putExtra("recording",it[0].path)
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                        context!!.startForegroundService(serviceIntent)
                    } else {
                        context!!.startService(serviceIntent)
                    }
                }
            }
        }

        prevState = state


    }
}