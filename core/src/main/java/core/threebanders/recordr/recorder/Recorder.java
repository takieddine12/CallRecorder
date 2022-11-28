package core.threebanders.recordr.recorder;

import static android.media.MediaRecorder.AudioSource.MIC;
import static android.media.MediaRecorder.AudioSource.VOICE_CALL;
import static android.media.MediaRecorder.AudioSource.VOICE_COMMUNICATION;
import static android.media.MediaRecorder.AudioSource.VOICE_RECOGNITION;

import android.content.Context;

import org.acra.ACRA;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import core.threebanders.recordr.Cache;
import core.threebanders.recordr.Core;

public class Recorder {
    public static final String WAV_FORMAT = "wav";
    public static final String AAC_HIGH_FORMAT = "aac_hi";
    public static final String AAC_MEDIUM_FORMAT = "aac_med";
    public static final String AAC_BASIC_FORMAT = "aac_bas";
    static final String MONO = "mono";
    private static final String ACRA_FORMAT = "format";
    private static final String ACRA_MODE = "mode";
    private static final String ACRA_SAVE_PATH = "save_path";
    private final String format;
    private final String mode;
    private final Context context;
    private File audioFile;
    private Thread recordingThread;
    private long startingTime;
    private int source;
    private boolean hasError = false;

    public Recorder(Context context) {
        this.context = context;
        Cache cache = Core.getInstance().getCache();
        format = cache.format();
        mode = cache.mode();
    }

    public long getStartingTime() {
        return startingTime;
    }

    public String getAudioFilePath() {
        return audioFile.getAbsolutePath();
    }

    public void startRecording(String phoneNumber) throws RecordingException {
        if (phoneNumber == null)
            phoneNumber = "";

        if (isRunning())
            stopRecording();
        String extension = format.equals(WAV_FORMAT) ? ".wav" : ".aac";
        File recordingsDir;

        if (Core.getInstance().getCache().storage().equals("private"))
            recordingsDir = context.getFilesDir();
        else {
            String filePath = Core.getInstance().getCache().storagePath();
            recordingsDir = (filePath == null) ? context.getExternalFilesDir(null) : new File(filePath);
            if (recordingsDir == null)
                recordingsDir = context.getFilesDir();
        }

        phoneNumber = phoneNumber.replaceAll("[()/.,* ;+]", "_");
        String fileName = "Recording" + phoneNumber +
                new SimpleDateFormat("-d-MMM-yyyy-HH-mm-ss", Locale.US).
                        format(new Date(System.currentTimeMillis())) + extension;
        audioFile = new File(recordingsDir, fileName);

        try {
            ACRA.getErrorReporter().putCustomData(ACRA_FORMAT, format);
            ACRA.getErrorReporter().putCustomData(ACRA_MODE, mode);
            ACRA.getErrorReporter().putCustomData(ACRA_SAVE_PATH, audioFile.getAbsolutePath());
        } catch (IllegalStateException ignored) {
        }

        if (format.equals(WAV_FORMAT))
            recordingThread = new Thread(new RecordingThreadWav(context, mode, this));
        else
            recordingThread = new Thread(new RecordingThreadAac(context, audioFile,
                    format, mode, this));

        recordingThread.start();
        startingTime = System.currentTimeMillis();
    }

    public void stopRecording() {
        if (recordingThread != null) {
            recordingThread.interrupt();
            recordingThread = null;
            if (format.equals(WAV_FORMAT)) {
                Thread copyPcmToWav = new Thread(new RecordingThreadWav.CopyPcmToWav(context,
                        audioFile, mode, this));
                copyPcmToWav.start();
            }
        }
    }

    public boolean isRunning() {
        return recordingThread != null && recordingThread.isAlive();
    }

    public String getFormat() {
        return format;
    }

    public String getMode() {
        return mode;
    }

    public String getSource() {
        switch (source) {
            case VOICE_RECOGNITION:
                return "Voice recognition";
            case VOICE_COMMUNICATION:
                return "Voice communication";
            case VOICE_CALL:
                return "Voice call";
            case MIC:
                return "Microphone";
            default:
                return "Source unrecognized";
        }
    }

    public void setSource(int source) {
        this.source = source;
    }

    public boolean hasError() {
        return hasError;
    }

    void setHasError() {
        this.hasError = true;
    }
}
