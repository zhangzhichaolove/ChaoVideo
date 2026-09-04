package com.app.chao.chaoapp.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.chao.chaoapp.R;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.utils.ImageLoader;
import com.app.chao.chaoapp.utils.JumpUtil;

import java.util.List;

/**
 * Description: BannerAdapter
 */
public final class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.ViewHolder> {

    private Context ctx;
    private List<VideoRes> list;

    public BannerAdapter(Context ctx, List<VideoRes> list) {
        this.ctx = ctx;
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ImageView imageView = new ImageView(ctx);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setBackgroundResource(R.mipmap.default_320);
        return new ViewHolder(imageView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        //加载图片
        ImageLoader.load(ctx, list.get(position).getImg(), holder.image);
        holder.image.setContentDescription(list.get(position).getTitle());
        //点击事件
        holder.image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int adapterPosition = holder.getBindingAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    JumpUtil.goGSYYVideoActivity(ctx, list.get(adapterPosition));
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView image;

        ViewHolder(ImageView image) {
            super(image);
            this.image = image;
        }
    }
}
