package com.app.chao.chaoapp.ui.fragment;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.widget.Toolbar;

import com.app.chao.chaoapp.R;
import com.app.chao.chaoapp.ui.activity.PersonalCoreActivity;
import com.app.chao.chaoapp.utils.StatusBarUtils;
import com.google.android.material.appbar.CollapsingToolbarLayout;

/**
 * Created by Chao on 2017/3/13.
 */

public class PersonalCoreFragment extends BaseFragment {
    Toolbar toolbar;
    CollapsingToolbarLayout collapsing_toolbar;

    public static PersonalCoreFragment newInstance() {
        Bundle args = new Bundle();
        PersonalCoreFragment fragment = new PersonalCoreFragment();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    protected int getLayout() {
        return R.layout.fragment_personal_core;
    }

    @Override
    protected void initView(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        collapsing_toolbar = view.findViewById(R.id.collapsing_toolbar);
        PersonalCoreActivity activity = (PersonalCoreActivity) requireActivity();
        activity.setSupportActionBar(toolbar);
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        collapsing_toolbar.setTitle(getString(R.string.user_profile));
        toolbar.setNavigationOnClickListener(clicked -> activity.finish());
        StatusBarUtils.applyTopInset(view, toolbar);
    }

}
