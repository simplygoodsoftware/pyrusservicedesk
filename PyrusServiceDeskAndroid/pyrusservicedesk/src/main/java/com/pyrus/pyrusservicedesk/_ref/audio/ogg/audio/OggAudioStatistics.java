/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pyrus.pyrusservicedesk._ref.audio.ogg.audio;

import com.pyrus.pyrusservicedesk._ref.audio.ogg.OggStreamAudioData;
import com.pyrus.pyrusservicedesk._ref.audio.ogg.OggStreamPacket;
import com.pyrus.pyrusservicedesk._ref.audio.opus.OpusAudioData;
import com.pyrus.pyrusservicedesk._ref.audio.opus.OpusInfo;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;


/**
 * For computing statistics around an {@link OggAudioStream},
 *  such as how long it lasts.
 * Format specific subclasses may be able to also identify 
 *  additional statistics beyond these.
 */
public class OggAudioStatistics {
    private final OggAudioStream audio;
    private final OggAudioHeaders headers;

    private int audioPackets = 0;
    private long lastGranule = -1;
    private double durationSeconds = 0;

    private long oggOverheadSize = 0;
    private long headerOverheadSize = 0;
    private long audioDataSize = 0;

    public OggAudioStatistics(OggAudioHeaders headers, OggAudioStream audio) throws IOException {
        this.audio = audio;
        this.headers = headers;
    }

    /**
     * Calculate the statistics
     */
    public void calculate() throws IOException {
        OggStreamAudioData data;

        // Calculate the headers sizing
        OggAudioInfoHeader info = headers.getInfo();
        handleHeader(info);
        handleHeader(headers.getTags());
        handleHeader(headers.getSetup());

        // Have each audio packet handled, tracking at least granules
        while ((data = audio.getNextAudioPacket()) != null) {
            handleAudioData(data);
        }

        // Calculate the duration from the granules, if found
        if (lastGranule > 0) {
            long samples = lastGranule - info.getPreSkip();
            double sampleRate = info.getSampleRate();
            if (info instanceof OpusInfo) {
                // Opus is a special case - granule *always* runs at 48kHz
                sampleRate = OpusAudioData.OPUS_GRANULE_RATE;
            }
            durationSeconds = samples / sampleRate;
        }
    }

    protected void handleHeader(OggStreamPacket header) {
        if (header != null) {
            oggOverheadSize += header.getOggOverheadSize();
            headerOverheadSize += header.getData().length;
        }
    }

    protected void handleAudioData(OggStreamAudioData audioData) {
        audioPackets++;
        audioDataSize += audioData.getData().length;
        oggOverheadSize += audioData.getOggOverheadSize();

        if (audioData.getGranulePosition() > lastGranule) {
            lastGranule = audioData.getGranulePosition();
        }
    }

    /**
     * Returns the duration, in Hours:Minutes:Seconds.MS
     */
    public String getDuration() {
        return getDuration(Locale.ROOT);
    }
    /**
     * Returns the duration, in Hours:Minutes:Seconds.MS
     *  or Hours:Minutes:Seconds,MS (depending on Locale)
     */
    public String getDuration(Locale locale) {
        long durationL = (long)durationSeconds;

        // Output as Hours / Minutes / Seconds / Parts
        long hours = TimeUnit.SECONDS.toHours(durationL);
        long mins = TimeUnit.SECONDS.toMinutes(durationL) - (hours*60);
        double secs = durationSeconds - (((hours*60)+mins)*60);

        return String.format(locale, "%02d:%02d:%05.2f", hours, mins, secs);
    }

    /**
     * The number of audio packets in the stream
     */
    public int getAudioPacketsCount() {
        return audioPackets;
    }
}
