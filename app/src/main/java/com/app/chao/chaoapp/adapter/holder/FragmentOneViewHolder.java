package com.app.chao.chaoapp.adapter.holder;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.app.chao.chaoapp.R;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.utils.ImageLoader;

public final class FragmentOneViewHolder extends RecyclerView.ViewHolder {
    private final ImageView imgPicture;
    private final TextView title;

    public FragmentOneViewHolder(View itemView) {
        super(itemView);
        imgPicture = itemView.findViewById(R.id.img_video);
        title = itemView.findViewById(R.id.tv_title);
        imgPicture.setScaleType(ImageView.ScaleType.CENTER_CROP);
    }

    public void bind(VideoRes data) {
        title.setText(data.getTitle());
        ImageLoader.load(itemView.getContext(), data.getImg(), imgPicture);
    }
}
