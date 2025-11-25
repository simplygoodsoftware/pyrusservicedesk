package com.pyrus.pyrusservicedesk.audiocontroller.src.main.java.com.pyrus.audiocontroller.record;

@FunctionalInterface
public interface AudioCancelledListener {

    /**
     * Notify about cancelled audio
     */
    void onAudioCancelled();

}
