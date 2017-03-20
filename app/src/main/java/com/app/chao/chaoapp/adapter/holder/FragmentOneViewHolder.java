package com.app.chao.chaoapp.adapter.holder;

import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.app.chao.chaoapp.R;
import com.app.chao.chaoapp.bean.VideoInfo;
import com.app.chao.chaoapp.utils.ImageLoader;
import com.jude.easyrecyclerview.adapter.BaseViewHolder;

public class FragmentOneViewHolder extends BaseViewHolder<VideoInfo> {
    ImageView imgPicture;
    TextView tv_title;

    public FragmentOneViewHolder(ViewGroup parent) {
        super(parent, R.layout.item_video);
        imgPicture = $(R.id.img_video);
        tv_title = $(R.id.tv_title);
        imgPicture.setScaleType(ImageView.ScaleType.CENTER_CROP);
    }

    @Override
    public void setData(VideoInfo data) {
        tv_title.setText(data.title);
        ImageLoader.load(getContext(), data.pic, imgPicture);
    }
}