package com.app.chao.chaoapp.adapter;

import android.content.Context;
import android.view.ViewGroup;

import com.app.chao.chaoapp.adapter.holder.CommentViewHolder;
import com.app.chao.chaoapp.bean.VideoType;
import com.jude.easyrecyclerview.adapter.BaseViewHolder;
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter;

/**
 * Description: 评论列表
 */
public class CommentAdapter extends RecyclerArrayAdapter<String> {


    public CommentAdapter(Context context) {
        super(context);
    }

    @Override
    public BaseViewHolder OnCreateViewHolder(ViewGroup parent, int viewType) {
        return new CommentViewHolder(parent);
    }


}
