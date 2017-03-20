package com.app.chao.chaoapp.adapter;

import android.content.Context;
import android.view.ViewGroup;

import com.app.chao.chaoapp.adapter.holder.RelatedViewHolder;
import com.app.chao.chaoapp.bean.VideoInfo;
import com.jude.easyrecyclerview.adapter.BaseViewHolder;
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter;

/**
 * Description: 推荐
 */
public class RelatedAdapter extends RecyclerArrayAdapter<VideoInfo> {

    public RelatedAdapter(Context context) {
        super(context);
    }

    @Override
    public BaseViewHolder OnCreateViewHolder(ViewGroup parent, int viewType) {
        return new RelatedViewHolder(parent);
    }

}
