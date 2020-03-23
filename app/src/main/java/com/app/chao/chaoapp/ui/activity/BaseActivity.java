package com.app.chao.chaoapp.ui.activity;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.app.chao.chaoapp.base.BasePresenter;

import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by Chao on 2017/3/14.
 */

public abstract class BaseActivity<T extends BasePresenter> extends AppCompatActivity {
    protected T mPresenter;
    Unbinder bind;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayout());
        bind = ButterKnife.bind(this);
        init();
    }

    protected abstract int getLayout();

    protected abstract void init();

    protected void showToast(String coutent) {
        Toast.makeText(this, coutent, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPresenter = null;
        bind.unbind();
    }
}
