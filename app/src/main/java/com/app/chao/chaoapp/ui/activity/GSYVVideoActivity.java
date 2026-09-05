package com.app.chao.chaoapp.ui.activity;

import android.Manifest;
import android.content.res.Configuration;
import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Rational;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.ImageView;
import com.app.chao.chaoapp.playback.VideoPlaybackSettings;
import com.app.chao.chaoapp.playback.GsyPlaybackControls;
import com.shuyu.gsyvideoplayer.player.IPlayerManager;
import tv.danmaku.ijk.media.exo2.IjkExo2MediaPlayer;
import androidx.media3.exoplayer.offline.DownloadRequest;
import androidx.media3.exoplayer.offline.DownloadService;
import com.app.chao.chaoapp.download.VideoDownloadService;
import com.app.chao.chaoapp.download.VideoDownloads;
import android.content.Context;
import android.net.Uri;
import android.content.pm.PackageManager;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.IntentCompat;
import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.app.chao.chaoapp.R;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.cast.DlnaCastManager;
import com.app.chao.chaoapp.data.VideoLibraryRepository;
import com.app.chao.chaoapp.ui.fragment.EpisodeSelectionFragment;
import com.app.chao.chaoapp.ui.fragment.VideoIntroFragment;
import com.app.chao.chaoapp.utils.ImageLoader;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.shuyu.gsyvideoplayer.GSYVideoManager;
import com.shuyu.gsyvideoplayer.cache.CacheFactory;
import com.shuyu.gsyvideoplayer.listener.LockClickListener;
import com.shuyu.gsyvideoplayer.player.PlayerFactory;
import com.shuyu.gsyvideoplayer.utils.GSYVideoType;
import com.shuyu.gsyvideoplayer.utils.OrientationUtils;
import com.shuyu.gsyvideoplayer.video.NormalGSYVideoPlayer;
import com.shuyu.gsyvideoplayer.video.base.GSYVideoView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.app.chao.chaoapp.playback.SubtitlePlayerManager;
import com.app.chao.chaoapp.playback.PlaybackVideoPlayer;
import com.app.chao.chaoapp.playback.GsyCueOutput;
import tv.danmaku.ijk.media.exo2.ExoPlayerCacheManager;

