package core.threebanders.recordr.recorder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.SQLException;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import org.acra.ACRA;

import core.threebanders.recordr.Const;
import core.threebanders.recordr.Core;
import core.threebanders.recordr.CoreUtil;
import core.threebanders.recordr.CrLog;
import core.threebanders.recordr.data.Contact;
import core.threebanders.recordr.data.Recording;

public class RecorderService extends Service {
    public static final int NOTIFICATION_ID = 1;
    public static final int RECORD_AUTOMATICALLY = 1;
    public static final int RECORD_ERROR = 4;
    public static final int RECORD_SUCCESS = 5;
    static final String ACTION_STOP_SPEAKER = "net.synapticweb.callrecorder.STOP_SPEAKER";
    static final String ACTION_START_SPEAKER = "net.synapticweb.callrecorder.START_SPEAKER";
    static final String ACRA_PHONE_NUMBER = "phone_number";
    static final String ACRA_INCOMING = "incoming";
    private static final String CHANNEL_ID = "call_recorder_channel";
    private static RecorderService self;
    private String receivedNumPhone = null;
    private boolean privateCall = false;
    private Boolean incoming = null;
    private Recorder recorder;
    private Thread speakerOnThread;
    private AudioManager audioManager;
    private NotificationManager nm;
    private boolean speakerOn = false;
    private Contact contact = null;
    private String callIdentifier;
    private SharedPreferences settings;

    public static RecorderService getService() {
        return self;
    }

    @Override
    public IBinder onBind(Intent i) {
        return null;
    }

    public void onCreate() {
        super.onCreate();
        recorder = new Recorder(getApplicationContext());
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        nm = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        settings = Core.getInstance().getCache().getPrefs();

        self = this;
    }

