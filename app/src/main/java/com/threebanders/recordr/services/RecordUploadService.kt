package com.threebanders.recordr.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
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
import kotlin.random.Random

class RecordUploadService : Service() {

    private var mainViewModel  = MainViewModel()
    private val FOLDER_NAME = Constants.APP_NAME
    override fun onBind(intent: Intent?): IBinder?  = null

    override fun onCreate() {
        super.onCreate()
        mainViewModel.loadRecordings()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {


        mainViewModel.records.observeForever {
          // val recording = intent?.getStringExtra("recording")
            if(it.size != 0){
                uploadFileToGDrive(File(it[0].path))
                showNotification()
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun uploadFileToGDrive(file: File) {
        try {
          CoroutineScope(Dispatchers.Main).launch{
              val drive = getDriveService()
              var folderId = ""
              withContext(Dispatchers.IO) {
                  val gFolder = com.google.api.services.drive.model.File()
                  gFolder.name = FOLDER_NAME
                  gFolder.mimeType = "application/vnd.google-apps.folder"

                  launch {
                      val fileList = drive?.Files()?.list()
                          ?.setQ("mimeType='application/vnd.google-apps.folder' and trashed=false and name='$FOLDER_NAME'")
                          ?.execute()

                      folderId = if (fileList?.files?.isEmpty() == true) {
                          val folder = drive.Files().create(gFolder)?.setFields("id")?.execute()
                          folder?.id ?: ""
                      } else {
                          fileList?.files?.get(0)?.id ?: ""
                      }
                  }
              }.join()
              withContext(Dispatchers.IO) {
                  launch {
                      val gFile = com.google.api.services.drive.model.File()
                      gFile.name = file.name
                      gFile.parents = mutableListOf(folderId)
                      val fileContent = FileContent("audio/wav", file)
                      drive?.Files()?.create(gFile, fileContent)?.setFields("id, parents")
                          ?.execute()
                  }
              }.key
          }
        }
        catch (userAuthEx: UserRecoverableAuthIOException) {
            startActivity(
                userAuthEx.intent
            )
        }
        catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun getDriveService(): Drive? {
        GoogleSignIn.getLastSignedInAccount(this)?.let { googleAccount ->
            val credential = GoogleAccountCredential.usingOAuth2(
               this, listOf(DriveScopes.DRIVE_FILE)
            )
            credential.selectedAccount = googleAccount.account!!
            return Drive.Builder(
                AndroidHttp.newCompatibleTransport(),
                JacksonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName(getString(R.string.app_name))
                .build()
        }
        return null
    }

    private fun showNotification(){

        val pendingIntent: PendingIntent = Intent(this, ContactsListActivityMain::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE)
            }

        val notification: Notification = NotificationCompat.Builder(this, Extras.NOTIFICATION_ID)
            .setContentTitle("Upload")
            .setContentText("Uploading File to google drivce")
            .setSmallIcon(R.drawable.ic_baseline_circle_notifications_24)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(Random.nextInt() + 1 , notification)
    }

    override fun onDestroy() {
        stopSelf()
        super.onDestroy()
    }
}