@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
public class GSYVVideoActivity extends BaseActivity implements
        EpisodeSelectionFragment.OnEpisodeSelectedListener {
    public static final String EXTRA_EPISODE = "episode";
    private static final String ACTION_PIP_PLAY_PAUSE =
            "com.app.chao.chaoapp.action.PIP_PLAY_PAUSE";
    private static final int MAX_PLAY_RETRIES = 2;
    private static final long MIN_RESUME_POSITION_MS = 10_000L;

    //推荐使用StandardGSYVideoPlayer，功能一致
    //CustomGSYVideoPlayer部分功能处于试验阶段
    NormalGSYVideoPlayer videoPlayer;

    Toolbar toolbar;
    private final List<String> titles = new ArrayList<>();
    VideoRes videoInfo;
    TabLayout viewpagertab;
    ViewPager2 viewpager;
    VideoRes videoRes;
    List<Fragment> fragments;


    private boolean isPlay;
    private boolean isPause;
    private boolean isCasting;
    private boolean castRequestPending;
    private String currentVideoUrl;
    private String currentVideoTitle;
    private DlnaCastManager castManager;
    private DlnaCastManager.Device castDevice;
    private MenuItem castMenuItem;
    private EpisodeSelectionFragment episodeSelectionFragment;
    private long playbackGeneration;
    private int castStatusGeneration;
    private boolean playbackStarted;
    private boolean playbackCompleted;
    private boolean resumeLocalOnResume;
    private Runnable deferredPlayback;
    private AlertDialog resumeDialog;
    private final Handler playbackHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingRetry;
    private final Runnable progressSaver = new Runnable() {
        @Override public void run() {
            saveCurrentProgress();
            playbackHandler.postDelayed(this, 5000);
        }
    };
    private VideoLibraryRepository libraryRepository;
    private MenuItem favoriteMenuItem;
    private boolean favorite;
    private int currentEpisode;
    private int playRetryCount;
    private VideoPlaybackSettings playbackSettings;
    private GsyPlaybackControls playbackControls;
    private TabLayoutMediator tabMediator;
    private FrameLayout playerContainer;
    private View playbackError;
    private long castPositionMs;
    private long castDurationMs;
    private boolean castPlaying = true;
    private int castStatusFailures;
    private boolean pictureInPictureReceiverRegistered;
    private final BroadcastReceiver pictureInPictureReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_PIP_PLAY_PAUSE.equals(intent.getAction())) {
                togglePictureInPicturePlayback();
            }
        }
    };
    private VideoDownloads.Preparation downloadProbe;
    private DownloadRequest pendingDownload;
    private final ActivityResultLauncher<String> downloadNotificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> enqueuePendingDownload());
    private final Handler castStatusHandler = new Handler(Looper.getMainLooper());
    private final Runnable castStatusPoller = new Runnable() {
        @Override
        public void run() {
            if (!isCasting || castDevice == null) {
                return;
            }
            int generation = castStatusGeneration;
            String mediaUrl = currentVideoUrl;
            DlnaCastManager.Device device = castDevice;
            castManager.getPlaybackStatus(device, new DlnaCastManager.PlaybackStatusCallback() {
                @Override
                public void onStatus(DlnaCastManager.PlaybackStatus status) {
                    if (!isCasting || generation != castStatusGeneration
                            || !TextUtils.equals(mediaUrl, currentVideoUrl)) return;
                    if (!status.hasMedia()) {
                        castStatusFailures++;
                        if (castStatusFailures == 3) showToast(getString(R.string.cast_media_unverified));
                        castStatusHandler.postDelayed(castStatusPoller, 5000);
                        return;
                    }
                    if (!status.ownsMedia(mediaUrl)) {
                        detachCastSession();
                        showToast(getString(R.string.cast_media_changed));
                        return;
                    }
                    castStatusFailures = 0;
                    castPositionMs = status.getPositionMs();
                    castDurationMs = status.getDurationMs();
                    castPlaying = status.isPlaying();
                    castStatusHandler.postDelayed(castStatusPoller, 3000);
                }

                @Override
                public void onError(String error) {
                    if (!isCasting || generation != castStatusGeneration) return;
                    castStatusFailures++;
                    if (castStatusFailures == 3) {
                        showToast(getString(R.string.cast_device_unreachable));
                    }
                    castStatusHandler.postDelayed(castStatusPoller, 5000);
                }
            });
        }
    };

    private OrientationUtils orientationUtils;


    @Override
    protected int getLayout() {
        return R.layout.activity_video_info_views;
    }

    @Override
    protected void init() {
        playbackSettings = new VideoPlaybackSettings(this);
        videoPlayer = findViewById(R.id.detail_player);
        playbackControls = new GsyPlaybackControls(this, videoPlayer,
                playbackSettings, () -> !isCasting && !castRequestPending, this::currentExoPlayer, this::currentSubtitleOutput);
        ContextCompat.registerReceiver(this, pictureInPictureReceiver,
                new IntentFilter(ACTION_PIP_PLAY_PAUSE),
                ContextCompat.RECEIVER_NOT_EXPORTED);
        pictureInPictureReceiverRegistered = true;
        videoPlayer.post(this::updatePictureInPictureParams);
        playerContainer = findViewById(R.id.player_container);
        playbackError = findViewById(R.id.playback_error);
        findViewById(R.id.playback_retry).setOnClickListener(view -> {
            playbackError.setVisibility(View.GONE);
            playVideo(currentVideoUrl, currentVideoTitle);
        });
        toolbar = findViewById(R.id.toolbar);
        viewpagertab = findViewById(R.id.viewpagertab);
        viewpager = findViewById(R.id.viewpager);
        castManager = new DlnaCastManager(this);
        libraryRepository = VideoLibraryRepository.get(this);
        // 必须在 setUp/startPlayLogic 之前选择播放器内核；完整 IJK 包已不再引入。
        PlayerFactory.setPlayManager(SubtitlePlayerManager.class);
        CacheFactory.setCacheManager(ExoPlayerCacheManager.class);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (orientationUtils != null) {
                    orientationUtils.backToProtVideo();
                }
                if (!GSYVideoManager.backFromWindowFull(GSYVVideoActivity.this)) {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
        getIntentData();
        if (isFinishing()) return;
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        /*VideoOptionModel videoOptionModel =
                new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 50);
        List<VideoOptionModel> list = new ArrayList<>();
        list.add(videoOptionModel);
        GSYVideoManager.instance().setOptionModelList(list);*/


        //EXOPlayer内核，支持格式更多
        //系统内核模式
        //PlayerFactory.setPlayManager(SystemPlayerManager.class);
        //ijk内核，默认模式
        //PlayerFactory.setPlayManager(IjkPlayerManager.class);
        //exo缓存模式，支持m3u8，只支持exo
        //代理缓存模式，支持所有模式，不支持m3u8等，默认
        //CacheFactory.setCacheManager(ProxyCacheManager.class);
        playbackControls.applyAspect();

        //切换绘制模式
        //GSYVideoType.setRenderType(GSYVideoType.SUFRACE);
        //GSYVideoType.setRenderType(GSYVideoType.GLSURFACE);
        GSYVideoType.setRenderType(GSYVideoType.TEXTURE);

        //增加title
        resolveNormalVideoUI();

        //外部辅助的旋转，帮助全屏
        orientationUtils = new OrientationUtils(this, videoPlayer);
        //初始化不打开外部的旋转
        orientationUtils.setEnable(false);

        videoPlayer.setIsTouchWiget(true);
        //关闭自动旋转
        videoPlayer.setRotateViewAuto(false);
        videoPlayer.setLockLand(false);
        videoPlayer.setShowFullAnimation(false);
        videoPlayer.setNeedLockFull(true);
        videoPlayer.getFullscreenButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //直接横屏
                orientationUtils.resolveByClick();

                //第一个true是否需要隐藏actionbar，第二个true是否需要隐藏statusbar
                NormalGSYVideoPlayer fullscreenPlayer = (NormalGSYVideoPlayer)
                        videoPlayer.startWindowFullscreen(GSYVVideoActivity.this, true, true);
                addFullscreenControls(fullscreenPlayer);
            }
        });

        videoPlayer.setVideoAllCallBack(new SampleListener() {

            @Override
            public void onPrepared(String url, Object... objects) {
                if (!TextUtils.equals(url, currentVideoUrl)) return;
                if (isCasting || castRequestPending) {
                    // A restored DLNA session owns playback. Preparation is asynchronous, so
                    // pause again here to prevent the local player from starting behind the TV.
                    GSYVideoManager.onPause();
                    return;
                }
                //开始播放了才能旋转和全屏
                orientationUtils.setEnable(true);
                isPlay = true;
                playbackStarted = true;
                playbackCompleted = false;
                playRetryCount = 0;
                playbackError.setVisibility(View.GONE);
                playbackControls.applyAspect();
                playbackControls.bindSubtitles();
                invalidateOptionsMenu();
            }

            @Override
            public void onAutoComplete(String url, Object... objects) {
                if (!TextUtils.equals(url, currentVideoUrl)) return;
                playbackControls.clearSubtitles();
                playbackCompleted = true;
                long duration = videoPlayer.getCurrentPlayer().getDuration();
                libraryRepository.updateProgress(videoInfo, currentEpisode, 0, duration);
                if (videoInfo != null && currentEpisode > 0
                        && currentEpisode < videoInfo.getEpisodes()) {
                    playEpisode(currentEpisode + 1, false);
                }
            }

            @Override
            public void onEnterFullscreen(String url, Object... objects) {
                playbackControls.refreshSubtitles();
                addFullscreenControls((NormalGSYVideoPlayer) videoPlayer.getCurrentPlayer());
                playbackControls.applyAspect();
            }

            @Override
            public void onQuitFullscreen(String url, Object... objects) {
                playbackControls.refreshSubtitles();
                playbackControls.applyAspect();
                updatePictureInPictureParams();
                if (orientationUtils != null) {
                    orientationUtils.backToProtVideo();
                }
            }

            @Override
            public void onPlayError(String url, Object... objects) {
                if (!TextUtils.equals(url, currentVideoUrl)) return;
                playbackControls.clearSubtitles();
                if (!isCasting && !castRequestPending) {
                    if (playRetryCount < MAX_PLAY_RETRIES) {
                        long delayMs = 500L * (1L << playRetryCount);
                        playRetryCount++;
                        long generation = playbackGeneration;
                        cancelPlaybackRetry();
                        pendingRetry = () -> {
                            if (generation == playbackGeneration && !isPause
                                    && !isFinishing() && !isCasting && !castRequestPending) {
                                videoPlayer.getCurrentPlayer().startPlayLogic();
                            }
                        };
                        playbackHandler.postDelayed(pendingRetry, delayMs);
                    } else {
                        showToast(getString(R.string.video_play_failed));
                        playbackError.setVisibility(View.VISIBLE);
                    }
                }
            }

        });
        videoPlayer.setGSYStateUiListener(state -> updatePictureInPictureParams());

        videoPlayer.setLockClickListener(new LockClickListener() {
            @Override
            public void onClick(View view, boolean lock) {
                if (orientationUtils != null) {
                    //配合下方的onConfigurationChanged
                    orientationUtils.setEnable(!lock);
                }
            }
        });
    }

    private void getIntentData() {
        videoInfo = IntentCompat.getParcelableExtra(getIntent(), "videoInfo", VideoRes.class);
        if (videoInfo == null) {
            showToast(getString(R.string.video_missing));
            finish();
            return;
        }
        fragments = new ArrayList<>();
        VideoIntroFragment videoIntroFragment = VideoIntroFragment.newInstance(videoInfo);
        fragments.add(videoIntroFragment);
        titles.add("简介");
        if (videoInfo.getEpisodes() > 0) {
            int requestedEpisode = getIntent().getIntExtra(EXTRA_EPISODE, 0);
            currentEpisode = requestedEpisode > 0
                    ? Math.max(1, Math.min(videoInfo.getEpisodes(), requestedEpisode))
                    : 1;
            episodeSelectionFragment = EpisodeSelectionFragment.newInstance(videoInfo);
            fragments.add(episodeSelectionFragment);
            titles.add("选集");
        }
        // No comment endpoint is configured; do not expose an empty, nonfunctional tab.


        MyAdapter adapter = new MyAdapter(this);
        viewpager.setAdapter(adapter);
        tabMediator = new TabLayoutMediator(viewpagertab, viewpager,
                (tab, position) -> tab.setText(titles.get(position)));
        tabMediator.attach();
        viewpager.setCurrentItem(0, false);


        toolbar.setTitle(videoInfo.getTitle());
        if (!TextUtils.isEmpty(videoInfo.getImg())) {
            ImageView imageView = new ImageView(this);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            ImageLoader.load(this, videoInfo.getImg(), imageView);
            videoPlayer.setThumbImageView(imageView);
        }
        int requestedEpisode = getIntent().getIntExtra(EXTRA_EPISODE, 0);
        libraryRepository.loadLastEpisode(videoInfo, savedEpisode -> {
            if (isFinishing() || isDestroyed() || currentVideoUrl != null) return;
            currentEpisode = videoInfo.getEpisodes() > 0
                    ? Math.max(1, Math.min(videoInfo.getEpisodes(),
                    requestedEpisode > 0 ? requestedEpisode : savedEpisode)) : 0;
            if (currentEpisode > 0) {
                playEpisode(currentEpisode, false);
            } else {
                playVideo(videoInfo.getVideo(), videoInfo.getTitle());
            }
            libraryRepository.recordOpened(videoInfo, currentEpisode);
        });

    }

    @Override
    public void onEpisodeSelected(int episode) {
        playEpisode(episode, true);
    }

    private void playEpisode(int episode, boolean savePrevious) {
        if (savePrevious) saveCurrentProgress();
        currentEpisode = episode;
        if (episodeSelectionFragment != null) {
            episodeSelectionFragment.setSelectedEpisode(episode);
        }
        String title = videoInfo.getTitle() + " 第" + episode + "集";
        playVideo(videoInfo.getEpisodeVideo(episode), title);
        toolbar.setTitle(title);
    }

    private void playVideo(String url, String title) {
        if (TextUtils.isEmpty(url)) {
            playbackError.setVisibility(View.VISIBLE);
            return;
        }
        playbackControls.dismiss();
        playbackControls.clearSubtitles();
        cancelPlaybackRetry();
        if (resumeDialog != null) resumeDialog.dismiss();
        long generation = ++playbackGeneration;
        castStatusGeneration++;
        castStatusHandler.removeCallbacks(castStatusPoller);
        GSYVideoManager.onPause();
        currentVideoUrl = url;
        currentVideoTitle = title;
        playbackStarted = false;
        playbackCompleted = false;
        castRequestPending = false;
        playRetryCount = 0;
        libraryRepository.loadProgress(videoInfo, currentEpisode, progress -> {
            if (generation != playbackGeneration || isFinishing() || isDestroyed()) return;
            Runnable localPlayback = () -> {
                if (generation != playbackGeneration || isFinishing() || isDestroyed()) return;
                if (progress.positionMs >= MIN_RESUME_POSITION_MS) {
                    resumeDialog = new AlertDialog.Builder(this)
                            .setTitle(R.string.resume_playback_title)
                            .setMessage(getString(R.string.resume_playback_message,
                                    formatPlaybackPosition(progress.positionMs)))
                            .setNegativeButton(R.string.play_from_start, (dialog, which) -> {
                                if (generation != playbackGeneration) return;
                                libraryRepository.updateProgress(videoInfo, currentEpisode, 0, 0);
                                startVideo(url, title, 0);
                            })
                            .setPositiveButton(R.string.continue_playing, (dialog, which) -> {
                                if (generation == playbackGeneration) startVideo(url, title, progress.positionMs);
                            })
                            .show();
                } else {
                    startVideo(url, title, 0);
                }
            };
            runWhenResumed(() -> {
                if (generation != playbackGeneration) return;
                if (isCasting && castDevice != null) {
                    castCurrentVideo(castDevice, progress.positionMs);
                } else {
                    restoreRememberedCast(() -> runWhenResumed(localPlayback));
                }
            });
        });
    }

    private void runWhenResumed(Runnable action) {
        if (isFinishing() || isDestroyed()) return;
        if (isPause) deferredPlayback = action;
        else action.run();
    }

    private void cancelPlaybackRetry() {
        if (pendingRetry != null) playbackHandler.removeCallbacks(pendingRetry);
        pendingRetry = null;
    }

    private void saveCurrentProgress() {
        if (videoInfo == null || TextUtils.isEmpty(currentVideoUrl)) return;
        if (isCasting) {
            if (TextUtils.equals(currentVideoUrl, castManager.getRememberedMediaUrl())
                    && castStatusFailures == 0) {
                libraryRepository.updateProgress(videoInfo, currentEpisode, castPositionMs, castDurationMs);
            }
        } else if (playbackStarted) {
            libraryRepository.updateProgress(videoInfo, currentEpisode,
                    playbackCompleted ? 0 : videoPlayer.getCurrentPlayer().getCurrentPositionWhenPlaying(),
                    videoPlayer.getCurrentPlayer().getDuration());
        }
    }

    private void startVideo(String url, String title, long positionMs) {
        if (isCasting && castDevice != null) {
            castCurrentVideo(castDevice, positionMs);
            return;
        }
        playLocalVideo(url, title, positionMs);
    }

    private void playLocalVideo(String url, String title, long positionMs) {
        playbackControls.clearSubtitles();
        playbackCompleted = false;
        NormalGSYVideoPlayer target = (NormalGSYVideoPlayer) videoPlayer.getCurrentPlayer();
        target.setSeekOnStart(positionMs);
        target.setUp(url, true, null, title);
        target.setSpeed(playbackSettings.speed(), true);
        target.startPlayLogic();
        addFullscreenControls(target);
    }

    private String formatPlaybackPosition(long positionMs) {
        long totalSeconds = positionMs / 1000;
        return String.format(java.util.Locale.getDefault(), "%02d:%02d:%02d",
                totalSeconds / 3600, (totalSeconds % 3600) / 60, totalSeconds % 60);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.video_player, menu);
        castMenuItem = menu.findItem(R.id.action_cast);
        favoriteMenuItem = menu.findItem(R.id.action_favorite);
        libraryRepository.isFavorite(videoInfo, saved -> {
            favorite = saved;
            updateFavoriteMenu();
        });
        updateCastMenu();
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean local = !isCasting && !castRequestPending;
        menu.findItem(R.id.action_speed).setVisible(local);
        menu.findItem(R.id.action_video_aspect).setVisible(local);
        menu.findItem(R.id.action_audio_track).setVisible(local && playbackControls.audioChoices().size() > 1);
        menu.findItem(R.id.action_subtitles).setVisible(local && !playbackControls.subtitleChoices().isEmpty());
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_cast) {
            if (isCasting && castDevice != null) {
                showCastActions();
            } else if (!castRequestPending) {
                showCastDevicePicker();
            }
            return true;
        }
        if (item.getItemId() == R.id.action_speed) {
            playbackControls.showSpeedPicker();
            return true;
        }
        if (item.getItemId() == R.id.action_video_aspect) {
            playbackControls.showAspectPicker();
            return true;
        }
        if (item.getItemId() == R.id.action_audio_track) {
            playbackControls.showAudioPicker();
            return true;
        }
        if (item.getItemId() == R.id.action_subtitles) {
            playbackControls.showSubtitlePicker();
            return true;
        }
        if (item.getItemId() == R.id.action_favorite) {
            libraryRepository.toggleFavorite(videoInfo, saved -> {
                favorite = saved;
                updateFavoriteMenu();
                showToast(getString(saved ? R.string.favorite_added : R.string.favorite_removed));
            });
            return true;
        }
        if (item.getItemId() == R.id.action_picture_in_picture) {
            enterPictureInPicture();
            return true;
        }
        if (item.getItemId() == R.id.action_download_video) {
            downloadCurrentVideo();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private IjkExo2MediaPlayer currentExoPlayer() {
        if (!playbackStarted || isCasting || castRequestPending) return null;
        IPlayerManager manager = GSYVideoManager.instance().getCurPlayerManager();
        if (manager == null || !(manager.getMediaPlayer() instanceof IjkExo2MediaPlayer)) return null;
        return (IjkExo2MediaPlayer) manager.getMediaPlayer();
    }

    private GsyCueOutput currentSubtitleOutput() {
        if (currentExoPlayer() == null) return null;
        IPlayerManager manager = GSYVideoManager.instance().getCurPlayerManager();
        return manager instanceof SubtitlePlayerManager ? ((SubtitlePlayerManager) manager).subtitleOutput() : null;
    }

    private boolean pictureInPictureTransition;

    private void enterPictureInPicture() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            showToast(getString(R.string.picture_in_picture_unavailable));
            return;
        }
        pictureInPictureTransition = true;
        if (!enterPictureInPictureMode(createPictureInPictureParams(false))) pictureInPictureTransition = false;
    }

    private void updatePictureInPictureParams() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && videoPlayer != null) {
            setPictureInPictureParams(createPictureInPictureParams(!isCasting));
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private PictureInPictureParams createPictureInPictureParams(boolean autoEnter) {
        PictureInPictureParams.Builder builder = new PictureInPictureParams.Builder()
                .setAspectRatio(new Rational(16, 9));
        if (!isCasting) {
            boolean playing = isPictureInPicturePlaybackActive();
            String label = getString(playing ? R.string.pip_pause : R.string.pip_play);
            Intent intent = new Intent(ACTION_PIP_PLAY_PAUSE).setPackage(getPackageName());
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            RemoteAction action = new RemoteAction(
                    Icon.createWithResource(this, playing
                            ? android.R.drawable.ic_media_pause
                            : android.R.drawable.ic_media_play),
                    label, label, pendingIntent);
            builder.setActions(Collections.singletonList(action));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Rect sourceRect = new Rect();
            videoPlayer.getGlobalVisibleRect(sourceRect);
            builder.setAutoEnterEnabled(autoEnter && isPictureInPicturePlaybackActive())
                    .setSourceRectHint(sourceRect);
        }
        return builder.build();
    }

    private boolean isPictureInPicturePlaybackActive() {
        if (videoPlayer == null) {
            return false;
        }
        int state = videoPlayer.getCurrentPlayer().getCurrentState();
        return state == GSYVideoView.CURRENT_STATE_PREPAREING
                || state == GSYVideoView.CURRENT_STATE_PLAYING
                || state == GSYVideoView.CURRENT_STATE_PLAYING_BUFFERING_START;
    }

    private void togglePictureInPicturePlayback() {
        if (videoPlayer == null || isCasting) {
            return;
        }
        if (isPictureInPicturePlaybackActive()) {
            videoPlayer.getCurrentPlayer().onVideoPause();
        } else if (videoPlayer.getCurrentPlayer().getCurrentState()
                == GSYVideoView.CURRENT_STATE_PAUSE) {
            videoPlayer.getCurrentPlayer().onVideoResume();
        } else {
            videoPlayer.getCurrentPlayer().startPlayLogic();
        }
        updatePictureInPictureParams();
    }

    private void addFullscreenControls(NormalGSYVideoPlayer player) {
        playbackControls.addFullscreenControls(player,
                videoInfo != null && videoInfo.getEpisodes() > 0, this::showFullscreenEpisodePicker);
    }

    private void showFullscreenEpisodePicker() {
        String[] episodes = new String[videoInfo.getEpisodes()];
        for (int i = 0; i < episodes.length; i++) {
            episodes[i] = getString(R.string.episode_number, i + 1);
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.episode_picker)
                .setSingleChoiceItems(episodes, Math.max(0, currentEpisode - 1),
                        (dialog, which) -> {
                            dialog.dismiss();
                            playEpisode(which + 1, true);
                        })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode,
                                               Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        pictureInPictureTransition = false;
        playbackControls.dismiss();
        ((PlaybackVideoPlayer) videoPlayer).setPictureInPictureUi(isInPictureInPictureMode);
        if (videoPlayer.getCurrentPlayer() != videoPlayer) {
            ((PlaybackVideoPlayer) videoPlayer.getCurrentPlayer()).setPictureInPictureUi(isInPictureInPictureMode);
        }
        toolbar.setVisibility(isInPictureInPictureMode ? View.GONE : View.VISIBLE);
        viewpagertab.setVisibility(isInPictureInPictureMode ? View.GONE : View.VISIBLE);
        viewpager.setVisibility(isInPictureInPictureMode ? View.GONE : View.VISIBLE);
        ViewGroup.LayoutParams containerParams = playerContainer.getLayoutParams();
        containerParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        containerParams.height = isInPictureInPictureMode
                ? ViewGroup.LayoutParams.MATCH_PARENT : ViewGroup.LayoutParams.WRAP_CONTENT;
        playerContainer.setLayoutParams(containerParams);
        ViewGroup.LayoutParams playerParams = videoPlayer.getLayoutParams();
        playerParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        playerParams.height = isInPictureInPictureMode
                ? ViewGroup.LayoutParams.MATCH_PARENT
                : (int) (200 * getResources().getDisplayMetrics().density);
        videoPlayer.setLayoutParams(playerParams);
    }

    private void showCastDevicePicker() {
        List<DlnaCastManager.Device> devices = new ArrayList<>();
        List<String> deviceNames = new ArrayList<>();
        for (DlnaCastManager.Device recent : castManager.getRecentDevices()) {
            devices.add(recent);
            deviceNames.add(getString(R.string.cast_recent_device, recent.getName()));
        }
        if (devices.isEmpty()) {
            deviceNames.add(getString(R.string.cast_searching));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, deviceNames);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.cast_device_picker_title)
                .setAdapter(adapter, (dialogInterface, position) -> {
                    if (!devices.isEmpty() && position < devices.size()) {
                        castToDevice(devices.get(position));
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.setOnDismissListener(ignored -> castManager.cancelDiscovery());
        dialog.show();

        castManager.discover(new DlnaCastManager.DiscoveryCallback() {
            @Override
            public void onDeviceFound(DlnaCastManager.Device device) {
                if (devices.contains(device)) {
                    return;
                }
                if (devices.isEmpty()) {
                    deviceNames.clear();
                }
                devices.add(device);
                deviceNames.add(device.getName());
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFinished() {
                if (devices.isEmpty()) {
                    deviceNames.clear();
                    deviceNames.add(getString(R.string.cast_no_device));
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onError(String error) {
                deviceNames.clear();
                deviceNames.add(getString(R.string.cast_search_failed, error));
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void restoreRememberedCast(Runnable localPlayback) {
        DlnaCastManager.Device remembered = castManager.getRememberedDevice();
        String url = currentVideoUrl;
        long generation = playbackGeneration;
        if (remembered == null || !TextUtils.equals(url, castManager.getRememberedMediaUrl())) {
            localPlayback.run();
            return;
        }
        castManager.getPlaybackStatus(remembered, new DlnaCastManager.PlaybackStatusCallback() {
            @Override public void onStatus(DlnaCastManager.PlaybackStatus status) {
                if (generation != playbackGeneration || isFinishing() || isDestroyed()) return;
                if (!status.ownsMedia(url) || !status.isActive()) {
                    localPlayback.run();
                    return;
                }
                castDevice = remembered;
                isCasting = true;
                castPositionMs = status.getPositionMs();
                castDurationMs = status.getDurationMs();
                castPlaying = status.isPlaying();
                castStatusFailures = 0;
                updatePictureInPictureParams();
                updateCastMenu();
                startCastStatusPolling();
            }
            @Override public void onError(String error) {
                if (generation == playbackGeneration) localPlayback.run();
            }
        });
    }

    private void detachCastSession() {
        isCasting = false;
        castDevice = null;
        castStatusGeneration++;
        castStatusHandler.removeCallbacks(castStatusPoller);
        updateCastMenu();
        updatePictureInPictureParams();
    }

    private void castToDevice(DlnaCastManager.Device targetDevice) {
        if (TextUtils.isEmpty(currentVideoUrl)) {
            return;
        }
        long position = isCasting ? castPositionMs : videoPlayer.getCurrentPositionWhenPlaying();
        boolean wasPlaying = isPictureInPicturePlaybackActive();
        long generation = playbackGeneration;
        castRequestPending = true;
        playbackControls.clearSubtitles();
        if (!isCasting) {
            GSYVideoManager.onPause();
        }
        castManager.cast(targetDevice, currentVideoUrl, position,
                new DlnaCastManager.CommandCallback() {
                    @Override
                    public void onSuccess() {
                        if (generation != playbackGeneration) return;
                        castRequestPending = false;
                        isCasting = true;
                        castDevice = targetDevice;
                        castPositionMs = position;
                        castDurationMs = 0;
                        castStatusFailures = 0;
                        castPlaying = true;
                        startCastStatusPolling();
                        updateCastMenu();
                        updatePictureInPictureParams();
                        showToast(getString(R.string.cast_connected, targetDevice.getName()));
                    }

                    @Override
                    public void onError(String error) {
                        if (generation != playbackGeneration) return;
                        castRequestPending = false;
                        playbackControls.bindSubtitles();
                        if (!isCasting && wasPlaying && !isPause) {
                            GSYVideoManager.onResume();
                        }
                        showToast(getString(R.string.cast_failed, error));
                    }
                });
    }

    private void castCurrentVideo(DlnaCastManager.Device targetDevice, long position) {
        long generation = playbackGeneration;
        castRequestPending = true;
        playbackControls.clearSubtitles();
        castManager.cast(targetDevice, currentVideoUrl, position,
                new DlnaCastManager.CommandCallback() {
                    @Override
                    public void onSuccess() {
                        if (generation != playbackGeneration) return;
                        castRequestPending = false;
                        castPositionMs = position;
                        castDurationMs = 0;
                        castStatusFailures = 0;
                        castPlaying = true;
                        startCastStatusPolling();
                        showToast(getString(R.string.cast_connected, targetDevice.getName()));
                    }

                    @Override
                    public void onError(String error) {
                        if (generation != playbackGeneration) return;
                        castRequestPending = false;
                        showToast(getString(R.string.cast_failed, error));
                    }
                });
    }

    private void showCastActions() {
        String toggle = getString(castPlaying ? R.string.cast_pause : R.string.cast_resume);
        String status = getString(R.string.cast_status, formatPlaybackPosition(castPositionMs),
                formatPlaybackPosition(castDurationMs));
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.cast_active_title, castDevice.getName()) + "\n" + status)
                .setItems(new String[]{toggle, getString(R.string.cast_rewind),
                                getString(R.string.cast_forward), getString(R.string.cast_volume),
                                getString(R.string.cast_stop), getString(R.string.cast_switch_device)},
                        (dialog, which) -> {
                            if (which == 0) {
                                toggleCastPlayback();
                            } else if (which == 1) {
                                seekCast(castPositionMs - 30_000L);
                            } else if (which == 2) {
                                seekCast(Math.min(castDurationMs > 0 ? castDurationMs : Long.MAX_VALUE,
                                        castPositionMs + 30_000L));
                            } else if (which == 3) {
                                showCastVolumeDialog();
                            } else if (which == 4) {
                                stopCastingAndResumeLocal();
                            } else if (which == 5) {
                                showCastDevicePicker();
                            }
                        })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void toggleCastPlayback() {
        DlnaCastManager.CommandCallback callback = castCommandCallback();
        if (castPlaying) {
            castManager.pause(castDevice, callback);
        } else {
            castManager.play(castDevice, callback);
        }
    }

    private void seekCast(long positionMs) {
        castManager.seek(castDevice, Math.max(0, positionMs), castCommandCallback());
    }

    private DlnaCastManager.CommandCallback castCommandCallback() {
        return new DlnaCastManager.CommandCallback() {
            @Override
            public void onSuccess() {
                startCastStatusPolling();
            }

            @Override
            public void onError(String error) {
                showToast(getString(R.string.cast_control_failed, error));
            }
        };
    }

    private void showCastVolumeDialog() {
        castManager.getVolume(castDevice, new DlnaCastManager.VolumeCallback() {
            @Override
            public void onVolume(int volume) {
                SeekBar seekBar = new SeekBar(GSYVVideoActivity.this);
                seekBar.setMax(100);
                seekBar.setProgress(Math.max(0, Math.min(volume, 100)));
                int padding = (int) (24 * getResources().getDisplayMetrics().density);
                seekBar.setPadding(padding, padding, padding, padding);
                new AlertDialog.Builder(GSYVVideoActivity.this)
                        .setTitle(R.string.cast_volume)
                        .setView(seekBar)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.save, (dialog, which) ->
                                castManager.setVolume(castDevice, seekBar.getProgress(),
                                        castCommandCallback()))
                        .show();
            }

            @Override
            public void onError(String error) {
                showToast(getString(R.string.cast_control_failed, error));
            }
        });
    }

    private void startCastStatusPolling() {
        castStatusGeneration++;
        castStatusHandler.removeCallbacks(castStatusPoller);
        castStatusHandler.post(castStatusPoller);
    }

    private void stopCastingAndResumeLocal() {
        DlnaCastManager.Device previousDevice = castDevice;
        boolean sameMedia = TextUtils.equals(currentVideoUrl,
                castManager.getRememberedMediaUrl());
        long resumePosition = sameMedia ? castPositionMs : 0;
        saveCurrentProgress();
        detachCastSession();
        if (previousDevice != null) {
            castManager.stop(previousDevice, null);
        }
        if (!TextUtils.isEmpty(currentVideoUrl)) {
            playLocalVideo(currentVideoUrl, currentVideoTitle, resumePosition);
        }
    }

    private void updateCastMenu() {
        playbackControls.dismiss();
        playbackControls.bindSubtitles();
        if (toolbar.getMenu().findItem(R.id.action_speed) != null) {
            onPrepareOptionsMenu(toolbar.getMenu());
        }
        if (castMenuItem != null) {
            castMenuItem.setIcon(isCasting
                    ? R.drawable.ic_cast_connected : R.drawable.ic_cast);
        }
    }

    private void updateFavoriteMenu() {
        if (favoriteMenuItem != null) {
            favoriteMenuItem.setTitle(favorite
                    ? R.string.remove_favorite : R.string.add_favorite);
            favoriteMenuItem.setIcon(favorite
                    ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
        }
    }

    private void downloadCurrentVideo() {
        if (TextUtils.isEmpty(currentVideoUrl) || downloadProbe != null || pendingDownload != null) return;
        showToast(getString(R.string.download_identifying));
        try {
            downloadProbe = VideoDownloads.get(this).prepare(currentVideoUrl, currentVideoTitle,
                    new VideoDownloads.RequestCallback() {
                        @Override public void onReady(DownloadRequest request) {
                            downloadProbe = null;
                            if (isFinishing() || isDestroyed()) return;
                            pendingDownload = request;
                            requestDownloadNotification();
                        }
                        @Override public void onError(String message) {
                            downloadProbe = null;
                            if (!isFinishing() && !isDestroyed()) showToast(message);
                        }
                    });
        } catch (IllegalArgumentException error) {
            showToast(getString(R.string.download_probe_failed));
        }
    }

    private void requestDownloadNotification() {
        if (isPause || pendingDownload == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                && !getPreferences(MODE_PRIVATE).getBoolean("download_notification_asked", false)) {
            getPreferences(MODE_PRIVATE).edit().putBoolean("download_notification_asked", true).apply();
            downloadNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        } else enqueuePendingDownload();
    }

    private void enqueuePendingDownload() {
        if (isPause || pendingDownload == null || isFinishing() || isDestroyed()) return;
        try {
            DownloadService.sendAddDownload(this, VideoDownloadService.class, pendingDownload, true);
            showToast(getString(R.string.download_started));
            pendingDownload = null;
        } catch (RuntimeException error) {
            pendingDownload = null;
            showToast(getString(R.string.download_failed));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        playbackHandler.postDelayed(progressSaver, 5000);
    }

    @Override
    protected void onStop() {
        playbackControls.dismiss();
        if (!isPause) {
            // PiP remains active through onPause, but its dismissal makes the Activity invisible.
            resumeLocalOnResume = !isCasting && isPictureInPicturePlaybackActive();
            GSYVideoManager.onPause();
            isPause = true;
        }
        saveCurrentProgress();
        playbackHandler.removeCallbacks(progressSaver);
        cancelPlaybackRetry();
        super.onStop();
    }

    @Override protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !isCasting && isPictureInPicturePlaybackActive()) {
            pictureInPictureTransition = true;
        }
    }

    @Override
    protected void onPause() {
        castStatusGeneration++;
        castStatusHandler.removeCallbacks(castStatusPoller);
        cancelPlaybackRetry();
        saveCurrentProgress();
        super.onPause();
        boolean inPictureInPicture = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && isInPictureInPictureMode();
        resumeLocalOnResume = !isCasting && isPictureInPicturePlaybackActive();
        if (!inPictureInPicture) {
            GSYVideoManager.onPause();
        }
        isPause = !inPictureInPicture;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !isInPictureInPictureMode()) {
            pictureInPictureTransition = false;
        }
        isPause = false;
        requestDownloadNotification();
        if (!isCasting && playbackStarted && !playbackCompleted && resumeLocalOnResume) {
            GSYVideoManager.onResume();
        } else if (isCasting) {
            startCastStatusPolling();
        }
        resumeLocalOnResume = false;
        if (deferredPlayback != null) {
            Runnable action = deferredPlayback;
            deferredPlayback = null;
            action.run();
        }
    }

    @Override
    protected void onDestroy() {
        playbackControls.dismiss();
        playbackControls.clearSubtitles();
        playbackGeneration++;
        deferredPlayback = null;
        if (downloadProbe != null) downloadProbe.cancel();
        playbackHandler.removeCallbacksAndMessages(null);
        if (resumeDialog != null) resumeDialog.dismiss();
        // DLNA 播放由电视独立维持；离开页面时仅释放本页资源，不发送 Stop。
        castManager.release();
        castStatusHandler.removeCallbacksAndMessages(null);
        //videoPlayer.release();
        GSYVideoManager.releaseAllVideos();
        if (orientationUtils != null)
            orientationUtils.releaseListener();
        if (tabMediator != null) {
            tabMediator.detach();
        }
        if (pictureInPictureReceiverRegistered) {
            unregisterReceiver(pictureInPictureReceiver);
            pictureInPictureReceiverRegistered = false;
        }
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        //如果旋转了就全屏
        boolean backUpIsPlay = isPlay;
        boolean inPictureInPicture = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isInPictureInPictureMode();
        if (!pictureInPictureTransition && !inPictureInPicture && !isPause && !isCasting
                && videoPlayer.getVisibility() == View.VISIBLE) {
            if (isADStarted()) {
                isPlay = false;
                videoPlayer.getCurrentPlayer().onConfigurationChanged(this, newConfig, orientationUtils, true, true);
            }
        }
        super.onConfigurationChanged(newConfig);
        isPlay = backUpIsPlay;
//        super.onConfigurationChanged(newConfig);
//        //如果旋转了就全屏
//        if (isPlay && !isPause) {
//            if (newConfig.orientation == ActivityInfo.SCREEN_ORIENTATION_USER) {
//                if (!videoPlayer.isIfCurrentIsFullscreen()) {
//                    videoPlayer.startWindowFullscreen(GSYVVideoActivity.this, true, true);
//                }
//            } else {
//                //新版本isIfCurrentIsFullscreen的标志位内部提前设置了，所以不会和手动点击冲突
//                if (videoPlayer.isIfCurrentIsFullscreen()) {
//                    GSYVideoADManager.backFromWindowFull(this);
//                }
//                if (orientationUtils != null) {
//                    orientationUtils.setEnable(true);
//                }
//            }
//        }
    }

    protected boolean isADStarted() {
        return videoPlayer.getCurrentPlayer().getCurrentState() >= 0 &&
                videoPlayer.getCurrentPlayer().getCurrentState() != GSYVideoView.CURRENT_STATE_NORMAL
                && videoPlayer.getCurrentPlayer().getCurrentState() != GSYVideoView.CURRENT_STATE_AUTO_COMPLETE;
    }


    private void resolveNormalVideoUI() {
        //增加title
        videoPlayer.getTitleTextView().setVisibility(View.GONE);
        videoPlayer.getTitleTextView().setText("测试视频");
        videoPlayer.getBackButton().setVisibility(View.GONE);
    }

    class MyAdapter extends FragmentStateAdapter {
        public MyAdapter(FragmentActivity activity) {
            super(activity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return fragments.get(position);
        }

        @Override
        public int getItemCount() {
            return fragments.size();
        }
    }
}
