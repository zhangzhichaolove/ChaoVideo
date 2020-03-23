package com.app.chao.chaoapp.adapter;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.app.chao.chaoapp.R;
import com.app.chao.chaoapp.baseadapter.recyclerview.CommonAdapter;
import com.app.chao.chaoapp.baseadapter.recyclerview.base.ViewHolder;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.utils.ImageLoader;

import java.util.List;

/**
 * Created by Chao on 2017/3/22.
 */

public class VideoListAdapter extends CommonAdapter<VideoRes> {


    public VideoListAdapter(Context context, int layoutId, List<VideoRes> datas) {
        super(context, layoutId, datas);
    }

    @Override
    protected void convert(ViewHolder holder, VideoRes videoType, int position) {
        holder.setText(R.id.tv_title, videoType.getTitle());
        ImageView iv = holder.getView(R.id.img_video);
        iv.setScaleType(ImageView.ScaleType.FIT_XY);
        ViewGroup.LayoutParams params = iv.getLayoutParams();
        DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
        int width = dm.widthPixels / 3;//宽度为屏幕宽度1/3
//        int height = data.getHeight()*width/data.getWidth();//计算View的高度
        params.height = (int) (width / 1.1);
        iv.setLayoutParams(params);
        ImageLoader.load(mContext, videoType.getImg(), iv);
    }
}
