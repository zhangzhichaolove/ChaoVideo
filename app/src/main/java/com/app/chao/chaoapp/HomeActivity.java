package com.app.chao.chaoapp;

import android.animation.Animator;
import android.content.Intent;
import android.graphics.Color;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.app.chao.chaoapp.cast.DlnaCastManager;
import com.app.chao.chaoapp.net.ApiAddressManager;
import com.app.chao.chaoapp.ui.activity.BaseActivity;
import com.app.chao.chaoapp.ui.activity.PersonalCoreActivity;
import com.app.chao.chaoapp.ui.activity.ApiLogActivity;
import com.app.chao.chaoapp.ui.activity.VideoLibraryActivity;
import com.app.chao.chaoapp.ui.fragment.TabFragmentOne;
import com.app.chao.chaoapp.ui.fragment.TabFragmentTwo;
import com.app.chao.chaoapp.utils.ILayoutAnimationController;
import com.app.chao.chaoapp.utils.StatusBarUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;


/**
 * Created by Chao on 2017/3/13.
 */

public class HomeActivity extends BaseActivity implements View.OnClickListener {
    Toolbar toolbar;
    DrawerLayout mDrawerLayout;
    TabLayout tabs;
    ViewPager2 viewpager;
    FloatingActionButton home_fab;
    FloatingActionButton home_fab2;
    FloatingActionButton home_fab3;
    private DlnaCastManager castManager;
    private TabLayoutMediator tabMediator;

    private String[] mTitles = new String[]{"推荐", "动作", "剧情", "犯罪", "爱情", "悬疑", "惊悚", "科幻", "动画"};


    @Override
    protected int getLayout() {
        return R.layout.activity_main;
    }

    @Override
    protected void init() {
        toolbar = findViewById(R.id.toolbar);
        mDrawerLayout = findViewById(R.id.dl_left);
        tabs = findViewById(R.id.tabs);
        viewpager = findViewById(R.id.viewpager);
        home_fab = findViewById(R.id.home_fab);
        home_fab2 = findViewById(R.id.home_fab2);
        home_fab3 = findViewById(R.id.home_fab3);
        castManager = new DlnaCastManager(this);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                    mDrawerLayout.closeDrawer(GravityCompat.START);
                    return;
                }
                long secondTime = System.currentTimeMillis();
                if (secondTime - firstTime > 1500) {
                    showToast("再按一次退出");
                    firstTime = secondTime;
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
        StatusBarUtils.setTranslucent(this);
        mDrawerLayout.setScrimColor(Color.TRANSPARENT);//设置取消阴影
        setSupportActionBar(toolbar);
        //DrawerLayout要求Content View必须是DrawerLayout的第一个Child View
        //mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        //mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        NavigationView navigationView = (NavigationView) findViewById(R.id.id_nv_menu);
        navigationView.setNavigationItemSelectedListener(item -> {
            mDrawerLayout.closeDrawer(GravityCompat.START);
            if (item.getItemId() == R.id.wallet) {
                startActivity(VideoLibraryActivity.favoritesIntent(this));
                return true;
            }
            if (item.getItemId() == R.id.watch_history) {
                startActivity(VideoLibraryActivity.historyIntent(this));
                return true;
            }
            if (item.getItemId() == R.id.action_personal) {
                startActivity(new Intent(this, PersonalCoreActivity.class));
                return true;
            }
            return false;
        });
        View view = navigationView.inflateHeaderView(R.layout.header_just_username);
        //navigationView.setItemIconTintList(null);
        view.findViewById(R.id.iv_user_icon).setOnClickListener(this);
        home_fab.setOnClickListener(this);
        home_fab2.setOnClickListener(this);
        home_fab2.setOnLongClickListener(button -> {
            startActivity(new Intent(this, ApiLogActivity.class));
            return true;
        });
        home_fab3.setOnClickListener(this);
        initView();
    }

    private void initView() {
        attachPages();
        viewpager.setCurrentItem(0, false);

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
        mDrawerLayout.addDrawerListener(mDrawerToggle);

        home_fab2.setTag(true);
    }

    private void attachPages() {
        if (tabMediator != null) {
            tabMediator.detach();
        }
        viewpager.setAdapter(new MyAdapter(this));
        tabMediator = new TabLayoutMediator(tabs, viewpager,
                (tab, position) -> tab.setText(mTitles[position]));
        tabMediator.attach();
    }

    class MyAdapter extends FragmentStateAdapter {
        public MyAdapter(FragmentActivity activity) {
            super(activity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return position == 0 ? TabFragmentOne.newInstance() : TabFragmentTwo.newInstance(mTitles[position]);
        }

        @Override
        public int getItemCount() {
            return mTitles.length;
        }
    }

    private Long firstTime = 0L;

    boolean animateStart = false;

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.iv_user_icon) {
            if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                mDrawerLayout.closeDrawer(GravityCompat.START);
            }
            startActivity(new Intent(HomeActivity.this, PersonalCoreActivity.class));
        } else if (id == R.id.home_fab) {
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
        } else if (id == R.id.home_fab2) {
            showApiAddressDialog();
        } else if (id == R.id.home_fab3) {
            boolean stopping = castManager.stopRemembered(new DlnaCastManager.CommandCallback() {
                @Override
                public void onSuccess() {
                    showToast(getString(R.string.cast_stopped));
                }

                @Override
                public void onError(String error) {
                    showToast(getString(R.string.cast_stop_failed, error));
                }
            });
            if (!stopping) {
                showToast(getString(R.string.cast_not_active));
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (castManager != null) {
            castManager.release();
        }
        if (tabMediator != null) {
            tabMediator.detach();
        }
        super.onDestroy();
    }

    private void showApiAddressDialog() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setText(ApiAddressManager.getBaseUrl());
        input.setSelection(input.length());

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        content.setPadding(padding, 0, padding, 0);
        content.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        LinearLayout actions = new LinearLayout(this);
        Button restore = new Button(this);
        restore.setText(R.string.restore_default);
        Button test = new Button(this);
        test.setText(R.string.test_connection);
        actions.addView(restore, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        actions.addView(test, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        content.addView(actions);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.api_address_title)
                .setView(content)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                    if (!ApiAddressManager.saveBaseUrl(input.getText().toString())) {
                        input.setError(getString(R.string.api_address_invalid));
                        return;
                    }
                    dialog.dismiss();
                    showToast(getString(R.string.api_address_saved));
                    if (ApiAddressManager.getBaseUrl().startsWith("http://")) {
                        showToast(getString(R.string.cleartext_api_warning));
                    }
                    attachPages();
                });
            restore.setOnClickListener(view -> {
                input.setText(com.app.chao.chaoapp.net.VideoApis.HOST);
                input.setSelection(input.length());
            });
            test.setOnClickListener(view -> {
                test.setEnabled(false);
                test.setText(R.string.testing_connection);
                ApiAddressManager.testConnection(input.getText().toString(), (reachable, detail) -> {
                    test.setEnabled(true);
                    test.setText(R.string.test_connection);
                    showToast(getString(reachable
                                    ? R.string.connection_success : R.string.connection_failed,
                            detail == null ? getString(R.string.unknown_error) : detail));
                });
            });
        });
        dialog.show();
    }

}
