package core.threebanders.recordr.player;

public interface PlaybackListenerInterface {

    void onDurationChanged(int duration);

    void onPositionChanged(int position);

    void onPlaybackCompleted();

    void onError();

    void onReset();
}
