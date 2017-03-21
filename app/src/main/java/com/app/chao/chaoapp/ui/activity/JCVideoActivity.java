package com.app.chao.chaoapp.ui.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.app.chao.chaoapp.R;
import com.app.chao.chaoapp.bean.VideoInfo;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.contract.VideoInfoContract;
import com.app.chao.chaoapp.contract.impl.VideoInfoPresenter;
import com.app.chao.chaoapp.ui.fragment.VideoCommentFragment;
import com.app.chao.chaoapp.ui.fragment.VideoIntroFragment;
import com.app.chao.chaoapp.utils.ImageLoader;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import fm.jiecao.jcvideoplayer_lib.JCVideoPlayer;
import fm.jiecao.jcvideoplayer_lib.JCVideoPlayerStandard;

public class JCVideoActivity extends BaseActivity implements VideoInfoContract.View {
    VideoInfoContract.Presenter mPresenter;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    private String[] mTitles = new String[]{"简介", "评论"};
    VideoInfo videoInfo;
    @BindView(R.id.videoplayer)
    JCVideoPlayerStandard videoplayer;
    @BindView(R.id.viewpagertab)
    TabLayout viewpagertab;
    @BindView(R.id.viewpager)
    ViewPager viewpager;
    VideoRes videoRes;
    List<Fragment> fragments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_info_view);
        ButterKnife.bind(this);
        getIntentData();
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        //mPresenter = new VideoInfoPresenter(videoInfoView, videoInfo);
    }

    private void getIntentData() {
        fragments = new ArrayList<>();
        VideoIntroFragment videoIntroFragment = new VideoIntroFragment();
        VideoCommentFragment videoCommentFragment = new VideoCommentFragment();
        fragments.add(videoIntroFragment);
        fragments.add(videoCommentFragment);

        videoInfo = (VideoInfo) getIntent().getSerializableExtra("videoInfo");

        MyAdapter adapter = new MyAdapter(getSupportFragmentManager(), this);
        viewpager.setAdapter(adapter);

        viewpagertab.setupWithViewPager(viewpager);
        viewpager.setCurrentItem(0);
        TabLayout.TabLayoutOnPageChangeListener listener =
                new TabLayout.TabLayoutOnPageChangeListener(viewpagertab);
        viewpager.addOnPageChangeListener(listener);

        videoplayer.thumbImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        videoplayer.backButton.setVisibility(View.GONE);
        videoplayer.titleTextView.setVisibility(View.GONE);
        videoplayer.tinyBackImageView.setVisibility(View.GONE);

        new VideoInfoPresenter(this, videoInfo);

    }

    @Override
    protected void onPause() {
        super.onPause();
        JCVideoPlayer.releaseAllVideos();
    }

    @Override
    public void onBackPressed() {
        if (JCVideoPlayer.backPress()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void setPresenter(VideoInfoContract.Presenter presenter) {
        mPresenter = presenter;
    }

    @Override
    public void showContent(VideoRes videoRes) {
        this.videoRes = videoRes;
        toolbar.setTitle(videoRes.title);
        if (!TextUtils.isEmpty(videoRes.pic))
            ImageLoader.load(this, videoRes.pic, videoplayer.thumbImageView);
        if (!TextUtils.isEmpty(videoRes.getVideoUrl())) {
            Log.e("TAG", videoRes.getVideoUrl());
            videoplayer.setUp(videoRes.getVideoUrl()
                    , JCVideoPlayerStandard.SCREEN_LAYOUT_LIST, videoRes.title);
            videoplayer.onClick(videoplayer.thumbImageView);
        }
//        videoplayer.setUp("http://2449.vod.myqcloud.com/2449_22ca37a6ea9011e5acaaf51d105342e3.f20.mp4"
//                , JCVideoPlayerStandard.SCREEN_LAYOUT_NORMAL, "嫂子闭眼睛");
        //       videoplayer.thumbImageView.setImageURI(Uri.parse("http://p.qpic.cn/videoyun/0/2449_43b6f696980311e59ed467f22794e792_1/640"));
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