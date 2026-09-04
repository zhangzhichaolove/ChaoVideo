package com.app.chao.chaoapp.ui.fragment;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.app.chao.chaoapp.R;


/**
 * Created by Chao on 2017/3/21.
 */

public class TabFragmentThree extends BaseFragment {
    static TabFragmentThree fragment;
    RecyclerView recyclerView;

    public static TabFragmentThree newInstance() {
        //if (fragment==null)
        fragment = new TabFragmentThree();
        return fragment;
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_tabone;
    }

    @Override
    protected void initView(View inflater) {
        recyclerView = inflater.findViewById(R.id.recyclerView);

    }


}
