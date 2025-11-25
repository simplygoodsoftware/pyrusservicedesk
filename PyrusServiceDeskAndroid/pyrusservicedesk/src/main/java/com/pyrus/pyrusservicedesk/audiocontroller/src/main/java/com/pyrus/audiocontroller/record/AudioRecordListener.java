package com.pyrus.pyrusservicedesk.audiocontroller.src.main.java.com.pyrus.audiocontroller.record;

import java.io.File;

@FunctionalInterface
public interface AudioRecordListener {

    /**
     * Notify about completion of recording the file.
     *
     * @param audioFile Recorded audio file.
     */
    void onAudioRecorded(File audioFile);

}
