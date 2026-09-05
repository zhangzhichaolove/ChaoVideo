package com.app.chao.chaoapp.ui.activity;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.IntentCompat;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Tracks;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.PlaceholderDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.offline.Download;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;
import androidx.media3.ui.AspectRatioFrameLayout;

import com.app.chao.chaoapp.R;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.data.VideoLibraryRepository;
import com.app.chao.chaoapp.data.VideoSource;
import com.app.chao.chaoapp.download.DownloadMediaType;
import com.app.chao.chaoapp.download.VideoDownloads;
import com.app.chao.chaoapp.utils.StatusBarUtils;
import com.app.chao.chaoapp.playback.VideoPlaybackSettings;
import com.app.chao.chaoapp.playback.VideoSettingsDialogs;
import com.app.chao.chaoapp.playback.VideoTrackChoices;

import java.util.List;

/** Offline cache and SAF playback deliberately have no network fallback. */
@androidx.annotation.OptIn(markerClass = UnstableApi.class)
public class OfflineVideoActivity extends AppCompatActivity {
    public static final String EXTRA_DOWNLOAD_ID = "download_id";
    public static final String EXTRA_LEGACY_RECORD = "legacy_record";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable checkpoint = new Runnable() {
        @Override public void run() {
            saveProgress();
            handler.postDelayed(this, 5000);
        }
    };
    private ExoPlayer player;
    private PlayerView playerView;
    private Toolbar toolbar;
    private TextView errorView;
    private VideoLibraryRepository library;
    private VideoRes record;
    private MediaItem media;
    private boolean cached;
    private boolean started;
    private boolean ready;
    private boolean completed;
    private boolean playWhenReady = true;
    private long restoredPosition = -1;
    private int generation;
    private VideoPlaybackSettings playbackSettings;
    private AlertDialog settingsDialog;
    private TrackSelectionParameters trackParameters;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        playbackSettings = new VideoPlaybackSettings(this);
        setContentView(R.layout.activity_offline_video);
        toolbar = findViewById(R.id.offline_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(view -> finish());
        StatusBarUtils.applyPageInsets(findViewById(android.R.id.content), toolbar);
        playerView = findViewById(R.id.offline_player);
        playerView.setShowSubtitleButton(false);
        playerView.setShowNextButton(false);
        playerView.setShowPreviousButton(false);
        errorView = findViewById(R.id.offline_error);
        library = VideoLibraryRepository.get(this);
        if (state != null) {
            restoredPosition = state.getLong("position", -1);
            playWhenReady = state.getBoolean("play", true);
            Bundle tracks = state.getBundle("tracks");
            if (tracks != null) trackParameters = TrackSelectionParameters.fromBundle(tracks);
        }
    }

    @Override protected void onStart() {
        super.onStart();
        started = true;
        int request = ++generation;
        if (media != null) {
            loadProgress(request);
            return;
        }
        String id = getIntent().getStringExtra(EXTRA_DOWNLOAD_ID);
        if (id != null) {
            cached = true;
            VideoDownloads.get(this).load(new VideoDownloads.DownloadsCallback() {
                @Override public void onLoaded(List<Download> downloads) {
                    if (!started || request != generation) return;
                    for (Download download : downloads) {
                        if (id.equals(download.request.id) && download.state == Download.STATE_COMPLETED) {
                            media = download.request.toMediaItem();
                            setRecord("download:" + id, download.request.uri, VideoDownloads.title(download));
                            loadProgress(request);
                            return;
                        }
                    }
                    showError();
                }
                @Override public void onError() { if (started && request == generation) showError(); }
            });
        } else {
            Uri uri = getIntent().getData();
            if (uri == null || !"content".equals(uri.getScheme())) {
                showError();
                return;
            }
            media = MediaItem.fromUri(uri);
            setRecord("local:" + DownloadMediaType.id(uri.toString()), uri, fileName(uri));
            loadProgress(request);
        }
    }

    private void setRecord(String id, Uri uri, String title) {
        VideoRes legacy = IntentCompat.getParcelableExtra(getIntent(), EXTRA_LEGACY_RECORD, VideoRes.class);
        if (legacy != null && VideoSource.LEGACY.equals(legacy.getSourceId())
                && legacy.getStoredLibraryKey() != null && id.equals(legacy.getId())
                && uri.toString().equals(legacy.getVideo())) {
            record = legacy; // A selected old history row retains its exact progress/favorite key.
        } else {
            record = new VideoRes();
            record.bindOfflineSource(cached);
        }
        record.setId(id);
        record.setVideo(uri.toString());
        record.setTitle(title);
        getSupportActionBar().setTitle(title);
    }

