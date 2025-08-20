package com.pyrus.audiocontroller.record;

import android.media.AudioRecord;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pyrus.audiocontroller.opus.OpusEncoder;

import org.gagravarr.opus.OpusAudioData;
import org.gagravarr.opus.OpusFile;
import org.gagravarr.opus.OpusInfo;
import org.gagravarr.opus.OpusTags;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.pyrus.audiocontroller.Constant.AUDIO_FORMAT;
import static com.pyrus.audiocontroller.Constant.BUFFER_SIZE;
import static com.pyrus.audiocontroller.Constant.CHANNEL_CONFIG;
import static com.pyrus.audiocontroller.Constant.FRAME_SIZE;
import static com.pyrus.audiocontroller.Constant.NUM_CHANNELS;
import static com.pyrus.audiocontroller.Constant.SAMPLE_RATE;
import static com.pyrus.audiocontroller.Constant.SOURCE;

/**
 * The AudioRecordController class manages audio recording.
 */
public class AudioRecordController {

    private static final String TAG = AudioRecordController.class.getSimpleName();

    private static final SimpleDateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("'Recording_'ddMMyyyy'_'HHmmss", Locale.ENGLISH);
    private static final SimpleDateFormat US_DATE_FORMAT = new SimpleDateFormat("'Recording_'MMddyyyy'_'HHmmss", Locale.US);
    public static final double MIN_AUDIO_RECORD_DURATION_SECONDS = 1.;

    private final File mAudioFilesDir;

    private final ExecutorService mRecordScheduler;
    private AudioProgressListener mProgressListener;
    private AudioRecordListener mListener;
    private AudioCancelledListener mCancelledListener;

    @Nullable
    private File mPendingFile;
    private RecordState mState;

    /**
     * @param audioFilesDir   Folder where audio files stored.
     * @param recordScheduler Scheduler for audio record processing.
     */
    AudioRecordController(@NonNull File audioFilesDir,
                          @NonNull ExecutorService recordScheduler) {
        this.mAudioFilesDir = audioFilesDir;
        this.mRecordScheduler = recordScheduler;
        this.mState = RecordState.READY;
    }

    /**
     * Return base instance of the {@link AudioRecordController}.
     *
     * @param audioFilesDir Directory where audio files will be stored.
     */
    public static AudioRecordController init(@NonNull File audioFilesDir) {
        return new AudioRecordController(audioFilesDir, Executors.newSingleThreadExecutor());
    }

    /**
     * Set listener.
     *
     * @param listener The callback that will run when audio recording is
     *                 processed or ended.
     */
    public synchronized void setAudioRecordedListener(@NonNull AudioRecordListener listener) {
        mListener = listener;

        if (mState == RecordState.PENDING
                && mPendingFile != null) {
            listener.onAudioRecorded(mPendingFile);
            mPendingFile = null;
            mState = RecordState.READY;
        }
    }

    public synchronized void setProgressListener(@NonNull AudioProgressListener listener) {
        mProgressListener = listener;
    }

    public synchronized void setRecordCancelledListener(@NonNull AudioCancelledListener listener) {
        mCancelledListener = listener;
    }

    /**
     * Remove record listener.
     */
    public synchronized void removeRecordListener() {
        mListener = null;
    }

    /**
     * Remove progress listener.
     */
    public synchronized void removeProgressListener() {
        mProgressListener = null;
    }

    /**
     * Remove audion cancelled listener.
     */
    public synchronized void removeCancelledListener() {
        mCancelledListener = null;
    }

    /**
     * Start recording.
     */
    public synchronized void startRecord() {
        if (mState != RecordState.READY)
            return;
        mState = RecordState.RECORDING;

        String name;
        if (Locale.getDefault().equals(Locale.US)) {
            name = US_DATE_FORMAT.format(new Date(System.currentTimeMillis()));
        }
        else {
            name = DEFAULT_DATE_FORMAT.format(new Date(System.currentTimeMillis()));
        }
        final String fileName = name + ".opus";
        File newFile = new File(mAudioFilesDir, fileName);
        mRecordScheduler.submit(() -> recording(newFile));
    }

