package com.app.chao.chaoapp.playback;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.CaptioningManager;
import androidx.media3.common.text.Cue;
import androidx.media3.common.text.CueGroup;
import androidx.media3.ui.SubtitleView;
import com.app.chao.chaoapp.R;
import com.shuyu.gsyvideoplayer.video.NormalGSYVideoPlayer;
import tv.danmaku.ijk.media.player.IMediaPlayer;

/** GSY view with decoder-backed captions, compact PiP UI and buffering-safe pause intent. */
@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
public class PlaybackVideoPlayer extends NormalGSYVideoPlayer {
    public PlaybackVideoPlayer(Context context) { super(context); initSubtitles(); }
    public PlaybackVideoPlayer(Context context, AttributeSet attrs) { super(context, attrs); initSubtitles(); }
    public PlaybackVideoPlayer(Context context, Boolean fullscreen) { super(context, fullscreen); initSubtitles(); }

    private boolean pictureInPictureUi;
    private SubtitleView subtitleView;
    private CueGroup subtitleCues = CueGroup.EMPTY_TIME_ZERO;

    private void initSubtitles() {
        subtitleView = new SubtitleView(getContext());
        subtitleView.setId(R.id.online_subtitles);
        subtitleView.setUserDefaultStyle();
        updateSubtitleLayout();
        View surface = findViewById(com.shuyu.gsyvideoplayer.R.id.surface_container);
        ViewGroup parent = (ViewGroup) surface.getParent();
        // Above decoded video, below thumbnail/error/playback controls; texture recreation must not remove it.
        parent.addView(subtitleView, parent.indexOfChild(surface) + 1, new ViewGroup.LayoutParams(-1, -1));
    }

    public void setSubtitleCues(CueGroup cues) {
        subtitleCues = cues;
        subtitleView.setCues(cues.cues);
        StringBuilder text = new StringBuilder();
        for (Cue cue : cues.cues) if (cue.text != null) {
            if (text.length() > 0) text.append('\n');
            text.append(cue.text);
        }
        subtitleView.setContentDescription(text.length() == 0 ? null : text.toString());
    }

    public CueGroup getSubtitleCues() { return subtitleCues; }

    public void setPictureInPictureUi(boolean compact) {
        pictureInPictureUi = compact;
        updateSubtitleLayout();
        if (compact) hideAllWidget();
        else resolveUIState(mCurrentState);
    }

    @Override protected void setViewShowState(View view, int visibility) {
        // GSY state callbacks may try to show transport controls again after a PiP remote action.
        boolean transport = view == mTopContainer || view == mBottomContainer || view == mStartButton
                || view == mBottomProgressBar || view == mLockScreen;
        super.setViewShowState(view, pictureInPictureUi && transport ? INVISIBLE : visibility);
    }

    private void updateSubtitleLayout() {
        if (subtitleView == null) return;
        CaptioningManager captions = (CaptioningManager)
                getContext().getSystemService(Context.CAPTIONING_SERVICE);
        float scale = captions == null ? 1f : captions.getFontScale();
        if (pictureInPictureUi) subtitleView.setFractionalTextSize(0.0533f * scale);
        else subtitleView.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, 16f * scale);
        int controls = (int) (48 * getResources().getDisplayMetrics().density);
        // Keep the inner canvas full-size; late FrameLayout padding clips its glyphs.
        subtitleView.setBottomPaddingFraction(pictureInPictureUi || getHeight() == 0 ? 0.08f
                : Math.min(0.3f, 0.08f + (float) controls / getHeight()));
    }

    @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        updateSubtitleLayout();
    }

    @Override public void startPlayLogic() {
        setSubtitleCues(CueGroup.EMPTY_TIME_ZERO);
        mBackUpPlayingBufferState = -1;
        super.startPlayLogic();
    }

    @Override public void onVideoPause() {
        int state = mCurrentState;
        super.onVideoPause();
        if (state == CURRENT_STATE_PLAYING || state == CURRENT_STATE_PLAYING_BUFFERING_START
                || state == CURRENT_STATE_PAUSE) {
            mCurrentPosition = getGSYVideoManager().getCurrentPosition();
            getGSYVideoManager().pause();
            mBackUpPlayingBufferState = CURRENT_STATE_PAUSE;
            setStateAndUi(CURRENT_STATE_PAUSE);
        }
    }

    @Override public void onVideoResume(boolean seek) {
        mBackUpPlayingBufferState = -1;
        super.onVideoResume(seek);
    }

    @Override public void onInfo(int what, int extra) {
        boolean paused = mCurrentState == CURRENT_STATE_PAUSE
                || mBackUpPlayingBufferState == CURRENT_STATE_PAUSE;
        super.onInfo(what, extra);
        if (paused && (what == IMediaPlayer.MEDIA_INFO_BUFFERING_START
                || what == IMediaPlayer.MEDIA_INFO_BUFFERING_END)) {
            // A late buffering notification must not present PLAYING or enable automatic PiP.
            mBackUpPlayingBufferState = what == IMediaPlayer.MEDIA_INFO_BUFFERING_START ? CURRENT_STATE_PAUSE : -1;
            setStateAndUi(CURRENT_STATE_PAUSE);
        }
    }
}
