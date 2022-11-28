package com.threebanders.recordr

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.threebanders.recordr.ui.contact.ContactsListActivityMain
import core.threebanders.recordr.Core
import org.acra.ACRA
import org.acra.annotation.AcraCore
import org.acra.annotation.AcraHttpSender
import org.acra.data.StringFormat
import org.acra.sender.HttpSender

@AcraCore(reportFormat = StringFormat.KEY_VALUE_LIST)
@AcraHttpSender(uri = "http://crashes.infopsihologia.ro", httpMethod = HttpSender.Method.POST)
class CrApp : Application() {
    lateinit var core: Core
        private set

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)

        if (!BuildConfig.DEBUG) {
            ACRA.init(this)
        }
    }

    init {
        instance = this
    }

    override fun onCreate() {
        super.onCreate()

        createNotification()
        core = Core.Builder.newInstance()
            .setContext(applicationContext)
            .setNotifyGoToActivity(ContactsListActivityMain::class.java)
            .setNotificationIcon(R.drawable.notification_icon)
            .setIconSpeakerOff(R.drawable.speaker_phone_off)
            .setIconSuccess(R.drawable.speaker_phone_on)
            .setIconFailure(R.drawable.notification_icon_error)
            .setIconSpeakerOn(R.drawable.speaker_phone_on)
            .setVersionCode(BuildConfig.VERSION_CODE)
            .setVersionName(BuildConfig.VERSION_NAME)
            .build()
    }

    companion object {
        lateinit var instance: CrApp
            private set
    }

    private fun createNotification(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val notificationChannel = NotificationChannel(
                Extras.NOTIFICATION_ID,Extras.NOTIFICATION_STRING, NotificationManager.IMPORTANCE_DEFAULT)
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }
}