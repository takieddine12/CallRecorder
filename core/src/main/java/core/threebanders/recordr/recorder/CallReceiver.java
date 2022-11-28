package core.threebanders.recordr.recorder;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;

import core.threebanders.recordr.Core;


public class CallReceiver extends BroadcastReceiver {
    public static final String ARG_NUM_PHONE = "arg_num_phone";
    public static final String ARG_INCOMING = "arg_incoming";
    private static final String TAG = "CallRecorder";
    private static boolean serviceStarted = false; //Fiind statică, dacă se fac 2 apeluri simultan numai primul poate porni
    //serviciul de recording. Dacă nu ar fi statică s-ar putea porni simultan mai multe servicii. Asta e un lucru rău, pentru
    //că de ex. dacă se sună de pe un nr în timp ce se vorbește cu un altul, dacă userul răspunde la al doilea apel primul e pus
    //pe hold. Cînd userul îi închide celui de-al doilea nu se primește nicio stare idle, ceea ce face ca al doilea serviciu să
    //rămînă pornit fără posibilitate de oprire.
    private static ComponentName serviceName = null;

    public CallReceiver() {
        super();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle;
        String state;
        String incomingNumber;
        String action = intent.getAction();

        if (action != null && action.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {

            if ((bundle = intent.getExtras()) != null) {
                state = bundle.getString(TelephonyManager.EXTRA_STATE);
                Log.d(TAG, intent.getAction() + " " + state);

                //acum serviciul este pornit totdeauna în extra_state_ringing (pentru ca userul să aibă posibilitatea
                // în cazul nr necunoscute să pornească înregistrarea înainte de începerea convorbirii),
                if (state != null && state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                    //în pie+ va fi întotdeauna null. În celelalte versiuni va conține nr, null însemnănd nr privat.
                    incomingNumber = bundle.getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
                    boolean isEnabled = Core.getInstance().getCache().enabled();
                    Log.d(TAG, "Incoming number: " + incomingNumber);
                    if (!serviceStarted && isEnabled) {
                        Intent intentService = new Intent(context, RecorderService.class);
                        serviceName = intentService.getComponent();
                        intentService.putExtra(ARG_NUM_PHONE, incomingNumber);
                        intentService.putExtra(ARG_INCOMING, true);
                        //https://stackoverflow.com/questions/46445265/android-8-0-java-lang-illegalstateexception-not-allowed-to-start-service-inten
                        //Bugul a fost detectat cu ACRA, nu apare pe dispozitivele mele
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            context.startForegroundService(intentService);
                        else
                            context.startService(intentService);
                        serviceStarted = true;
                    }
                } else if (state != null && state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                    boolean isEnabled = Core.getInstance().getCache().enabled();
                    //dacă serviciul nu e pornit înseamnă că e un apel outgoing.
                    if (!serviceStarted && isEnabled) { //outgoing
                        Intent intentService = new Intent(context, RecorderService.class);
                        serviceName = intentService.getComponent();
                        intentService.putExtra(ARG_INCOMING, false);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            context.startForegroundService(intentService);
                        else
                            context.startService(intentService);
                        serviceStarted = true;
                    }
                } else if (state != null && state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                    if (serviceStarted) {
                        Intent stopIntent = new Intent(context, RecorderService.class);
                        stopIntent.setComponent(serviceName);
                        context.stopService(stopIntent);
                        serviceStarted = false;
                    }
                    serviceName = null;
                }
            }
        }
    }

}
