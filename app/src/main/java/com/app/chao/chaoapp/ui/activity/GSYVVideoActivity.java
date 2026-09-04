package com.app.chao.chaoapp.ui.activity;

import android.content.Context;
import android.content.res.Configuration;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.app.chao.chaoapp.R;
import com.app.chao.chaoapp.base.Preconditions;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.cast.DlnaCastManager;
import com.app.chao.chaoapp.contract.VideoInfoContract;
import com.app.chao.chaoapp.ui.fragment.EpisodeSelectionFragment;
import com.app.chao.chaoapp.ui.fragment.VideoCommentFragment;
import com.app.chao.chaoapp.ui.fragment.VideoIntroFragment;
import com.app.chao.chaoapp.utils.ImageLoader;
import com.google.android.material.tabs.TabLayout;
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

public class GSYVVideoActivity extends BaseActivity implements VideoInfoContract.View,
        EpisodeSelectionFragment.OnEpisodeSelectedListener {
    VideoInfoContract.Presenter mPresenter;

    //推荐使用StandardGSYVideoPlayer，功能一致
    //CustomGSYVideoPlayer部分功能处于试验阶段
    NormalGSYVideoPlayer videoPlayer;

    Toolbar toolbar;
    private final List<String> titles = new ArrayList<>();
    VideoRes videoInfo;
    TabLayout viewpagertab;
    ViewPager viewpager;
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

    private OrientationUtils orientationUtils;


    @Override
    protected int getLayout() {
        return R.layout.activity_video_info_views;
    }

    @Override
    protected void init() {
        videoPlayer = findViewById(R.id.detail_player);
        toolbar = findViewById(R.id.toolbar);
        viewpagertab = findViewById(R.id.viewpagertab);
        viewpager = findViewById(R.id.viewpager);
        castManager = new DlnaCastManager(this);
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
                videoPlayer.startWindowFullscreen(GSYVVideoActivity.this, true, true);
            }
        });

        videoPlayer.setVideoAllCallBack(new SampleListener() {

            @Override
            public void onPrepared(String url, Object... objects) {
                //开始播放了才能旋转和全屏
                orientationUtils.setEnable(true);
                isPlay = true;
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
                    videoPlayer.startPlayLogic();//第三方播放器Bug，在某些情况下，第一次播放总会失败，设置播放错误的监听，重新播放。
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
        videoInfo = (VideoRes) getIntent().getSerializableExtra("videoInfo");
        fragments = new ArrayList<>();
        VideoIntroFragment videoIntroFragment = VideoIntroFragment.newInstance(videoInfo);
        VideoCommentFragment videoCommentFragment = new VideoCommentFragment();
        fragments.add(videoIntroFragment);
        titles.add("简介");
        if (videoInfo.getEpisodes() > 0) {
            fragments.add(EpisodeSelectionFragment.newInstance(videoInfo));
            titles.add("选集");
        }
        fragments.add(videoCommentFragment);
        titles.add("评论");


        MyAdapter adapter = new MyAdapter(getSupportFragmentManager(), this);
        viewpager.setAdapter(adapter);

        viewpagertab.setupWithViewPager(viewpager);
        viewpager.setCurrentItem(0);
        TabLayout.TabLayoutOnPageChangeListener listener =
                new TabLayout.TabLayoutOnPageChangeListener(viewpagertab);
        viewpager.addOnPageChangeListener(listener);


        toolbar.setTitle(videoInfo.getTitle());
        if (!TextUtils.isEmpty(videoInfo.getImg())) {
            ImageView imageView = new ImageView(this);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            ImageLoader.load(this, videoInfo.getImg(), imageView);
            videoPlayer.setThumbImageView(imageView);
        }
        if (!TextUtils.isEmpty(videoInfo.getVideo())) {
            playVideo(videoInfo.getVideo(), videoInfo.getTitle(), false);
        }

        //new VideoInfoPresenter(this, videoInfo);

    }

    @Override
    public void onEpisodeSelected(int episode) {
        String title = videoInfo.getTitle() + " 第" + episode + "集";
        playVideo(videoInfo.getEpisodeVideo(episode), title, true);
        toolbar.setTitle(title);
    }

    private void playVideo(String url, String title, boolean releaseCurrent) {
        if (TextUtils.isEmpty(url)) {
            return;
        }
        currentVideoUrl = url;
        currentVideoTitle = title;
        if (isCasting && castDevice != null) {
            castCurrentVideo(castDevice, 0);
            return;
        }
        playLocalVideo(url, title, releaseCurrent);
    }

    private void playLocalVideo(String url, String title, boolean releaseCurrent) {
        if (releaseCurrent) {
            GSYVideoManager.releaseAllVideos();
        }
        videoPlayer.setUp(url, true, null, title);
        videoPlayer.startPlayLogic();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.video_player, menu);
        castMenuItem = menu.findItem(R.id.action_cast);
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
        return super.onOptionsItemSelected(item);
    }

    private void showCastDevicePicker() {
        List<DlnaCastManager.Device> devices = new ArrayList<>();
        List<String> deviceNames = new ArrayList<>();
        deviceNames.add(getString(R.string.cast_searching));
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

    private void castToDevice(DlnaCastManager.Device targetDevice) {
        if (TextUtils.isEmpty(currentVideoUrl)) {
            return;
        }
        long position = isCasting ? 0 : videoPlayer.getCurrentPositionWhenPlaying();
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
                        updateCastMenu();
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
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.cast_active_title, castDevice.getName()))
                .setItems(new String[]{getString(R.string.cast_stop),
                                getString(R.string.cast_switch_device)},
                        (dialog, which) -> {
                            if (which == 0) {
                                stopCastingAndResumeLocal();
                            } else {
                                showCastDevicePicker();
                            }
                        })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void stopCastingAndResumeLocal() {
        DlnaCastManager.Device previousDevice = castDevice;
        isCasting = false;
        castDevice = null;
        updateCastMenu();
        if (previousDevice != null) {
            castManager.stop(previousDevice, null);
        }
        if (!TextUtils.isEmpty(currentVideoUrl)) {
            playLocalVideo(currentVideoUrl, currentVideoTitle, true);
        }
    }

    private void updateCastMenu() {
        if (castMenuItem != null) {
            castMenuItem.setIcon(isCasting
                    ? R.drawable.ic_cast_connected : R.drawable.ic_cast);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        GSYVideoManager.onPause();
        isPause = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isCasting) {
            GSYVideoManager.onResume();
        }
        isPause = false;
    }

    @Override
    protected void onDestroy() {
        // DLNA 播放由电视独立维持；离开页面时仅释放本页资源，不发送 Stop。
        castManager.release();
        //videoPlayer.release();
        GSYVideoManager.releaseAllVideos();
        if (orientationUtils != null)
            orientationUtils.releaseListener();
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

    @Override
    public void setPresenter(VideoInfoContract.Presenter presenter) {
        mPresenter = Preconditions.checkNotNull(presenter);
    }

    @Override
    public void showContent(VideoRes videoRes) {
        this.videoRes = videoRes;
        toolbar.setTitle(videoRes.title);
        if (!TextUtils.isEmpty(videoRes.getImg())) {
            ImageView imageView = new ImageView(this);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            ImageLoader.load(this, videoRes.getImg(), imageView);
            videoPlayer.setThumbImageView(imageView);
        }
        if (!TextUtils.isEmpty(videoRes.getVideo())) {
            playVideo(videoRes.getVideo(), videoRes.title, true);
        }
    }

    @Override
    public void showError(String error) {
        showToast(error);
    }

    class MyAdapter extends FragmentPagerAdapter {
        private Context context;

        public MyAdapter(FragmentManager fm, Context context) {
            super(fm);
            this.context = context;
        }

        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return titles.get(position);
//            return null;
        }
    }
}
