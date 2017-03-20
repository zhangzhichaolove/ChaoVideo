package com.app.chao.chaoapp.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.app.chao.chaoapp.R;
import com.app.chao.chaoapp.bean.VideoInfo;
import com.app.chao.chaoapp.utils.ImageLoader;
import com.app.chao.chaoapp.utils.JumpUtil;
import com.jude.rollviewpager.adapter.StaticPagerAdapter;

import java.util.List;

/**
 * Description: BannerAdapter
 */
public class BannerAdapter extends StaticPagerAdapter {

    private Context ctx;
    private List<VideoInfo> list;

    public BannerAdapter(Context ctx, List<VideoInfo> list) {
        this.ctx = ctx;
        this.list = list;
        removeEmpty(this.list);
    }

    private void removeEmpty(List<VideoInfo> list) {
        for (int i = 0; i < list.size(); i++) {
            if (!list.get(i).loadType.equals("video")) {
                list.remove(i);
                i--;
            }
        }
    }

    @Override
    public View getView(ViewGroup container, final int position) {
        ImageView imageView = new ImageView(ctx);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        imageView.setBackgroundResource(R.mipmap.default_320);
        //加载图片
        ImageLoader.load(ctx, list.get(position).pic, imageView);
        //点击事件
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JumpUtil.goGSYYVideoActivity(ctx, list.get(position));
            }
        });
        return imageView;
    }

    @Override
    public int getCount() {
        return list.size();
    }
}