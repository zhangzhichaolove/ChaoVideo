package com.app.chao.chaoapp;

import android.animation.Animator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.app.chao.chaoapp.ui.activity.BaseActivity;
import com.app.chao.chaoapp.ui.activity.PersonalCoreActivity;
import com.app.chao.chaoapp.ui.fragment.TabFragmentOne;
import com.app.chao.chaoapp.ui.fragment.TabFragmentTwo;
import com.app.chao.chaoapp.utils.StatusBarUtils;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Chao on 2017/3/13.
 */

public class HomeActivity extends BaseActivity implements View.OnClickListener {
    @BindView(R.id.dl_left)
    DrawerLayout mDrawerLayout;
    @BindView(R.id.tabs)
    TabLayout tabs;
    @BindView(R.id.viewpager)
    ViewPager viewpager;
    @BindView(R.id.home_fab)
    FloatingActionButton home_fab;
    @BindView(R.id.home_fab2)
    FloatingActionButton home_fab2;
    @BindView(R.id.home_fab3)
    FloatingActionButton home_fab3;

    private String[] mTitles = new String[]{"推荐", "专题", "动漫", "喜剧", "爱情", "悬疑", "惊悚", "科幻", "记录"};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //getSupportActionBar().hide();
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        StatusBarUtils.setTranslucent(this);
        mDrawerLayout.setScrimColor(Color.TRANSPARENT);//设置取消阴影
        //DrawerLayout要求Content View必须是DrawerLayout的第一个Child View
        //mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        //mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        NavigationView navigationView = (NavigationView) findViewById(R.id.id_nv_menu);
        View view = navigationView.inflateHeaderView(R.layout.header_just_username);
        //navigationView.setItemIconTintList(null);
        view.findViewById(R.id.iv_user_icon).setOnClickListener(this);
        home_fab.setOnClickListener(this);
        home_fab2.setOnClickListener(this);
        home_fab3.setOnClickListener(this);
        initView();
    }

    private void initView() {
        MyAdapter adapter = new MyAdapter(getSupportFragmentManager(), this);
        viewpager.setAdapter(adapter);

        tabs.setupWithViewPager(viewpager);
        viewpager.setCurrentItem(0);
        //viewpager.setOffscreenPageLimit(3);
        TabLayout.TabLayoutOnPageChangeListener listener =
                new TabLayout.TabLayoutOnPageChangeListener(tabs);
        viewpager.addOnPageChangeListener(listener);

        Toolbar tl = (Toolbar) findViewById(R.id.toolbar);
        tl.setTitle("");
        tl.setLogo(R.mipmap.bilibili);
        setSupportActionBar(tl);
        getSupportActionBar().setHomeButtonEnabled(true); //设置返回键可用
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, tl, R.string.open, R.string.close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);

            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);

            }
        };

        mDrawerToggle.syncState();
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        home_fab2.setTag(true);
    }

    class MyAdapter extends FragmentPagerAdapter {
        private Context context;

        public MyAdapter(FragmentManager fm, Context context) {
            super(fm);
            this.context = context;
        }

        @Override
        public Fragment getItem(int position) {
            return position % 2 == 0 ? TabFragmentOne.newInstance() : TabFragmentTwo.newInstance();
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

    private Long firstTime = 0L;

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            long secondTime = System.currentTimeMillis();
            if (secondTime - firstTime > 1500) {
                showToast("再按一次退出");
                firstTime = secondTime;
            } else {
                super.onBackPressed();
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(0);
            }
        }

    }

    boolean animateStart = false;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_user_icon:
                if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                    mDrawerLayout.closeDrawer(GravityCompat.START);
                }
                startActivity(new Intent(HomeActivity.this, PersonalCoreActivity.class));
                break;
            case R.id.home_fab:
                boolean tag = ((boolean) home_fab2.getTag());
                if (!animateStart && tag) {//没有动画在执行
                    animateStart = true;
                    home_fab2.setTag(false);
                    home_fab2.animate().setDuration(600).translationY(-home_fab.getHeight() - 10).start();
                    home_fab3.animate().setDuration(600).setStartDelay(300).translationY(-home_fab.getHeight() * 2 - 10 * 2).start();
                } else if (!animateStart && !tag) {
                    animateStart = true;
                    home_fab2.setTag(true);
                    home_fab3.animate().setDuration(600).translationY(0).start();
                    home_fab2.animate().setDuration(600).setStartDelay(300).translationY(0).start();
                }
                home_fab3.animate().setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        animateStart = false;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
                break;
            case R.id.home_fab2:
                showToast("home_fab2");
                break;
            case R.id.home_fab3:
                showToast("home_fab3");
                break;
        }
    }

}
