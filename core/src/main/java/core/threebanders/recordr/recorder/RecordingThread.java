package core.threebanders.recordr.recorder;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;

import core.threebanders.recordr.Core;

abstract class RecordingThread {
    static final int SAMPLE_RATE = 44100;
    protected final Recorder recorder;
    final int channels;
    final int bufferSize;
    final AudioRecord audioRecord;
    protected Context context;

    RecordingThread(Context context, String mode, Recorder recorder) throws RecordingException {
        this.context = context;
        channels = (mode.equals(Recorder.MONO) ? 1 : 2);
        this.recorder = recorder;
        bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, channels == 1 ?
                        AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);
        audioRecord = createAudioRecord();
        audioRecord.startRecording();
    }

    //e statică ca să poată fi apelată din CopyPcmToWav
    static void notifyOnError(Context context) {
        RecorderService service = RecorderService.getService();
        if (service != null) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null)
                nm.notify(RecorderService.NOTIFICATION_ID,
                        service.buildNotification(RecorderService.RECORD_ERROR, "Recorder failed."));
        }
    }

    @SuppressLint("MissingPermission")
    private AudioRecord createAudioRecord() throws RecordingException {
        AudioRecord audioRecord;
        int source = Integer.parseInt(Core.getInstance().getCache().source());
        try {
            audioRecord = new AudioRecord(source, SAMPLE_RATE,
                    channels == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT, bufferSize * 10);
        } catch (Exception e) { //La VOICE_CALL dă IllegalArgumentException. Aplicația nu se oprește, rămîne
            //hanging, nu înregistrează nimic.
            throw new RecordingException(e.getMessage());
        }

        if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            recorder.setSource(audioRecord.getAudioSource());
        }

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED)
            throw new RecordingException("Unable to initialize AudioRecord");

        return audioRecord;
    }

    void disposeAudioRecord() {
        audioRecord.stop();
        audioRecord.release();
    }
}
