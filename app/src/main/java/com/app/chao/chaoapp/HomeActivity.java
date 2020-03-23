package com.app.chao.chaoapp;

import android.animation.Animator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.app.chao.chaoapp.base.Preconditions;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.contract.HomeActivityContract;
import com.app.chao.chaoapp.contract.impl.HomeActivityPresenter;
import com.app.chao.chaoapp.dagger.Persion;
import com.app.chao.chaoapp.ui.activity.BaseActivity;
import com.app.chao.chaoapp.ui.activity.PersonalCoreActivity;
import com.app.chao.chaoapp.ui.fragment.TabFragmentOne;
import com.app.chao.chaoapp.ui.fragment.TabFragmentTwo;
import com.app.chao.chaoapp.utils.ILayoutAnimationController;
import com.app.chao.chaoapp.utils.RxBus;
import com.app.chao.chaoapp.utils.StatusBarUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;

/**
 * Created by Chao on 2017/3/13.
 */

public class HomeActivity extends BaseActivity<HomeActivityPresenter> implements View.OnClickListener, HomeActivityContract.View {
    @BindView(R.id.toolbar)
    Toolbar toolbar;
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
    protected int getLayout() {
        return R.layout.activity_main;
    }

    @Override
    protected void init() {
        StatusBarUtils.setTranslucent(this);
        mDrawerLayout.setScrimColor(Color.TRANSPARENT);//设置取消阴影
        setSupportActionBar(toolbar);
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
        new HomeActivityPresenter(this);
        mPresenter.start();//加载全局数据，一次获取，然后子页面对数据做处理
        //mPresenter.getVideoBanner();

        MyAdapter adapter = new MyAdapter(getSupportFragmentManager(), this);
        viewpager.setAdapter(adapter);

        tabs.setupWithViewPager(viewpager);
        viewpager.setCurrentItem(0);
        //viewpager.setOffscreenPageLimit(3);
        TabLayout.TabLayoutOnPageChangeListener listener =
                new TabLayout.TabLayoutOnPageChangeListener(tabs);
        viewpager.addOnPageChangeListener(listener);

        ILayoutAnimationController.setLayoutAnimation(
                (ViewGroup) findViewById(R.id.tabs),
                R.anim.slide_left_in,
                0.8f,
                ILayoutAnimationController.IndexAlgorithm.INDEXSIMPLEPENDULUM);

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
        // 构造桥梁对象
        //MainComponent component = DaggerMainComponent.builder().mainModule(new MainModule(this)).build();
        //注入
        //component.inject(this);
        //Log.e("TAG", persion.toString());

    }

    @Inject
    Persion persion;//标明需要注入的对象

    @Inject

    @Override
    public void setPresenter(HomeActivityContract.Presenter presenter) {
        mPresenter = (HomeActivityPresenter) Preconditions.checkNotNull(presenter);
    }

    @Override
    public void showContent(List<VideoRes> videoRes) {
        RxBus.getDefault().postSticky(videoRes);//分发消息到其他页面
    }

    @Override
    public void showBanner(List<VideoRes> videoRes) {
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
                // 移除所有Sticky事件
                RxBus.getDefault().removeAllStickyEvents();
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
                    home_fab3.animate().setDuration(1200).setStartDelay(0).translationY(-home_fab.getHeight() * 2 - 10 * 2).start();
                } else if (!animateStart && !tag) {
                    animateStart = true;
                    home_fab2.setTag(true);
                    home_fab3.animate().setDuration(1200).translationY(0).start();
                    home_fab2.animate().setDuration(600).setStartDelay(0).translationY(0).start();
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
