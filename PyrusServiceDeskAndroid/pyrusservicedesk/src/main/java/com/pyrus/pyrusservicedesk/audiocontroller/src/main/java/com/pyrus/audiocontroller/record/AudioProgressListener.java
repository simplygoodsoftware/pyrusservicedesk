package com.pyrus.pyrusservicedesk.audiocontroller.src.main.java.com.pyrus.audiocontroller.record;

@FunctionalInterface
public interface AudioProgressListener {

    /**
     * Notify about new recorded segment.
     *
     * @param recordedSegmentValues Recorded segment raw values
     *                              (without any processing).
     */
    void onRecordingProgressUpdated(short[] recordedSegmentValues);

}
