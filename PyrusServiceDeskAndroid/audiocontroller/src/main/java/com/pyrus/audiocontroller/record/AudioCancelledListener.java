package com.pyrus.audiocontroller.record;

@FunctionalInterface
public interface AudioCancelledListener {

    /**
     * Notify about cancelled audio
     */
    void onAudioCancelled();

}
