package com.pyrus.audiocontroller.player;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


import org.gagravarr.opus.OpusFile;
import org.gagravarr.opus.OpusStatistics;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The AudioPlayerController class manages audio playing.
 */
public class AudioPlayerController {

    private static final String TAG = AudioPlayerController.class.getSimpleName();

    private final ExecutorService mPlayScheduler;
    private final ExecutorService mComputationScheduler;
    private final Map<String, Set<AudioPlayerListener>> mListeners;
    private final Map<String, AudioPlayerEvent> mEvents;

    @Nullable
    private OpusPlayableFile playableFile;

    /**
     * @param playScheduler        Scheduler for audio file playing.
     * @param computationScheduler Scheduler for audio info processing.
     */
    AudioPlayerController(@NonNull ExecutorService playScheduler,
                          @NonNull ExecutorService computationScheduler) {
        this.mPlayScheduler = playScheduler;
        this.mComputationScheduler = computationScheduler;
        this.mListeners = new HashMap<>();
        this.mEvents = new HashMap<>();
    }

    /**
     * Returns base instance of the {@link AudioPlayerController}.
     */
    public static AudioPlayerController init() {
        return new AudioPlayerController(Executors.newSingleThreadExecutor(),
                                         Executors.newCachedThreadPool());
    }

    /**
     * Adds listener.
     *
     * @param listener The listener which receive {@link AudioPlayerEvent}s from
     *                 controller.
     */
    public synchronized void addListener(AudioPlayerListener listener) {
        if (mListeners.get(listener.getUid()) == null)
            mListeners.put(listener.getUid(), new HashSet<>());
        mListeners.get(listener.getUid()).add(listener);
        AudioPlayerEvent event = mEvents.get(listener.getUid());
        if (event == null) {
            sendEvent(listener.getUid(), new AudioPlayerEvent());
            calculateDuration(listener.getUid(), listener.getFile());
        }
        else
            sendEvent(listener.getUid(), event);
    }

    /**
     * Removes listener.
     *
     * @param listener The listener which receive {@link AudioPlayerEvent}s from
     *                 controller.
     */
    public synchronized void removeListener(AudioPlayerListener listener) {
        if (mListeners.get(listener.getUid()) == null)
            return;
        mListeners.get(listener.getUid()).remove(listener);
    }

    public synchronized void togglePlayback(String uid, final File audioFile) {
        if (mEvents.get(uid) == null) {
            sendEvent(uid, new AudioPlayerEvent());
            calculateDuration(uid, audioFile);
        }

        if (playableFile != null && uid.equals(playableFile.uid)) {
            if (playableFile.isPlaying()) {
                playableFile.pause();
            }
            else {
                playableFile.play(this, mPlayScheduler);
            }
            return;
        }

        stop();
        playableFile = new OpusPlayableFile(uid, audioFile);
        playableFile.play(this, mPlayScheduler);
    }

    /**
     * Stops playback of current file and clear progress.
     */
    public synchronized void stop() {
        if (playableFile != null) {
            playableFile.stop();
            // Clear progress of stopped or paused file.
            onAudioStop(playableFile.uid);
        }
    }

    /**
     * Stops playback of file if playing.
     *
     * @param uid Id of the playing file.
     */
    public synchronized void stopIfPlaying(String uid) {
        if (playableFile == null || !uid.equals(playableFile.uid))
            return;
        playableFile.stop();
    }

    synchronized void onAudioStop(String id) {
        AudioPlayerEvent lastEvent = mEvents.get(id);
        if (lastEvent == null)
            return;
        sendEvent(id, lastEvent.stopped());
    }

    synchronized void onAudioProgressUpdate(String id, double progressInMillis) {
        AudioPlayerEvent lastEvent = mEvents.get(id);
        if (lastEvent == null)
            return;
        sendEvent(id, lastEvent.playing(progressInMillis));
    }

    synchronized void onAudioPaused(String id) {
        AudioPlayerEvent lastEvent = mEvents.get(id);
        if (lastEvent == null)
            return;
        sendEvent(id, lastEvent.paused());
    }

    private synchronized void sendEvent(@NonNull String id, @NonNull AudioPlayerEvent newEvent) {
        Collection<AudioPlayerListener> listeners = mListeners.get(id);
        if (listeners == null)
            return;
        mEvents.put(id, newEvent);
        for (AudioPlayerListener listener : listeners)
            listener.onNewEvent(newEvent);
    }

    private synchronized void calculateDuration(final String id, final File file) {
        mComputationScheduler.submit(() -> {
            final OpusStatistics statistics;
            try (OpusFile opus = new OpusFile(file)) {
                statistics = new OpusStatistics(opus);
                statistics.calculate();
                synchronized (AudioPlayerController.this) {
                    AudioPlayerEvent lastEvent = mEvents.get(id);
                    double durationInMillis = statistics.getAudioPacketsCount()
                            * statistics.getAvgPacketDuration();
                    sendEvent(id, lastEvent.durationCalculated(durationInMillis));
                }
            }
            catch (Exception e) {
                Log.wtf(TAG, e);
            }
        });
    }

    public enum AudioPlayerState {
        STOPPED, PLAYING, PAUSED, NOT_READY, DURATION_CALCULATED, ERROR
    }
}
