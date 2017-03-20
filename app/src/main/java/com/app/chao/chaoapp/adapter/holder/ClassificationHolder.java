package com.app.chao.chaoapp.adapter.holder;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.app.chao.chaoapp.R;

/**
 * Created by Chao on 2017/3/20.
 */

public class ClassificationHolder extends RecyclerView.ViewHolder {
    public ImageView imgPicture;
    public TextView tv_title;

    public ClassificationHolder(View itemView) {
        super(itemView);
        imgPicture = (ImageView) itemView.findViewById(R.id.img_video);
        tv_title = (TextView) itemView.findViewById(R.id.tv_title);
        imgPicture.setScaleType(ImageView.ScaleType.FIT_XY);
    }


}
