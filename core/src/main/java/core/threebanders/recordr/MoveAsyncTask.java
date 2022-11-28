package core.threebanders.recordr;

import android.app.Activity;
import android.os.AsyncTask;

import androidx.annotation.NonNull;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.lang.ref.WeakReference;

import core.threebanders.recordr.data.Recording;
import core.threebanders.recordr.data.Repository;

public class MoveAsyncTask extends AsyncTask<Recording, Integer, Boolean> {
    private final String path;
    private final long totalSize;
    private final Repository repository;
    private final WeakReference<Activity> activityRef;
    public long alreadyCopied = 0;
    private MaterialDialog dialog;

    MoveAsyncTask(Repository repository, String folderPath, long totalSize, Activity activity) {
        this.path = folderPath;
        this.totalSize = totalSize;
        this.repository = repository;
        activityRef = new WeakReference<>(activity);
    }

    public void callPublishProgress(int progress) {
        publishProgress(progress);
    }

    @Override
    protected void onPreExecute() {
        dialog = new MaterialDialog.Builder(activityRef.get())
                .title("Progress")
                .content("Moving recordings&#8230;")
                .progress(false, 100, true)
                .negativeText("Cancel")
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        cancel(true);
                    }
                })
                .build();
        dialog.setCancelable(false);
        dialog.show();
    }

    @Override
    protected void onProgressUpdate(Integer... integers) {
        dialog.setProgress(integers[0]);
    }

    @Override
    protected void onCancelled() {
        new MaterialDialog.Builder(activityRef.get())
                .title("Warning")
                .content("The move was canceled. Some files might be corrupted or missing.")
                .positiveText("OK")
                .show();
    }

    @Override
    protected void onPostExecute(Boolean result) {
        dialog.dismiss();
        if (result) {
            new MaterialDialog.Builder(activityRef.get())
                    .title("Success")
                    .content("The recording(s) were successfully moved.")
                    .positiveText("OK")
                    .show();
        } else {
            new MaterialDialog.Builder(activityRef.get())
                    .title("Error")
                    .content("An error occurred while moving the recording(s). Some files might be corrupted or missing.")
                    .positiveText("OK")
                    .show();
        }
    }

    @Override
    protected Boolean doInBackground(Recording... recordings) {
        for (Recording recording : recordings) {
            try {
                recording.move(repository, path, this, totalSize);
                if (isCancelled())
                    break;
            } catch (Exception exc) {
                CrLog.log(CrLog.ERROR, "Error moving the recording(s): " + exc.getMessage());
                return false;
            }
        }
        return true;
    }
}