package com.app.chao.chaoapp.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.app.chao.chaoapp.base.BasePresenter;

import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by Chao on 2017/3/14.
 */

public abstract class BaseFragment<T extends BasePresenter> extends Fragment {
    protected T mPresenter;
    protected Context mContext;
    View rootView;
    protected Unbinder unbinder;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(getLayout(), container, false);
        unbinder = ButterKnife.bind(this, rootView);
        mContext = getContext();
        initView(rootView);
        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (unbinder != null)
            unbinder.unbind();
    }

    protected abstract int getLayout();

    protected abstract void initView(View rootView);


}