    /**
     * Stop recording.
     */
    public synchronized void stopRecord() {
        if (mState != RecordState.RECORDING)
            return;

        mState = RecordState.STOPPED;
    }

    public synchronized void cancelRecord() {
        if (mState != RecordState.RECORDING)
            return;

        mState = RecordState.CANCELLED;
    }

    private synchronized boolean isRecordStopped() {
        return mState == RecordState.STOPPED;
    }

    private synchronized boolean isRecordCancelled() {
        return mState == RecordState.CANCELLED;
    }

    private synchronized void updateRecordProgress(short[] buffer) {
        final AudioProgressListener listener = mProgressListener;
        if (listener != null)
            listener.onRecordingProgressUpdated(buffer);
    }

    private synchronized void onAudioRecorded(@NonNull File audioFile) {
        if (mListener == null) {
            Log.d("DFD", "controller onAudioRecorded mListener == null");
            mPendingFile = audioFile;
            mState = RecordState.PENDING;
        }
        else {
            Log.d("DFD", "controller onAudioRecorded send to mListener");
            mListener.onAudioRecorded(audioFile);
            mState = RecordState.READY;
        }
    }

    private synchronized void onAudioCancelled(@NonNull File audioFile) {
        audioFile.delete();
        if (mCancelledListener != null) {
            mCancelledListener.onAudioCancelled();
        }
        mState = RecordState.READY;
    }

    private void recording(@NonNull File target) {
        AudioRecord recorder = null;
        try {
            recorder = new AudioRecord(SOURCE, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);
            recorder.startRecording();

            target.getParentFile().mkdirs();
            target.createNewFile();

            final OpusInfo opusInfo = new OpusInfo();
            opusInfo.setNumChannels(NUM_CHANNELS);
            opusInfo.setSampleRate(SAMPLE_RATE);

            final OpusTags opusTags = new OpusTags();
            opusTags.setVendor("ServiceDesk");

            try (OutputStream outputStream = new FileOutputStream(target);
                 OpusFile file = new OpusFile(outputStream, opusInfo, opusTags)) {
                short[] inBuf = new short[FRAME_SIZE * NUM_CHANNELS];
                byte[] encodedData = new byte[FRAME_SIZE];

                OpusEncoder encoder = new OpusEncoder();
                encoder.init(SAMPLE_RATE, NUM_CHANNELS, OpusEncoder.OPUS_APPLICATION_VOIP);
                encoder.setBitrate(SAMPLE_RATE);

                double recordedSeconds = 0;

                while (!isRecordStopped() && !isRecordCancelled()) {
                    recorder.read(inBuf, 0, inBuf.length);
                    final int bytesEncoded = encoder.encode(inBuf, FRAME_SIZE, encodedData);
                    OpusAudioData data = new OpusAudioData(Arrays.copyOf(encodedData, bytesEncoded));
                    recordedSeconds += (double) data.getNumberOfSamples() / OpusAudioData.OPUS_GRANULE_RATE;
                    file.writeAudioData(data);
                    updateRecordProgress(inBuf);
                }
                Log.d("DFD", "controller onAudioRecorded");
                if (recordedSeconds > MIN_AUDIO_RECORD_DURATION_SECONDS && !isRecordCancelled()) {
                    Log.d("DFD", "controller onAudioRecorded 2");
                    onAudioRecorded(target);
                }
                else {
                    Log.d("DFD", "controller onAudioCancelled too small");
                    onAudioCancelled(target);
                }
            }
        }
        catch (Exception e) {
            Log.d("DFD", "error: " + e.getMessage());
            e.printStackTrace();
            Log.wtf(TAG, e);
            target.delete();
            synchronized (this) {
                mState = RecordState.READY;
            }
        }
        finally {
            if (recorder != null)
                recorder.release();
        }
    }

    private enum RecordState {
        READY,
        RECORDING,
        PENDING,
        STOPPED,
        CANCELLED
    }

}
