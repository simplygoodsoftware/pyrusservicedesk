package com.pyrus.pyrusservicedesk.audiocontroller.src.main.java.com.pyrus.audiocontroller;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

public class Constant {
    public static final int SOURCE = MediaRecorder.AudioSource.DEFAULT;
    public static final int SAMPLE_RATE = 16000;
    public static final int NUM_CHANNELS = 1;
    public static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    public static final int BUFFER_SIZE = 2 * AudioRecord.getMinBufferSize(
            SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
    public static final int FRAME_SIZE = 640;
    public static final int CHANNEL_FORMAT_OUT = AudioFormat.CHANNEL_OUT_MONO;
}
