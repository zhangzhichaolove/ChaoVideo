package com.app.chao.chaoapp.adapter.holder;

import android.view.ViewGroup;
import android.widget.TextView;

import com.app.chao.chaoapp.R;
import com.app.chao.chaoapp.utils.ImageLoader;
import com.app.chao.chaoapp.view.CImageView;
import com.jude.easyrecyclerview.adapter.BaseViewHolder;

/**
 * Description: 评论列表
 */

public class CommentViewHolder extends BaseViewHolder<String> {
    CImageView avatar;
    TextView tv_nick;
    TextView tv_time;
    TextView tv_like;
    TextView tv_comment;

    public CommentViewHolder(ViewGroup parent) {
        super(parent, R.layout.item_comment);
        avatar = $(R.id.avatar);
        tv_nick = $(R.id.tv_nick);
        tv_time = $(R.id.tv_time);
        tv_like = $(R.id.tv_like);
        tv_comment = $(R.id.tv_comment);
    }

    @Override
    public void setData(String data) {
        tv_nick.setText("13594347818");
        tv_time.setText("2020-06-18");
        tv_like.setText("168");
        tv_comment.setText("哇，简直是太好看啦。");
        ImageLoader.load(getContext(), "https://c-ssl.duitang.com/uploads/item/201504/02/20150402H5812_cshdn.jpeg", avatar);
    }
}
