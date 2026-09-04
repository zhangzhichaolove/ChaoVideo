package com.app.chao.chaoapp.ui.activity;

import android.view.MenuItem;
import androidx.fragment.app.FragmentManager;

import com.app.chao.chaoapp.R;
import com.app.chao.chaoapp.ui.fragment.PersonalCoreFragment;
import com.app.chao.chaoapp.utils.StatusBarUtils;

/**
 * Created by Chao on 2017/3/13.
 */

public class PersonalCoreActivity extends BaseActivity {

    @Override
    protected int getLayout() {
        return R.layout.activity_core;
    }

    @Override
    protected void init() {
        StatusBarUtils.setTranslucent(this);

        FragmentManager fragmentManager = getSupportFragmentManager();
        PersonalCoreFragment fragment = (PersonalCoreFragment) fragmentManager.findFragmentByTag(PersonalCoreFragment.class.getName());
        if (fragment == null) {
            fragment = PersonalCoreFragment.newInstance();
            getSupportFragmentManager().beginTransaction().add(R.id.fl_cotent, fragment, fragment.getClass().getName()).commit();
        }
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
