package com.app.chao.chaoapp.adapter;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.app.chao.chaoapp.R;
import com.app.chao.chaoapp.baseadapter.recyclerview.CommonAdapter;
import com.app.chao.chaoapp.baseadapter.recyclerview.base.ViewHolder;
import com.app.chao.chaoapp.bean.VideoInfo;
import com.app.chao.chaoapp.utils.ImageLoader;

import java.util.List;

/**
 * Created by Chao on 2017/3/21.
 */

public class SpecialAdapter extends CommonAdapter<VideoInfo> {

    public SpecialAdapter(Context context, int layoutId, List<VideoInfo> datas) {
        super(context, layoutId, datas);
    }

    @Override
    protected void convert(ViewHolder holder, VideoInfo videoInfo, int position) {
        holder.setText(R.id.tv_title, videoInfo.title);
        ImageView iv = holder.getView(R.id.img_video);
        iv.setScaleType(ImageView.ScaleType.FIT_XY);
        ViewGroup.LayoutParams params = iv.getLayoutParams();
        DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
        int width = dm.widthPixels / 2;//宽度为屏幕宽度一半
//        int height = data.getHeight()*width/data.getWidth();//计算View的高度
        params.height = (int) (width / 1.8);
        iv.setLayoutParams(params);
        ImageLoader.load(mContext, videoInfo.pic, iv);
    }
}
