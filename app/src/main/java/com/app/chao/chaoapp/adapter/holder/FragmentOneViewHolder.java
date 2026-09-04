package com.app.chao.chaoapp.adapter.holder;

import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.databinding.ItemVideoBinding;
import com.app.chao.chaoapp.utils.ImageLoader;

public final class FragmentOneViewHolder extends RecyclerView.ViewHolder {
    private final ImageView imgPicture;
    private final TextView title;

    public FragmentOneViewHolder(ItemVideoBinding binding) {
        super(binding.getRoot());
        imgPicture = binding.imgVideo;
        title = binding.tvTitle;
        imgPicture.setScaleType(ImageView.ScaleType.CENTER_CROP);
    }

    public void bind(VideoRes data) {
        title.setText(data.getTitle());
        ImageLoader.load(itemView.getContext(), data.getImg(), imgPicture);
    }
}
