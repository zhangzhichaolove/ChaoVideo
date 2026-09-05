package com.app.chao.chaoapp.playback;

import static org.junit.Assert.*;
import com.shuyu.gsyvideoplayer.video.base.GSYVideoViewBridge;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.lang.reflect.Proxy;

@RunWith(org.robolectric.RobolectricTestRunner.class)
@Config(sdk = 28)
public class PlaybackVideoPlayerTest {
    private TestPlayer player;

    @Before public void setup() { player = new TestPlayer(); }

    @Test public void pauseWhileKernelBuffersStillSendsPauseAndSurvivesBufferEnd() {
        player.state(PlaybackVideoPlayer.CURRENT_STATE_PLAYING);
        player.onInfo(701, 0);
        assertEquals(PlaybackVideoPlayer.CURRENT_STATE_PLAYING_BUFFERING_START, player.getCurrentState());
        player.onVideoPause();
        assertTrue(player.pauses > 0);
        assertEquals(PlaybackVideoPlayer.CURRENT_STATE_PAUSE, player.getCurrentState());
        player.onInfo(702, 0);
        assertEquals(PlaybackVideoPlayer.CURRENT_STATE_PAUSE, player.getCurrentState());
    }

    @Test public void delayedBufferingEventsCannotUndoPauseOrMisrepresentPipState() {
        player.state(PlaybackVideoPlayer.CURRENT_STATE_PLAYING);
        player.onVideoPause();
        player.onInfo(701, 0);
        assertEquals(PlaybackVideoPlayer.CURRENT_STATE_PAUSE, player.getCurrentState());
        player.onInfo(702, 0);
        assertEquals(PlaybackVideoPlayer.CURRENT_STATE_PAUSE, player.getCurrentState());
    }

    @Test public void explicitResumeClearsPausedBufferState() {
        player.state(PlaybackVideoPlayer.CURRENT_STATE_PLAYING);
        player.onInfo(701, 0);
        player.onVideoPause();
        player.onVideoResume(false);
        player.onInfo(702, 0);
        assertEquals(1, player.starts);
        assertEquals(PlaybackVideoPlayer.CURRENT_STATE_PLAYING, player.getCurrentState());
    }

    @Test public void pauseBeforePreparedKeepsGsyDeferredPauseContract() {
        player.state(PlaybackVideoPlayer.CURRENT_STATE_PREPAREING);
        player.onVideoPause();
        assertTrue(player.deferredPause());
        assertEquals(0, player.pauses);
    }

    @Test public void newSelectionDoesNotInheritThePreviousPausedBufferState() {
        player.state(PlaybackVideoPlayer.CURRENT_STATE_PLAYING);
        player.onVideoPause();
        player.startPlayLogic();
        player.onInfo(701, 0);
        assertEquals(PlaybackVideoPlayer.CURRENT_STATE_PREPAREING, player.getCurrentState());
    }

    @Test public void pipHidesTransportControlsAcrossStateUpdatesAndRestoresThemOnExit() {
        player.state(PlaybackVideoPlayer.CURRENT_STATE_PLAYING);
        player.setPictureInPictureUi(true);
        player.showControls();
        assertEquals(android.view.View.INVISIBLE, player.getStartButton().getVisibility());
        assertEquals(android.view.View.INVISIBLE, player.findViewById(com.shuyu.gsyvideoplayer.R.id.layout_top).getVisibility());
        assertEquals(android.view.View.INVISIBLE, player.findViewById(com.shuyu.gsyvideoplayer.R.id.layout_bottom).getVisibility());
        player.setPictureInPictureUi(false);
        player.showControls();
        assertEquals(android.view.View.VISIBLE, player.getStartButton().getVisibility());
        assertEquals(android.view.View.VISIBLE, player.findViewById(com.shuyu.gsyvideoplayer.R.id.layout_bottom).getVisibility());
    }

    private static class TestPlayer extends PlaybackVideoPlayer {
        int pauses;
        int starts;
        // False is intentional: Exo's isPlaying() is false during buffering even with play intent.
        final GSYVideoViewBridge bridge = (GSYVideoViewBridge) Proxy.newProxyInstance(
                GSYVideoViewBridge.class.getClassLoader(), new Class[]{GSYVideoViewBridge.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("pause")) pauses++;
                    if (method.getName().equals("start")) starts++;
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == int.class) return 0;
                    if (method.getReturnType() == long.class) return 4000L;
                    return null;
                });
        TestPlayer() {
            super(RuntimeEnvironment.getApplication());
            mHadPlay = true;
            mAudioFocusManager = null;
        }
        void showControls() { changeUiToPauseShow(); }
        void state(int state) { mCurrentState = state; }
        boolean deferredPause() { return mPauseBeforePrepared; }
        @Override public GSYVideoViewBridge getGSYVideoManager() { return bridge; }
        @Override protected void setStateAndUi(int state) { mCurrentState = state; }
        @Override protected void prepareVideo() { mCurrentState = CURRENT_STATE_PREPAREING; }
        @Override protected void startDismissControlViewTimer() { }
    }
}
