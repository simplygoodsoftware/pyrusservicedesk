package com.pyrus.audiocontroller.player;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;

public interface AudioPlayerListener {

    /**
     * Notify listener about new event.
     * Note: all events coming from background thread, except event with
     * {@link AudioPlayerController.AudioPlayerState#NOT_READY} state.
     *
     * @param event Event with current state of playable file.
     */
    void onNewEvent(@NonNull AudioPlayerEvent event);

    /**
     * Return UID of the listener.
     */
    String getUid();

    /**
     * Return file of the listener.
     */
    File getFile();

}
