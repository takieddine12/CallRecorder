package core.threebanders.recordr;

import static android.media.MediaRecorder.AudioSource.VOICE_RECOGNITION;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Cache {
    public static final String ENABLED = "enabled";
    public static final String SOURCE = "source";
    public static final String FORMAT = "format";
    public static final String MODE = "mode";
    public static final String STORAGE = "storage";
    public static final String STORAGE_PATH = "public_storage_path";
    public static final String RECORDINGS_LIST = "RECORDINGS_LIST";
    private static Cache cache;
    private SharedPreferences prefs;

    public static Cache getInstance(Context context) {
        if (cache == null) {
            cache = new Cache();
            cache.prefs = PreferenceManager.getDefaultSharedPreferences(context);
        }

        return cache;
    }

    public SharedPreferences getPrefs() {
        return prefs;
    }

    public boolean enabled() {
        return prefs.getBoolean(ENABLED, true);
    }

    public void format(String format) {
        prefs.edit().putString(FORMAT, format).apply();
    }

    public String format() {
        return prefs.getString(FORMAT, "");
    }

    public void mode(String format) {
        prefs.edit().putString(MODE, format).apply();
    }

    public String mode() {
        return prefs.getString(MODE, "");
    }

    public void storage(String format) {
        prefs.edit().putString(STORAGE, format).apply();
    }

    public String storage() {
        return prefs.getString(STORAGE, "");
    }

    public String storagePath() {
        return prefs.getString(STORAGE_PATH, null);
    }

    public void source(String format) {
        prefs.edit().putString(SOURCE, format).apply();
    }

    public String source() {
        return prefs.getString(SOURCE, String.valueOf(VOICE_RECOGNITION));
    }
}
