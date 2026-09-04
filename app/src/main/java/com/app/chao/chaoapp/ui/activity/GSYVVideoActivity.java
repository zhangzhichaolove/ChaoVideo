package com.app.chao.chaoapp.ui.activity;

import android.Manifest;
import android.content.res.Configuration;
import android.app.PictureInPictureParams;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Rational;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.ImageView;
import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.content.pm.PackageManager;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
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
import com.app.chao.chaoapp.ui.fragment.VideoCommentFragment;
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
import java.util.List;

import tv.danmaku.ijk.media.exo2.Exo2PlayerManager;
import tv.danmaku.ijk.media.exo2.ExoPlayerCacheManager;

public class GSYVVideoActivity extends BaseActivity implements
        EpisodeSelectionFragment.OnEpisodeSelectedListener {
    public static final String EXTRA_EPISODE = "episode";
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
    private PlaybackProgressStore playbackProgressStore;
    private VideoLibraryRepository libraryRepository;
    private MenuItem favoriteMenuItem;
    private boolean favorite;
    private int currentEpisode;
    private int playRetryCount;
    private float playbackSpeed = 1f;
    private TabLayoutMediator tabMediator;
    private FrameLayout playerContainer;
    private View playbackError;
    private long castPositionMs;
    private long castDurationMs;
    private boolean castPlaying = true;
    private int castStatusFailures;
    private final ActivityResultLauncher<String> storagePermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    enqueueCurrentVideoDownload();
                } else {
                    showToast(getString(R.string.download_permission_denied));
                }
            });
    private final Handler castStatusHandler = new Handler(Looper.getMainLooper());
    private final Runnable castStatusPoller = new Runnable() {
        @Override
        public void run() {
            if (!isCasting || castDevice == null) {
                return;
            }
            castManager.getPlaybackStatus(castDevice, new DlnaCastManager.PlaybackStatusCallback() {
                @Override
                public void onStatus(DlnaCastManager.PlaybackStatus status) {
                    castStatusFailures = 0;
                    castPositionMs = status.getPositionMs();
                    castDurationMs = status.getDurationMs();
                    castPlaying = status.isPlaying();
                    castStatusHandler.postDelayed(castStatusPoller, 3000);
                }

                @Override
                public void onError(String error) {
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
        videoPlayer = findViewById(R.id.detail_player);
        videoPlayer.post(this::updatePictureInPictureParams);
        playerContainer = findViewById(R.id.player_container);
        playbackError = findViewById(R.id.playback_error);
        findViewById(R.id.playback_retry).setOnClickListener(view -> {
            playbackError.setVisibility(View.GONE);
            startVideo(currentVideoUrl, currentVideoTitle,
                    playbackProgressStore.getPosition(currentVideoUrl));
        });
        toolbar = findViewById(R.id.toolbar);
        viewpagertab = findViewById(R.id.viewpagertab);
        viewpager = findViewById(R.id.viewpager);
        castManager = new DlnaCastManager(this);
        playbackProgressStore = new PlaybackProgressStore(this);
        libraryRepository = VideoLibraryRepository.get(this);
        // 必须在 setUp/startPlayLogic 之前选择播放器内核；完整 IJK 包已不再引入。
        PlayerFactory.setPlayManager(Exo2PlayerManager.class);
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
        GSYVideoType.setShowType(GSYVideoType.SCREEN_MATCH_FULL);

        //切换绘制模式
        //GSYVideoType.setRenderType(GSYVideoType.SUFRACE);
        //GSYVideoType.setRenderType(GSYVideoType.GLSURFACE);
        GSYVideoType.setRenderType(GSYVideoType.TEXTURE);

        //增加封面
        ImageView imageView = new ImageView(this);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setImageResource(R.mipmap.xxx1);
        videoPlayer.setThumbImageView(imageView);

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
                addFullscreenEpisodeButton(fullscreenPlayer);
            }
        });

        videoPlayer.setVideoAllCallBack(new SampleListener() {

            @Override
            public void onPrepared(String url, Object... objects) {
                if (isCasting) {
                    // A restored DLNA session owns playback. Preparation is asynchronous, so
                    // pause again here to prevent the local player from starting behind the TV.
                    GSYVideoManager.onPause();
                    return;
                }
                //开始播放了才能旋转和全屏
                orientationUtils.setEnable(true);
                isPlay = true;
                playRetryCount = 0;
                playbackError.setVisibility(View.GONE);
            }

            @Override
            public void onAutoComplete(String url, Object... objects) {
                long duration = videoPlayer.getDuration();
                libraryRepository.updateProgress(videoInfo, currentEpisode, duration, duration);
                playbackProgressStore.clear(currentVideoUrl);
                if (videoInfo != null && currentEpisode > 0
                        && currentEpisode < videoInfo.getEpisodes()) {
                    playEpisode(currentEpisode + 1, false);
                }
            }

            @Override
            public void onQuitFullscreen(String url, Object... objects) {
                if (orientationUtils != null) {
                    orientationUtils.backToProtVideo();
                }
            }

            @Override
            public void onPlayError(String url, Object... objects) {
                if (!isCasting && !castRequestPending) {
                    if (playRetryCount < MAX_PLAY_RETRIES) {
                        long delayMs = 500L * (1L << playRetryCount);
                        playRetryCount++;
                        videoPlayer.postDelayed(videoPlayer::startPlayLogic, delayMs);
                    } else {
                        showToast(getString(R.string.video_play_failed));
                        playbackError.setVisibility(View.VISIBLE);
                    }
                }
            }

        });

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            videoInfo = getIntent().getParcelableExtra("videoInfo", VideoRes.class);
        } else {
            //noinspection deprecation
            videoInfo = getIntent().getParcelableExtra("videoInfo");
        }
        if (videoInfo == null) {
            showToast(getString(R.string.video_missing));
            finish();
            return;
        }
        fragments = new ArrayList<>();
        VideoIntroFragment videoIntroFragment = VideoIntroFragment.newInstance(videoInfo);
        VideoCommentFragment videoCommentFragment = new VideoCommentFragment();
        fragments.add(videoIntroFragment);
        titles.add("简介");
        if (videoInfo.getEpisodes() > 0) {
            int requestedEpisode = getIntent().getIntExtra(EXTRA_EPISODE, 0);
            currentEpisode = requestedEpisode > 0
                    ? Math.max(1, Math.min(videoInfo.getEpisodes(), requestedEpisode))
                    : playbackProgressStore.getLastEpisode(videoInfo.getVideo(),
                    videoInfo.getEpisodes());
            episodeSelectionFragment = EpisodeSelectionFragment.newInstance(videoInfo);
            fragments.add(episodeSelectionFragment);
            titles.add("选集");
        }
        fragments.add(videoCommentFragment);
        titles.add("评论");


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
        if (!TextUtils.isEmpty(videoInfo.getVideo())) {
            if (currentEpisode > 0) {
                String title = videoInfo.getTitle() + " 第" + currentEpisode + "集";
                toolbar.setTitle(title);
                episodeSelectionFragment.setSelectedEpisode(currentEpisode);
                playVideo(videoInfo.getEpisodeVideo(currentEpisode), title);
            } else {
                playVideo(videoInfo.getVideo(), videoInfo.getTitle());
            }
        }
        libraryRepository.recordOpened(videoInfo, currentEpisode);
        restoreRememberedCast();

    }

    @Override
    public void onEpisodeSelected(int episode) {
        playEpisode(episode, true);
    }

    private void playEpisode(int episode, boolean savePrevious) {
        if (savePrevious && !isCasting && !TextUtils.isEmpty(currentVideoUrl)) {
            playbackProgressStore.save(currentVideoUrl,
                    videoPlayer.getCurrentPositionWhenPlaying());
        }
        currentEpisode = episode;
        playbackProgressStore.saveLastEpisode(videoInfo.getVideo(), episode);
        if (episodeSelectionFragment != null) {
            episodeSelectionFragment.setSelectedEpisode(episode);
        }
        String title = videoInfo.getTitle() + " 第" + episode + "集";
        playVideo(videoInfo.getEpisodeVideo(episode), title);
        toolbar.setTitle(title);
    }

    private void playVideo(String url, String title) {
        if (TextUtils.isEmpty(url)) {
            return;
        }
        currentVideoUrl = url;
        currentVideoTitle = title;
        playRetryCount = 0;
        long savedPosition = playbackProgressStore.getPosition(url);
        if (savedPosition >= MIN_RESUME_POSITION_MS) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.resume_playback_title)
                    .setMessage(getString(R.string.resume_playback_message,
                            formatPlaybackPosition(savedPosition)))
                    .setNegativeButton(R.string.play_from_start,
                            (dialog, which) -> startVideo(url, title, 0))
                    .setPositiveButton(R.string.continue_playing,
                            (dialog, which) -> startVideo(url, title, savedPosition))
                    .show();
            return;
        }
        startVideo(url, title, 0);
    }

    private void startVideo(String url, String title, long positionMs) {
        if (isCasting && castDevice != null) {
            castCurrentVideo(castDevice, positionMs);
            return;
        }
        playLocalVideo(url, title, positionMs);
    }

    private void playLocalVideo(String url, String title, long positionMs) {
        NormalGSYVideoPlayer target = (NormalGSYVideoPlayer) videoPlayer.getCurrentPlayer();
        target.setSeekOnStart(positionMs);
        target.setUp(url, true, null, title);
        target.setSpeed(playbackSpeed, true);
        target.startPlayLogic();
        addFullscreenEpisodeButton(target);
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
            showSpeedPicker();
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

    private void showSpeedPicker() {
        float[] values = {0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f};
        String[] labels = new String[values.length];
        int selected = 0;
        for (int i = 0; i < values.length; i++) {
            labels[i] = getString(R.string.speed_value,
                    java.math.BigDecimal.valueOf(values[i]).stripTrailingZeros().toPlainString());
            if (values[i] == playbackSpeed) {
                selected = i;
            }
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.playback_speed)
                .setSingleChoiceItems(labels, selected, (dialog, which) -> {
                    playbackSpeed = values[which];
                    videoPlayer.getCurrentPlayer().setSpeedPlaying(playbackSpeed, true);
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void enterPictureInPicture() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            showToast(getString(R.string.picture_in_picture_unavailable));
            return;
        }
        enterPictureInPictureMode(createPictureInPictureParams(false));
    }

    private void updatePictureInPictureParams() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && videoPlayer != null) {
            setPictureInPictureParams(createPictureInPictureParams(!isCasting));
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private PictureInPictureParams createPictureInPictureParams(boolean autoEnter) {
        PictureInPictureParams.Builder builder = new PictureInPictureParams.Builder()
                .setAspectRatio(new Rational(16, 9));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Rect sourceRect = new Rect();
            videoPlayer.getGlobalVisibleRect(sourceRect);
            builder.setAutoEnterEnabled(autoEnter)
                    .setSourceRectHint(sourceRect);
        }
        return builder.build();
    }

    private void addFullscreenEpisodeButton(NormalGSYVideoPlayer player) {
        if (videoInfo == null || videoInfo.getEpisodes() <= 0 || player == videoPlayer
                || player.findViewWithTag("episode_picker") != null) {
            return;
        }
        Button button = new Button(this);
        button.setTag("episode_picker");
        button.setText(R.string.episode_picker);
        button.setOnClickListener(view -> showFullscreenEpisodePicker());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.END);
        int margin = (int) (12 * getResources().getDisplayMetrics().density);
        params.setMargins(margin, margin, margin, margin);
        player.addView(button, params);
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

    private void restoreRememberedCast() {
        DlnaCastManager.Device remembered = castManager.getRememberedDevice();
        if (remembered == null) {
            return;
        }
        castDevice = remembered;
        isCasting = true;
        GSYVideoManager.onPause();
        videoPlayer.post(GSYVideoManager::onPause);
        updatePictureInPictureParams();
        updateCastMenu();
        startCastStatusPolling();
    }

    private void castToDevice(DlnaCastManager.Device targetDevice) {
        if (TextUtils.isEmpty(currentVideoUrl)) {
            return;
        }
        long position = isCasting ? castPositionMs : videoPlayer.getCurrentPositionWhenPlaying();
        castRequestPending = true;
        if (!isCasting) {
            GSYVideoManager.onPause();
        }
        castManager.cast(targetDevice, currentVideoUrl, position,
                new DlnaCastManager.CommandCallback() {
                    @Override
                    public void onSuccess() {
                        castRequestPending = false;
                        isCasting = true;
                        castDevice = targetDevice;
                        castPositionMs = position;
                        castPlaying = true;
                        startCastStatusPolling();
                        updateCastMenu();
                        updatePictureInPictureParams();
                        showToast(getString(R.string.cast_connected, targetDevice.getName()));
                    }

                    @Override
                    public void onError(String error) {
                        castRequestPending = false;
                        if (!isCasting) {
                            GSYVideoManager.onResume();
                        }
                        showToast(getString(R.string.cast_failed, error));
                    }
                });
    }

    private void castCurrentVideo(DlnaCastManager.Device targetDevice, long position) {
        castRequestPending = true;
        castManager.cast(targetDevice, currentVideoUrl, position,
                new DlnaCastManager.CommandCallback() {
                    @Override
                    public void onSuccess() {
                        castRequestPending = false;
                        castPositionMs = position;
                        castPlaying = true;
                        startCastStatusPolling();
                        showToast(getString(R.string.cast_connected, targetDevice.getName()));
                    }

                    @Override
                    public void onError(String error) {
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
        castStatusHandler.removeCallbacks(castStatusPoller);
        castStatusHandler.post(castStatusPoller);
    }

    private void stopCastingAndResumeLocal() {
        DlnaCastManager.Device previousDevice = castDevice;
        boolean sameMedia = TextUtils.equals(currentVideoUrl,
                castManager.getRememberedMediaUrl());
        long resumePosition = sameMedia ? castPositionMs : 0;
        isCasting = false;
        castDevice = null;
        updateCastMenu();
        updatePictureInPictureParams();
        castStatusHandler.removeCallbacks(castStatusPoller);
        if (previousDevice != null) {
            castManager.stop(previousDevice, null);
        }
        if (!TextUtils.isEmpty(currentVideoUrl)) {
            playLocalVideo(currentVideoUrl, currentVideoTitle, resumePosition);
        }
    }

    private void updateCastMenu() {
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
        if (TextUtils.isEmpty(currentVideoUrl)) {
            showToast(getString(R.string.video_missing));
            return;
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return;
        }
        enqueueCurrentVideoDownload();
    }

    private void enqueueCurrentVideoDownload() {
        try {
            String name = currentVideoTitle == null ? "video" : currentVideoTitle;
            name = name.replaceAll("[\\\\/:*?\"<>|]", "_") + ".mp4";
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(currentVideoUrl))
                    .setTitle(currentVideoTitle)
                    .setNotificationVisibility(
                            DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES, name);
            DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            manager.enqueue(request);
            showToast(getString(R.string.download_started));
        } catch (RuntimeException error) {
            showToast(getString(R.string.download_failed));
        }
    }

    @Override
    protected void onPause() {
        castStatusHandler.removeCallbacks(castStatusPoller);
        if (!isCasting && !TextUtils.isEmpty(currentVideoUrl)) {
            long position = videoPlayer.getCurrentPositionWhenPlaying();
            long duration = videoPlayer.getDuration();
            playbackProgressStore.save(currentVideoUrl, position);
            libraryRepository.updateProgress(videoInfo, currentEpisode, position, duration);
        } else if (isCasting) {
            libraryRepository.updateProgress(videoInfo, currentEpisode,
                    castPositionMs, castDurationMs);
        }
        super.onPause();
        GSYVideoManager.onPause();
        isPause = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isCasting) {
            GSYVideoManager.onResume();
        } else {
            startCastStatusPolling();
        }
        isPause = false;
    }

    @Override
    protected void onDestroy() {
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
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        //如果旋转了就全屏
        boolean backUpIsPlay = isPlay;
        if (!isPause && !isCasting && videoPlayer.getVisibility() == View.VISIBLE) {
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
