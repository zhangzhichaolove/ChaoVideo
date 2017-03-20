package com.app.chao.chaoapp.adapter;

import android.content.Context;
import android.view.ViewGroup;

import com.app.chao.chaoapp.adapter.holder.FragmentOneViewHolder;
import com.app.chao.chaoapp.bean.VideoInfo;
import com.jude.easyrecyclerview.adapter.BaseViewHolder;
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter;

public class FragmentOneAdapter extends RecyclerArrayAdapter<VideoInfo> {

    public FragmentOneAdapter(Context context) {
        super(context);
    }

    @Override
    public BaseViewHolder OnCreateViewHolder(ViewGroup parent, int viewType) {
        return new FragmentOneViewHolder(parent);
    }

}
