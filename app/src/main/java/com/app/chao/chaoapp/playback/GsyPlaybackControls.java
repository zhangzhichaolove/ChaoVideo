package com.app.chao.chaoapp.playback;

import android.content.Context;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.media3.common.C;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.ui.DefaultTrackNameProvider;
import com.app.chao.chaoapp.R;
import com.shuyu.gsyvideoplayer.render.GSYRenderView;
import com.shuyu.gsyvideoplayer.utils.GSYVideoType;
import com.shuyu.gsyvideoplayer.video.NormalGSYVideoPlayer;
import com.shuyu.gsyvideoplayer.video.base.GSYVideoView;
import tv.danmaku.ijk.media.exo2.IjkExo2MediaPlayer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Local-only GSY settings UI; the Activity still owns playback and cast sessions. */
@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
public final class GsyPlaybackControls {
    public interface LocalPlayback { boolean get(); }
    public interface Engine { IjkExo2MediaPlayer get(); }
    public interface SubtitleSource { GsyCueOutput get(); }
    private final Context context;
    private final NormalGSYVideoPlayer videoPlayer;
    private final VideoPlaybackSettings playbackSettings;
    private final LocalPlayback localPlayback;
    private final Engine engine;
    private AlertDialog settingsDialog;
    private final SubtitleSource subtitleSource;
    private final GsySubtitleDisplay subtitles;

    public GsyPlaybackControls(Context context, NormalGSYVideoPlayer videoPlayer,
                               VideoPlaybackSettings settings, LocalPlayback localPlayback,
                               Engine engine, SubtitleSource subtitleSource) {
        this.context = context;
        this.videoPlayer = videoPlayer;
        this.playbackSettings = settings;
        this.localPlayback = localPlayback;
        this.engine = engine;
        this.subtitleSource = subtitleSource;
        this.subtitles = new GsySubtitleDisplay(videoPlayer);
    }

    public void bindSubtitles() { subtitles.bind(localPlayback.get() ? subtitleSource.get() : null); }
    public void clearSubtitles() { subtitles.bind(null); }
    public void refreshSubtitles() { subtitles.refresh(); }

    public void dismiss() {
        if (settingsDialog != null) settingsDialog.dismiss();
        settingsDialog = null;
    }

    public void showSpeedPicker() {
        dismiss();
        if (!localPlayback.get()) return;
        settingsDialog = VideoSettingsDialogs.speed(context, playbackSettings, () -> {
            // Update the stored view speed as well as the live kernel, so full-screen cloning
            // and the next episode cannot silently revert the user's choice.
            videoPlayer.setSpeed(playbackSettings.speed(), true);
            videoPlayer.getCurrentPlayer().setSpeedPlaying(playbackSettings.speed(), true);
        });
    }

    public void showAspectPicker() {
        dismiss();
        if (!localPlayback.get()) return;
        settingsDialog = VideoSettingsDialogs.aspect(context, playbackSettings, this::applyAspect);
    }

    public void applyAspect() {
        int aspect = playbackSettings.aspect();
        GSYVideoType.setShowType(aspect == VideoPlaybackSettings.CROP ? GSYVideoType.SCREEN_TYPE_FULL
                : aspect == VideoPlaybackSettings.STRETCH ? GSYVideoType.SCREEN_MATCH_FULL
                : GSYVideoType.SCREEN_TYPE_DEFAULT);
        resizePlayer(videoPlayer);
        if (videoPlayer.getCurrentPlayer() != videoPlayer) resizePlayer(videoPlayer.getCurrentPlayer());
    }

    private void resizePlayer(GSYVideoView player) {
        GSYRenderView render = player.getRenderProxy();
        if (render == null || render.getShowView() == null) return;
        ViewGroup.LayoutParams params = render.getLayoutParams();
        // MATCH_FULL uses MATCH_PARENT; fit/crop need WRAP_CONTENT for GSY's aspect measurement.
        params.width = GSYRenderView.getTextureParams();
        params.height = GSYRenderView.getTextureParams();
        render.setLayoutParams(params);
        render.requestLayout();
    }

    public List<VideoTrackChoices> audioChoices() {
        IjkExo2MediaPlayer player = engine.get();
        if (player == null || !(player.getTrackSelector() instanceof DefaultTrackSelector)) {
            return Collections.emptyList();
        }
        return VideoTrackChoices.supported(player.getCurrentTracks(), C.TRACK_TYPE_AUDIO);
    }

    public void showAudioPicker() {
        dismiss();
        IjkExo2MediaPlayer player = engine.get();
        List<VideoTrackChoices> choices = audioChoices();
        if (player == null || choices.size() < 2) return;
        DefaultTrackNameProvider names = new DefaultTrackNameProvider(context.getResources());
        String[] labels = new String[choices.size()];
        int selected = -1;
        for (int i = 0; i < choices.size(); i++) {
            VideoTrackChoices choice = choices.get(i);
            labels[i] = (i + 1) + " · " + names.getTrackName(choice.group.getTrackFormat(choice.index));
            if (choice.group.isTrackSelected(choice.index)) selected = i;
        }
        settingsDialog = new AlertDialog.Builder(context).setTitle(R.string.audio_track)
                .setSingleChoiceItems(labels, selected, (dialog, which) -> {
                    if (player == engine.get()) {
                        VideoTrackChoices choice = choices.get(which);
                        if (player.getCurrentTracks().getGroups().contains(choice.group)) {
                            DefaultTrackSelector selector = (DefaultTrackSelector) player.getTrackSelector();
                            selector.setParameters(selector.buildUponParameters()
                                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                                    .setOverrideForType(new TrackSelectionOverride(
                                            choice.group.getMediaTrackGroup(), choice.index)));
                        }
                    }
                    dialog.dismiss();
                }).setNegativeButton(R.string.cancel, null).show();
    }

