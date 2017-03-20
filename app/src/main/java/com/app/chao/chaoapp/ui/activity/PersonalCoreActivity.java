package com.app.chao.chaoapp.ui.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.app.chao.chaoapp.R;
import com.app.chao.chaoapp.ui.fragment.PersonalCoreFragment;
import com.app.chao.chaoapp.utils.StatusBarUtils;

/**
 * Created by Chao on 2017/3/13.
 */

public class PersonalCoreActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_core);
//        setTitle("个人中心");
//        getSupportActionBar().setHomeButtonEnabled(true); //设置返回键可用
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);//TODO 此页面toolbar未使用。
        toolbar.setTitle("个人中心");
        setSupportActionBar(toolbar);
        //关键下面两句话，设置了回退按钮，及点击事件的效果
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        getSupportActionBar().hide();
        StatusBarUtils.setTranslucent(this);

        FragmentManager fragmentManager = getSupportFragmentManager();
        PersonalCoreFragment fragment = (PersonalCoreFragment) fragmentManager.findFragmentByTag(PersonalCoreFragment.class.getName());
        if (fragment == null) {
            fragment = PersonalCoreFragment.newInstance();
            getSupportFragmentManager().beginTransaction().add(R.id.fl_cotent, fragment, fragment.getClass().getName()).commit();
        }
        //new LoginPresenter(fragment);
    }

    @Override//返回键监听
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            default:
                break;
        }
        return true;
    }
}
