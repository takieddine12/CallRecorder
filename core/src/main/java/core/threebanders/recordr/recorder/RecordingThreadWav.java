package core.threebanders.recordr.recorder;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import core.threebanders.recordr.CrLog;

class RecordingThreadWav extends RecordingThread implements Runnable {
    private static final int BITS_PER_SAMPLE = 16;
    private static final int HEADER_REMAINING = 36;
    private static final String TMP_FILE_NAME = "recording_tmp.raw";
    private final File tmpFile;

    RecordingThreadWav(Context context, String mode, Recorder recorder) throws RecordingException {
        super(context, mode, recorder);
        tmpFile = new File(context.getFilesDir(), TMP_FILE_NAME);
    }

    @Override
    public void run() {
        try (FileOutputStream outputStream = new
                FileOutputStream(tmpFile)) {
            while (!Thread.interrupted()) {
                byte[] data = new byte[bufferSize];
                int length = audioRecord.read(data, 0, bufferSize);

                if (length < 0) {
                    throw new RecordingException("Recorder failed. Aborting...");
                }

                outputStream.write(data);
            }
        } catch (RecordingException | IOException e) {
            if (!tmpFile.delete()) {
                CrLog.log(CrLog.ERROR, "Cannot delete incomplete temp pcm file.");
            }
            recorder.setHasError();
            notifyOnError(context);
        } finally {
            disposeAudioRecord();
        }
    }

    static class CopyPcmToWav implements Runnable {
        private final File wavFile;
        private final int channels;
        private final Recorder recorder;
        private final Context context;

        CopyPcmToWav(Context context, File wavFile, String mode, Recorder recorder) {
            this.wavFile = wavFile;
            this.recorder = recorder;
            channels = mode.equals(Recorder.MONO) ? 1 : 2;
            this.context = context;
        }

        @Override
        public void run() {
            long totalAudioLen, totalDataLen;
            long byteRate = (long) SAMPLE_RATE * channels * BITS_PER_SAMPLE / 8;
            byte[] buffer = new byte[1048576];
            File tmpFile = new File(context.getFilesDir(), TMP_FILE_NAME);

            try (FileInputStream tmpInput = new FileInputStream(tmpFile);
                 FileOutputStream wavOutput = new FileOutputStream(wavFile)
            ) {
                totalAudioLen = tmpInput.getChannel().size();
                totalDataLen = totalAudioLen + HEADER_REMAINING;
                writeWaveFileHeader(wavOutput, totalAudioLen, totalDataLen, byteRate);

                while (tmpInput.read(buffer) != -1) {
                    wavOutput.write(buffer);
                }

            } catch (IOException e) {
                if (!tmpFile.delete())
                    CrLog.log(CrLog.ERROR, "Error while deleting temp pcm file on exception.");
                if (!wavFile.delete())
                    CrLog.log(CrLog.ERROR, "Error while deleting wav file on exception.");
                recorder.setHasError();
                notifyOnError(context);
            } finally {
                if (!tmpFile.delete())
                    CrLog.log(CrLog.ERROR, "Error while deleting temp pcm file on normal exit.");
            }
        }

        private void writeWaveFileHeader
                (FileOutputStream out, long totalAudioLen,
                 long totalDataLen, long byteRate) throws IOException {
            byte[] header = new byte[44];

            header[0] = 'R'; // RIFF/WAVE header
            header[1] = 'I';
            header[2] = 'F';
            header[3] = 'F';
            header[4] = (byte) (totalDataLen & 0xff);
            header[5] = (byte) ((totalDataLen >> 8) & 0xff);
            header[6] = (byte) ((totalDataLen >> 16) & 0xff);
            header[7] = (byte) ((totalDataLen >> 24) & 0xff);
            header[8] = 'W';
            header[9] = 'A';
            header[10] = 'V';
            header[11] = 'E';
            header[12] = 'f'; // 'fmt ' chunk
            header[13] = 'm';
            header[14] = 't';
            header[15] = ' ';
            header[16] = 16; // 4 bytes: size of 'fmt ' chunk
            header[17] = 0;
            header[18] = 0;
            header[19] = 0;
            header[20] = 1; // format = 1
            header[21] = 0;
            header[22] = (byte) channels;
            header[23] = 0;
            header[24] = (byte) (SAMPLE_RATE & 0xff);
            header[25] = (byte) ((SAMPLE_RATE >> 8) & 0xff);
            header[26] = (byte) ((SAMPLE_RATE >> 16) & 0xff);
            header[27] = (byte) ((SAMPLE_RATE >> 24) & 0xff);
            header[28] = (byte) (byteRate & 0xff);
            header[29] = (byte) ((byteRate >> 8) & 0xff);
            header[30] = (byte) ((byteRate >> 16) & 0xff);
            header[31] = (byte) ((byteRate >> 24) & 0xff);
            header[32] = (byte) (channels * BITS_PER_SAMPLE / 8);
            header[33] = 0;
            header[34] = BITS_PER_SAMPLE;
            header[35] = 0;
            header[36] = 'd';
            header[37] = 'a';
            header[38] = 't';
            header[39] = 'a';
            header[40] = (byte) (totalAudioLen & 0xff);
            header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
            header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
            header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

            out.write(header, 0, 44);
        }
    }
}