    public List<VideoTrackChoices> subtitleChoices() {
        IjkExo2MediaPlayer player = engine.get();
        return player == null ? Collections.emptyList() : VideoTrackChoices.supported(player.getCurrentTracks(), C.TRACK_TYPE_TEXT);
    }

    public void showSubtitlePicker() {
        dismiss();
        IjkExo2MediaPlayer player = engine.get();
        List<VideoTrackChoices> choices = subtitleChoices();
        if (player == null || choices.isEmpty() || !(player.getTrackSelector() instanceof DefaultTrackSelector)) return;
        DefaultTrackSelector selector = (DefaultTrackSelector) player.getTrackSelector();
        DefaultTrackNameProvider names = new DefaultTrackNameProvider(context.getResources());
        String[] labels = new String[choices.size() + 2];
        labels[0] = context.getString(R.string.subtitles_off);
        labels[1] = context.getString(R.string.subtitles_auto);
        int selected = selector.getParameters().disabledTrackTypes.contains(C.TRACK_TYPE_TEXT) ? 0 : 1;
        for (int i = 0; i < choices.size(); i++) {
            VideoTrackChoices choice = choices.get(i);
            labels[i + 2] = (i + 1) + " · " + names.getTrackName(choice.group.getTrackFormat(choice.index));
            if (selected != 0 && choice.group.isTrackSelected(choice.index)
                    && selector.getParameters().overrides.containsKey(choice.group.getMediaTrackGroup())) selected = i + 2;
        }
        settingsDialog = new AlertDialog.Builder(context).setTitle(R.string.subtitles)
                .setSingleChoiceItems(labels, selected, (dialog, which) -> {
                    if (player == engine.get()) {
                        DefaultTrackSelector.Parameters.Builder parameters = selector.buildUponParameters()
                                .clearOverridesOfType(C.TRACK_TYPE_TEXT).setTrackTypeDisabled(C.TRACK_TYPE_TEXT, which == 0);
                        if (which >= 2) {
                            VideoTrackChoices choice = choices.get(which - 2);
                            if (!player.getCurrentTracks().getGroups().contains(choice.group)) { dialog.dismiss(); return; }
                            parameters.setOverrideForType(new TrackSelectionOverride(choice.group.getMediaTrackGroup(), choice.index));
                        }
                        selector.setParameters(parameters);
                    }
                    dialog.dismiss();
                }).setNegativeButton(R.string.cancel, null).show();
    }

    private void showFullscreenSettings() {
        dismiss();
        if (!localPlayback.get()) return;
        List<String> labels = new ArrayList<>();
        labels.add(context.getString(R.string.playback_speed));
        labels.add(context.getString(R.string.video_aspect));
        boolean audio = audioChoices().size() > 1;
        if (audio) labels.add(context.getString(R.string.audio_track));
        if (!subtitleChoices().isEmpty()) labels.add(context.getString(R.string.subtitles));
        settingsDialog = new AlertDialog.Builder(context).setTitle(R.string.playback_settings)
                .setItems(labels.toArray(new String[0]), (dialog, which) -> {
                    if (which == 0) showSpeedPicker();
                    else if (which == 1) showAspectPicker();
                    else if (which == 2 && audio) showAudioPicker();
                    else showSubtitlePicker();
                }).setNegativeButton(R.string.cancel, null).show();
    }

    public void addFullscreenControls(NormalGSYVideoPlayer player, boolean hasEpisodes, Runnable pickEpisode) {
        if (player == videoPlayer || player.findViewWithTag("playback_settings") != null) return;
        LinearLayout top = player.findViewById(com.shuyu.gsyvideoplayer.R.id.layout_top);
        // Keep the controls in GSY's top bar: they disappear with its controls and lock state.
        player.getTitleTextView().setSingleLine(true);
        player.getTitleTextView().setEllipsize(TextUtils.TruncateAt.END);
        player.getTitleTextView().setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button settings = new androidx.appcompat.widget.AppCompatButton(context);
        settings.setTag("playback_settings");
        settings.setText(R.string.playback_settings);
        settings.setOnClickListener(view -> showFullscreenSettings());
        top.addView(settings);
        if (hasEpisodes) {
            Button episodes = new androidx.appcompat.widget.AppCompatButton(context);
            episodes.setTag("episode_picker");
            episodes.setText(R.string.episode_picker);
            episodes.setOnClickListener(view -> pickEpisode.run());
            top.addView(episodes);
        }
    }

}
