package com.pyrus.pyrusservicedesk.audiocontroller.src.main.java.com.pyrus.audiocontroller;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.os.CancellationSignal;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Wav {

    private static final String TAG = Wav.class.getSimpleName();

    private static void writeWavHeader(OutputStream out, short channels, int sampleRate, short bitDepth) throws IOException {
        // Convert the multi-byte integers to raw bytes in little endian format as required by the spec
        byte[] littleBytes = ByteBuffer
                .allocate(14)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putShort(channels)
                .putInt(sampleRate)
                .putInt(sampleRate * channels * (bitDepth / 8))
                .putShort((short) (channels * (bitDepth / 8)))
                .putShort(bitDepth)
                .array();

        // Not necessarily the best, but it's very easy to visualize this way
        out.write(new byte[]{
                // RIFF header
                'R', 'I', 'F', 'F', // ChunkID
                0, 0, 0, 0, // ChunkSize (must be updated later)
                'W', 'A', 'V', 'E', // Format
                // fmt subchunk
                'f', 'm', 't', ' ', // Subchunk1ID
                16, 0, 0, 0, // Subchunk1Size
                1, 0, // AudioFormat
                littleBytes[0], littleBytes[1], // NumChannels
                littleBytes[2], littleBytes[3], littleBytes[4], littleBytes[5], // SampleRate
                littleBytes[6], littleBytes[7], littleBytes[8], littleBytes[9], // ByteRate
                littleBytes[10], littleBytes[11], // BlockAlign
                littleBytes[12], littleBytes[13], // BitsPerSample
                // data subchunk
                'd', 'a', 't', 'a', // Subchunk2ID
                0, 0, 0, 0, // Subchunk2Size (must be updated later)
        });
    }

    private static void updateWavHeader(File wav) throws IOException {
        byte[] sizes = ByteBuffer
                .allocate(8)
                .order(ByteOrder.LITTLE_ENDIAN)
                // There are probably a bunch of different/better ways to calculate
                // these two given your circumstances. Cast should be safe since if the WAV is
                // > 4 GB we've already made a terrible mistake.
                .putInt((int) (wav.length() - 8)) // ChunkSize
                .putInt((int) (wav.length() - 44)) // Subchunk2Size
                .array();

        RandomAccessFile accessWave = null;
        try {
            accessWave = new RandomAccessFile(wav, "rw");
            // ChunkSize
            accessWave.seek(4);
            accessWave.write(sizes, 0, 4);

            // Subchunk2Size
            accessWave.seek(40);
            accessWave.write(sizes, 4, 4);
        } catch (IOException e) {
            Log.wtf(TAG, e);
        } finally {
            if (accessWave != null) {
                try {
                    accessWave.close();
                } catch (IOException e) {
                    Log.wtf(TAG, e);
                }
            }
        }
    }

    @Nullable
    public static File recordWav(File target, CancellationSignal cancelRecord, CancellationSignal endRecord) {
        final int sampleRate = 16000;
        final int source = MediaRecorder.AudioSource.MIC;
        final int bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT) * 2;

        final AudioRecord recorder = new AudioRecord(
                source,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);
        try {
            recorder.startRecording();

            target.getParentFile().mkdirs();
            target.createNewFile();

            byte[] buffer = new byte[bufferSize];

            try (OutputStream file = new FileOutputStream(target)) {
                writeWavHeader(file, (short) 1, sampleRate, (short) 16);

                while (!endRecord.isCanceled()) {
                    if (cancelRecord.isCanceled()) {
                        target.delete();
                        return null;
                    }

                    final int read = recorder.read(buffer, 0, buffer.length);

                    file.write(buffer, 0, read);
                }
            }

            updateWavHeader(target);
        } catch (Exception e) {
            Log.wtf(TAG, e);
            return null;
        } finally {
            recorder.stop();
            recorder.release();
        }

        return target;
    }


}