    public Recorder getRecorder() {
        return recorder;
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void createChannel() {
        // The user-visible name of the channel.
        CharSequence name = "Call recorder";
        // The user-visible description of the channel.
        String description = "Call recorder controls";
        int importance = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
        // Configure the notification channel.
        mChannel.setDescription(description);
        mChannel.setShowBadge(false);
        mChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(mChannel);
    }

    public Notification buildNotification(int typeOfNotification, String message) {
        try {
            Intent goToActivity = new Intent(getApplicationContext(), Core.getNotifyGoToActivity());
            PendingIntent tapNotificationPi = PendingIntent.getActivity(getApplicationContext(),
                    0, goToActivity, PendingIntent.FLAG_IMMUTABLE);


            Intent sendBroadcast = new Intent(getApplicationContext(), ControlRecordingReceiver.class);
            Resources res = getApplicationContext().getResources();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                createChannel();
            NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                    .setSmallIcon(Core.getNotificationIcon())
                    .setContentTitle(callIdentifier + (incoming ? " (incoming)" : " (outgoing)"))
                    .setContentIntent(tapNotificationPi);

            switch (typeOfNotification) {
                //Acum nu se mai bazează pe speakerOn, recunoaște dacă difuzorul era deja pornit. speakerOn
                //a fost menținut deoarece în unele situații notificarea porneste prea devreme și isSpeakerphoneOn()
                //returnează false.
                case RECORD_AUTOMATICALLY:
                    if (audioManager.isSpeakerphoneOn() || speakerOn) {
                        sendBroadcast.setAction(ACTION_STOP_SPEAKER);
                        PendingIntent stopSpeakerPi = PendingIntent.getBroadcast(Core.getContext(), 0, sendBroadcast, 0);
                        builder.addAction(new NotificationCompat.Action.Builder(Core.getIconSpeakerOff(),
                                        "Stop speaker", stopSpeakerPi).build())
                                .setContentText("Recording&#8230; (speaker on)");
                    } else {
                        sendBroadcast.setAction(ACTION_START_SPEAKER);
                        PendingIntent startSpeakerPi = PendingIntent.getBroadcast(getApplicationContext(), 0, sendBroadcast, 0);
                        builder.addAction(new NotificationCompat.Action.Builder(Core.getIconSpeakerOn(),
                                        "Start speaker", startSpeakerPi).build())
                                .setContentText("Recording&#8230; (speaker off)");
                    }
                    break;

                case RECORD_ERROR:
                    builder.setColor(Color.RED)
                            .setColorized(true)
                            .setSmallIcon(Core.getIconFailure())
                            .setContentTitle("The call is not recorded")
                            .setContentText(message)
                            .setAutoCancel(true);
                    break;

                case RECORD_SUCCESS:
                    builder.setSmallIcon(Core.getIconSuccess())
                            .setContentText("The phone call was successfully recorded.")
                            .setAutoCancel(true);

            }

            return builder.build();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (intent.hasExtra(CallReceiver.ARG_NUM_PHONE))
            receivedNumPhone = intent.getStringExtra(CallReceiver.ARG_NUM_PHONE);
        incoming = intent.getBooleanExtra(CallReceiver.ARG_INCOMING, false);
        CrLog.log(CrLog.DEBUG, String.format("Recorder service started. Phone number: %s. Incoming: %s", receivedNumPhone, incoming));
        try {
            ACRA.getErrorReporter().putCustomData(ACRA_PHONE_NUMBER, receivedNumPhone);
            ACRA.getErrorReporter().putCustomData(ACRA_INCOMING, incoming.toString());
        } catch (IllegalStateException ignored) {
        }
        //de văzut dacă formarea ussd-urilor trimite ofhook dacă nu mai primim new_outgoing_call

        if (receivedNumPhone == null && incoming && Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
            privateCall = true;

        //se întîmplă numai la incoming, la outgoing totdeauna nr e null.
        if (receivedNumPhone != null) {
            contact = Contact.queryNumberInAppContacts(Core.getRepository(), receivedNumPhone);
            if (contact == null) {
                contact = Contact.queryNumberInPhoneContacts(receivedNumPhone, getContentResolver());
                if (contact == null) {
                    contact = new Contact(null, receivedNumPhone, "UNKNOWN CONTACT", null, CoreUtil.UNKNOWN_TYPE_PHONE_CODE);
                }
                try {
                    contact.save(Core.getRepository());
                } catch (SQLException exception) {
                    CrLog.log(CrLog.ERROR, "SQL exception: " + exception.getMessage());
                }
            }
        }

        if (contact != null) {
            String name = contact.getContactName();
            callIdentifier = name.equals("UNKNOWN CONTACT") ?
                    receivedNumPhone : name;
        } else if (privateCall)
            callIdentifier = "Hidden number";
        else
            callIdentifier = "Unknown phone number";

        try {
            CrLog.log(CrLog.DEBUG, "Recorder started in onStartCommand()");
            recorder.startRecording(receivedNumPhone);
            if (settings.getBoolean(Const.SPEAKER_USE, false))
                putSpeakerOn();
            Notification notification = buildNotification(RECORD_AUTOMATICALLY, "");
            if (notification != null) startForeground(NOTIFICATION_ID, notification);
        } catch (RecordingException e) {
            CrLog.log(CrLog.ERROR, "onStartCommand: unable to start recorder: " + e.getMessage() + " Stoping the service...");
            Notification notification = buildNotification(RECORD_ERROR, "Cannot start recorder. Maybe change audio source?");
            if (notification != null)
                startForeground(NOTIFICATION_ID, notification);
        }

        return START_NOT_STICKY;
    }

    private void resetState() {
        self = null;
    }

    //de aici: https://stackoverflow.com/questions/39725367/how-to-turn-on-speaker-for-incoming-call-programmatically-in-android-l
    void putSpeakerOn() {
        speakerOnThread = new Thread() {
            @Override
            public void run() {
                CrLog.log(CrLog.DEBUG, "Speaker has been turned on");
                try {
                    while (!Thread.interrupted()) {
                        audioManager.setMode(AudioManager.MODE_IN_CALL);
                        if (!audioManager.isSpeakerphoneOn())
                            audioManager.setSpeakerphoneOn(true);
                        sleep(500);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        speakerOnThread.start();
        speakerOn = true;
    }

    void putSpeakerOff() {
        if (speakerOnThread != null) {
            speakerOnThread.interrupt();
            CrLog.log(CrLog.DEBUG, "Speaker has been turned off");
        }
        speakerOnThread = null;
        if (audioManager != null && audioManager.isSpeakerphoneOn()) {
            audioManager.setMode(AudioManager.MODE_NORMAL);
            audioManager.setSpeakerphoneOn(false);
        }
        speakerOn = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        CrLog.log(CrLog.DEBUG, "RecorderService is stoping now...");

        putSpeakerOff();
        if (!recorder.isRunning() || recorder.hasError()) {
            onDestroyCleanUp();
            return;
        }

        recorder.stopRecording();
        Long contactId;

        if (privateCall) {
            contactId = Core.getRepository().getHiddenNumberContactId();
            if (contactId == null) { //încă nu a fost înregistrat un apel de pe număr ascuns
                Contact contact = new Contact();
                contact.setIsPrivateNumber();
                contact.setContactName("Hidden number");
                try {
                    contact.save(Core.getRepository());
                } catch (SQLException exc) {
                    CrLog.log(CrLog.ERROR, "SQL exception: " + exc.getMessage());
                    onDestroyCleanUp();
                    return;
                }
                contactId = contact.getId();
            }
        } else if (contact != null)
            contactId = contact.getId();

        else  //dacă nu e privat și contactul este null atunci nr e indisponibil.
            contactId = null;

        Recording recording = new Recording(null, contactId, recorder.getAudioFilePath(), incoming,
                recorder.getStartingTime(), System.currentTimeMillis(), recorder.getFormat(), false, recorder.getMode(),
                recorder.getSource());

        try {
            recording.save(Core.getRepository());
        } catch (SQLException exc) {
            CrLog.log(CrLog.ERROR, "SQL exception: " + exc.getMessage());
            onDestroyCleanUp();
            return;
        }

        nm.notify(NOTIFICATION_ID, buildNotification(RECORD_SUCCESS, ""));
        onDestroyCleanUp();
    }

    private void onDestroyCleanUp() {
        resetState();
        try {
            ACRA.getErrorReporter().clearCustomData();
        } catch (IllegalStateException ignored) {
        }
    }
}
