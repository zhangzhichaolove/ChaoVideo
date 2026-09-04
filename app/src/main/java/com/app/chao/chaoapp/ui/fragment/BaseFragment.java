package com.app.chao.chaoapp.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.app.chao.chaoapp.base.BasePresenter;

/**
 * Created by Chao on 2017/3/14.
 */

public abstract class BaseFragment<T extends BasePresenter<?>> extends Fragment {
    protected T mPresenter;
    protected Context mContext;
    View rootView;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(getLayout(), container, false);
        mContext = getContext();
        initView(rootView);
        return rootView;
    }

    @Override
    public void onDestroyView() {
        if (mPresenter != null) {
            mPresenter.detachView();
        }
        mPresenter = null;
        rootView = null;
        super.onDestroyView();
    }

    @Override
    public void onDetach() {
        mContext = null;
        super.onDetach();
    }

    protected abstract int getLayout();

    protected abstract void initView(View rootView);


}