    private String fileName(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) return cursor.getString(0);
        } catch (RuntimeException ignored) { }
        return getString(R.string.local_video);
    }

    private void loadProgress(int request) {
        library.loadProgress(record, 0, progress -> {
            if (!started || request != generation) return;
            DefaultMediaSourceFactory sources = new DefaultMediaSourceFactory(cached
                    ? VideoDownloads.get(this).offlineDataSource()
                    : new DefaultDataSource.Factory(this, PlaceholderDataSource.FACTORY));
            player = new ExoPlayer.Builder(this).setMediaSourceFactory(sources).build();
            player.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true);
            player.setHandleAudioBecomingNoisy(true);
            if (trackParameters != null) player.setTrackSelectionParameters(trackParameters);
            player.setPlaybackSpeed(playbackSettings.speed());
            applyAspect();
            playerView.setPlayer(player);
            playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING);
            player.addListener(new Player.Listener() {
                @Override public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_READY) {
                        ready = true;
                        errorView.setVisibility(View.GONE);
                    } else if (state == Player.STATE_ENDED) {
                        completed = true;
                        saveProgress();
                    }
                }
                @Override public void onIsPlayingChanged(boolean playing) {
                    if (playing) {
                        completed = false;
                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    } else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
                @Override public void onPlayerError(PlaybackException error) { showError(); }
                @Override public void onPlaybackParametersChanged(PlaybackParameters parameters) {
                    // Media3's built-in settings menu also changes speed.
                    playbackSettings.setSpeed(parameters.speed);
                }
                @Override public void onTracksChanged(Tracks tracks) {
                    playerView.setShowSubtitleButton(!VideoTrackChoices.supported(
                            tracks, C.TRACK_TYPE_TEXT).isEmpty());
                }
            });
            player.setMediaItem(media);
            player.seekTo(restoredPosition >= 0 ? restoredPosition : progress.positionMs);
            player.setPlayWhenReady(playWhenReady);
            player.prepare();
            handler.postDelayed(checkpoint, 5000);
        });
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, R.id.action_speed, Menu.NONE, R.string.playback_speed);
        menu.add(Menu.NONE, R.id.action_video_aspect, Menu.NONE, R.string.video_aspect);
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_speed) {
            dismissSettingsDialog();
            settingsDialog = VideoSettingsDialogs.speed(this, playbackSettings, () -> {
                if (player != null) player.setPlaybackSpeed(playbackSettings.speed());
            });
            return true;
        }
        if (item.getItemId() == R.id.action_video_aspect) {
            dismissSettingsDialog();
            settingsDialog = VideoSettingsDialogs.aspect(this, playbackSettings, this::applyAspect);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void applyAspect() {
        int aspect = playbackSettings.aspect();
        playerView.setResizeMode(aspect == VideoPlaybackSettings.CROP ? AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                : aspect == VideoPlaybackSettings.STRETCH ? AspectRatioFrameLayout.RESIZE_MODE_FILL
                : AspectRatioFrameLayout.RESIZE_MODE_FIT);
    }

    private void dismissSettingsDialog() {
        if (settingsDialog != null) settingsDialog.dismiss();
        settingsDialog = null;
    }

    private void showError() {
        errorView.setText(cached ? R.string.offline_content_missing : R.string.local_content_unreadable);
        errorView.setVisibility(View.VISIBLE);
    }

    private void saveProgress() {
        if (player != null && record != null && ready) {
            library.updateProgress(record, 0, completed ? 0 : player.getCurrentPosition(),
                    Math.max(0, player.getDuration()));
        }
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        if (player != null) {
            snapshotPlayer();
        }
        // Modern Android can call this after onStop has already released the player.
        state.putLong("position", restoredPosition);
        state.putBoolean("play", playWhenReady);
        if (trackParameters != null) state.putBundle("tracks", trackParameters.toBundle());
        super.onSaveInstanceState(state);
    }

    private void snapshotPlayer() {
        restoredPosition = completed ? 0 : player.getCurrentPosition();
        playWhenReady = player.getPlayWhenReady();
        trackParameters = player.getTrackSelectionParameters();
    }

    @Override protected void onStop() {
        dismissSettingsDialog();
        started = false;
        generation++;
        handler.removeCallbacks(checkpoint);
        saveProgress();
        if (player != null) {
            snapshotPlayer();
            playerView.setPlayer(null);
            player.release();
            player = null;
        }
        playerView.setShowSubtitleButton(false);
        ready = false;
        super.onStop();
    }
}
