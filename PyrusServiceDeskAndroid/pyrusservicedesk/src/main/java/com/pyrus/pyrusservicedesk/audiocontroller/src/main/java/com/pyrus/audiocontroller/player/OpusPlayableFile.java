package com.pyrus.pyrusservicedesk.audiocontroller.src.main.java.com.pyrus.audiocontroller.player;

import static com.pyrus.pyrusservicedesk.audiocontroller.src.main.java.com.pyrus.audiocontroller.Constant.AUDIO_FORMAT;
import static com.pyrus.pyrusservicedesk.audiocontroller.src.main.java.com.pyrus.audiocontroller.Constant.CHANNEL_FORMAT_OUT;
import static com.pyrus.pyrusservicedesk.audiocontroller.src.main.java.com.pyrus.audiocontroller.Constant.FRAME_SIZE;
import static com.pyrus.pyrusservicedesk.audiocontroller.src.main.java.com.pyrus.audiocontroller.Constant.NUM_CHANNELS;
import static com.pyrus.pyrusservicedesk.audiocontroller.src.main.java.com.pyrus.audiocontroller.Constant.SAMPLE_RATE;

import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import androidx.annotation.NonNull;

import com.pyrus.pyrusservicedesk.audiocontroller.src.main.java.com.pyrus.audiocontroller.opus.OpusDecoder;
import com.pyrus.pyrusservicedesk.audiocontroller.src.main.java.org.gagravarr.opus.OpusAudioData;
import com.pyrus.pyrusservicedesk.audiocontroller.src.main.java.org.gagravarr.opus.OpusFile;


import java.io.File;
import java.util.concurrent.ExecutorService;

/**
 * OpusPlayableFile class manages playing for single OPUS audio file.
 */
public class OpusPlayableFile {

    private static final String TAG = OpusPlayableFile.class.getSimpleName();

    public final String uid;

    private final File audioFile;

    private long currentPacketPosition;

    private boolean paused = true;

    private boolean stopped = true;


    /**
     * @param uid       Id of the playable file.
     * @param audioFile Playable file.
     */
    public OpusPlayableFile(@NonNull String uid, @NonNull File audioFile) {
        this.uid = uid;
        this.audioFile = audioFile;
    }

    /**
     * Returns true if file is playing.
     */
    public synchronized boolean isPlaying() {
        return !paused && !stopped;
    }

    /**
     * Pauses playback.
     */
    public synchronized void pause() {
        paused = true;
        stopped = false;
    }

    /**
     * Stops playback.
     */
    public synchronized void stop() {
        paused = false;
        stopped = true;
        currentPacketPosition = 0L;
    }

    /**
     * Starts playback.
     *
     * @param controller     Instance of the {@link AudioPlayerController}.
     * @param mPlayScheduler ExecutorService for playing audio in background
     *                       thread.
     */
    public synchronized void play(@NonNull AudioPlayerController controller, ExecutorService mPlayScheduler) {
        if (isPlaying())
            return;
        setPlaying(true);
        mPlayScheduler.submit(() -> {
            AudioTrack track = null;
            try {
                track = new AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        SAMPLE_RATE,
                        CHANNEL_FORMAT_OUT,
                        AUDIO_FORMAT,
                        AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_FORMAT_OUT, AUDIO_FORMAT),
                        AudioTrack.MODE_STREAM);

                OpusDecoder decoder = new OpusDecoder();
                decoder.init(SAMPLE_RATE, NUM_CHANNELS);

                byte[] outBuf = new byte[FRAME_SIZE * NUM_CHANNELS * 2];

                track.play();

                try (OpusFile opus = new OpusFile(audioFile)) {
                    OpusAudioData packet;
                    long packetPosition = 0;
                    while (packetPosition < currentPacketPosition) {
                        opus.getNextAudioPacket();
                        packetPosition++;
                    }
                    while ((packet = opus.getNextAudioPacket()) != null) {
                        currentPacketPosition = packetPosition;

                        if (isPaused() || isStopped()) {
                            break;
                        }

                        final byte[] data = packet.getData();
                        final int decoded = decoder.decode(data, outBuf, FRAME_SIZE);
                        track.write(outBuf, 0, decoded * 2);

                        controller.onAudioProgressUpdate(uid, packetPosition++ * packet.getNumberOfSamples() / 48.);
                    }
                }
                catch (Exception e) {
                    Log.wtf(TAG, e);
                }
                finally {
                    final boolean paused = isPaused();
                    setPlaying(false);
                    if (paused) {
                        controller.onAudioPaused(uid);
                    }
                    else {
                        controller.onAudioStop(uid);
                        currentPacketPosition = 0L;
                    }
                }
            }
            catch (Exception e) {
                Log.wtf(TAG, e);
            }
            finally {
                if (track != null)
                    track.release();
            }
        });
    }

    private synchronized boolean isStopped() {
        return stopped;
    }

    private synchronized boolean isPaused() {
        return paused;
    }

    private synchronized void setPlaying(boolean playing) {
        if (playing) {
            paused = false;
            stopped = false;
        }
        else {
            paused = true;
            stopped = true;
        }
    }

}
