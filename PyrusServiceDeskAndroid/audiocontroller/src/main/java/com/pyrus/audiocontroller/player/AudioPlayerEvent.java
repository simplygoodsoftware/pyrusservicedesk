package com.pyrus.audiocontroller.player;

/**
 * Events which {@link AudioPlayerController} sends to
 * {@link AudioPlayerListener}s.
 */
public class AudioPlayerEvent {

    public final int duration;
    public final int progress;
    public final AudioPlayerController.AudioPlayerState state;

    public AudioPlayerEvent() {
        this(0, 0, AudioPlayerController.AudioPlayerState.NOT_READY);
    }

    private AudioPlayerEvent(double duration, double progress, AudioPlayerController.AudioPlayerState state) {
        this.duration = (int) duration;
        this.progress = (int) progress;
        this.state = state;
    }

    public AudioPlayerEvent durationCalculated(double duration) {
        return new AudioPlayerEvent(
                duration,
                progress,
                state == AudioPlayerController.AudioPlayerState.NOT_READY
                        ? AudioPlayerController.AudioPlayerState.DURATION_CALCULATED
                        : state);
    }

    public AudioPlayerEvent stopped() {
        return new AudioPlayerEvent(duration, 0, AudioPlayerController.AudioPlayerState.STOPPED);
    }

    public AudioPlayerEvent playing(double progress) {
        return new AudioPlayerEvent(duration, progress, AudioPlayerController.AudioPlayerState.PLAYING);
    }

    public AudioPlayerEvent paused() {
        return new AudioPlayerEvent(duration, progress, AudioPlayerController.AudioPlayerState.PAUSED);
    }

    public AudioPlayerEvent failed() {
        return new AudioPlayerEvent(duration, 0, AudioPlayerController.AudioPlayerState.ERROR);
    }

}

