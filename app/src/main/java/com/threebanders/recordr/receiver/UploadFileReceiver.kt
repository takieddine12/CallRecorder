package com.threebanders.recordr.receiver

import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.http.FileContent
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.threebanders.recordr.Extras
import com.threebanders.recordr.R
import com.threebanders.recordr.common.Constants
import com.threebanders.recordr.ui.contact.ContactsListActivityMain
import com.threebanders.recordr.ui.settings.SettingsFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.random.Random


class UploadFileReceiver : BroadcastReceiver() {
    var prevState = TelephonyManager.EXTRA_STATE_IDLE
    private val FOLDER_NAME = Constants.APP_NAME
    override fun onReceive(context: Context?, intent: Intent?) {
        val sharedPreferences = context!!.getSharedPreferences("audioPrefs",Context.MODE_PRIVATE)
        val audioPath = sharedPreferences.getString("audioPath","")
        val bundle = intent!!.extras ?: return
        val state = bundle.getString(TelephonyManager.EXTRA_STATE)
        if (state == TelephonyManager.EXTRA_STATE_IDLE && prevState == TelephonyManager.EXTRA_STATE_IDLE) {
            if(audioPath!!.isNotEmpty()){
                uploadFileToGDrive(File(audioPath),context)
                context.getSharedPreferences("audioPrefs",MODE_PRIVATE).edit {
                    clear()
                }
            }
        }
        prevState = state

    }


    private fun uploadFileToGDrive(file: File,context: Context?) {
        try {
            CoroutineScope(Dispatchers.Main).launch{
                val drive = getDriveService(context)
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
                        showNotification(context)
                    }
                }.key
            }
        }
        catch (userAuthEx: UserRecoverableAuthIOException) {
            context!!.startActivity(
                userAuthEx.intent
            )
        }
        catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun getDriveService(context: Context?): Drive? {
        GoogleSignIn.getLastSignedInAccount(context!!)?.let { googleAccount ->
            val credential = GoogleAccountCredential.usingOAuth2(
                context, listOf(DriveScopes.DRIVE_FILE)
            )
            credential.selectedAccount = googleAccount.account!!
            return Drive.Builder(
                AndroidHttp.newCompatibleTransport(),
                JacksonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName(context.getString(R.string.app_name))
                .build()
        }
        return null
    }
    private fun showNotification(context: Context?){
        val pendingIntent: PendingIntent = Intent(context, ContactsListActivityMain::class.java).let { notificationIntent ->
            PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE) }

        val notification: Notification = NotificationCompat.Builder(context!!, Extras.NOTIFICATION_ID)
            .setContentTitle("Upload")
            .setContentText("File uploaded successfully...")
            .setSmallIcon(R.drawable.ic_baseline_circle_notifications_24)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(Random.nextInt(),notification)
    }
}