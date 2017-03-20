package com.app.chao.chaoapp.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.app.chao.chaoapp.R;
import com.app.chao.chaoapp.adapter.holder.ClassificationHolder;
import com.app.chao.chaoapp.bean.VideoInfo;
import com.app.chao.chaoapp.utils.ImageLoader;

import java.util.List;

/**
 * Created by Chao on 2017/3/20.
 */

public class ClassificationAdapter extends RecyclerView.Adapter<ClassificationHolder> {
    Context context;
    List<VideoInfo> videoRes;

    public ClassificationAdapter(Context context, List<VideoInfo> videoRes) {
        this.context = context;
        this.videoRes = videoRes;
    }


    @Override
    public ClassificationHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ClassificationHolder holder = new ClassificationHolder(LayoutInflater.from(
                context).inflate(R.layout.item_found, parent,
                false));
        return holder;
    }

    @Override
    public void onBindViewHolder(ClassificationHolder holder, int position) {
        holder.tv_title.setText(videoRes.get(position).title);
        ViewGroup.LayoutParams params = holder.imgPicture.getLayoutParams();
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        int width = dm.widthPixels / 2;//宽度为屏幕宽度一半
//        int height = data.getHeight()*width/data.getWidth();//计算View的高度
        params.height = (int) (width / 1.8);
        holder.imgPicture.setLayoutParams(params);
        ImageLoader.load(context, videoRes.get(position).pic, holder.imgPicture);
    }

    @Override
    public int getItemCount() {
        if (videoRes == null) return 0;
        else
            return videoRes.size();
    }
}
