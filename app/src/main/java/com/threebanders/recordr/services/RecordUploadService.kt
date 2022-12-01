package com.threebanders.recordr.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.http.FileContent
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.threebanders.recordr.Extras
import com.threebanders.recordr.R
import com.threebanders.recordr.common.Constants
import com.threebanders.recordr.ui.MainViewModel
import com.threebanders.recordr.ui.contact.ContactsListActivityMain
import com.threebanders.recordr.ui.settings.SettingsFragment
import core.threebanders.recordr.data.Recording
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class RecordUploadService : Service() {

    private var mainViewModel  = MainViewModel()

    override fun onBind(intent: Intent?): IBinder?  = null

    override fun onCreate() {
        super.onCreate()
        mainViewModel.loadRecordings()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Toast.makeText(this,"Service Called",Toast.LENGTH_LONG).show()
        val recording = intent?.getStringExtra("recording")
//        if(recording != null){
//            uploadFileToGDrive(File(recording))
//        }
        return START_STICKY
    }




    override fun onDestroy() {
        stopSelf()
        super.onDestroy()
    }

    private fun showTime() : String{
        val simpleDateFormat = SimpleDateFormat("HH:mm")
        return simpleDateFormat.format(Calendar.getInstance().timeInMillis)
    }
}