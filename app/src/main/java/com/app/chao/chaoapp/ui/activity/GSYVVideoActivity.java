package com.app.chao.chaoapp.ui.activity;

import android.content.Context;
import android.content.res.Configuration;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.app.chao.chaoapp.R;
import com.app.chao.chaoapp.base.Preconditions;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.contract.VideoInfoContract;
import com.app.chao.chaoapp.ui.fragment.VideoCommentFragment;
import com.app.chao.chaoapp.ui.fragment.VideoIntroFragment;
import com.app.chao.chaoapp.utils.ImageLoader;
import com.google.android.material.tabs.TabLayout;
import com.shuyu.gsyvideoplayer.GSYVideoManager;
import com.shuyu.gsyvideoplayer.cache.CacheFactory;
import com.shuyu.gsyvideoplayer.cache.ProxyCacheManager;
import com.shuyu.gsyvideoplayer.listener.LockClickListener;
import com.shuyu.gsyvideoplayer.player.PlayerFactory;
import com.shuyu.gsyvideoplayer.utils.GSYVideoType;
import com.shuyu.gsyvideoplayer.utils.OrientationUtils;
import com.shuyu.gsyvideoplayer.video.NormalGSYVideoPlayer;
import com.shuyu.gsyvideoplayer.video.base.GSYVideoView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import tv.danmaku.ijk.media.exo2.Exo2PlayerManager;

public class GSYVVideoActivity extends BaseActivity implements VideoInfoContract.View {
    VideoInfoContract.Presenter mPresenter;

    //推荐使用StandardGSYVideoPlayer，功能一致
    //CustomGSYVideoPlayer部分功能处于试验阶段
    @BindView(R.id.detail_player)
    NormalGSYVideoPlayer videoPlayer;

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    //    private String[] mTitles = new String[]{"简介", "评论"};
    private String[] mTitles = new String[]{"简介"};
    VideoRes videoInfo;
    @BindView(R.id.viewpagertab)
    TabLayout viewpagertab;
    @BindView(R.id.viewpager)
    ViewPager viewpager;
    VideoRes videoRes;
    List<Fragment> fragments;


    private boolean isPlay;
    private boolean isPause;

    private OrientationUtils orientationUtils;


    @Override
    protected int getLayout() {
        return R.layout.activity_video_info_views;
    }

    @Override
    protected void init() {
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
        PlayerFactory.setPlayManager(Exo2PlayerManager.class);
        //系统内核模式
        //PlayerFactory.setPlayManager(SystemPlayerManager.class);
        //ijk内核，默认模式
        //PlayerFactory.setPlayManager(IjkPlayerManager.class);
        //exo缓存模式，支持m3u8，只支持exo
        //CacheFactory.setCacheManager(ExoPlayerCacheManager.class);
        //代理缓存模式，支持所有模式，不支持m3u8等，默认
        CacheFactory.setCacheManager(ProxyCacheManager.class);
        GSYVideoType.setShowType(GSYVideoType.SCREEN_MATCH_FULL);

        //切换绘制模式
        GSYVideoType.setRenderType(GSYVideoType.SUFRACE);
        GSYVideoType.setRenderType(GSYVideoType.GLSURFACE);
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
                videoPlayer.startPlayLogic();//第三方播放器Bug，在某些情况下，第一次播放总会失败，设置播放错误的监听，重新播放。
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
        showContent(videoInfo);
        fragments = new ArrayList<>();
        VideoIntroFragment videoIntroFragment = VideoIntroFragment.newInstance(videoInfo);
        VideoCommentFragment videoCommentFragment = new VideoCommentFragment();
        fragments.add(videoIntroFragment);
        fragments.add(videoCommentFragment);


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
            videoPlayer.setUp(videoInfo.getVideo(), true, null, videoInfo.getTitle());
            videoPlayer.startPlayLogic();
        }

        //new VideoInfoPresenter(this, videoInfo);

    }

    @Override
    public void onBackPressed() {
        if (orientationUtils != null) {
            orientationUtils.backToProtVideo();
        }
        if (GSYVideoManager.backFromWindowFull(this)) {
            return;
        }
        super.onBackPressed();
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
        GSYVideoManager.onResume();
        isPause = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //videoPlayer.release();
        GSYVideoManager.releaseAllVideos();
        if (orientationUtils != null)
            orientationUtils.releaseListener();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        //如果旋转了就全屏
        boolean backUpIsPlay = isPlay;
        if (!isPause && videoPlayer.getVisibility() == View.VISIBLE) {
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
            videoPlayer.setUp(videoRes.getVideo(), true, null, videoRes.title);
            videoPlayer.startPlayLogic();
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
            return mTitles.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mTitles[position];
//            return null;
        }
    }
}