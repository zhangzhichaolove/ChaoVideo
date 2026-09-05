package com.app.chao.chaoapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.databinding.RecommendHeaderBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The recommendation banner rendered as the first full-width list item. */
public final class RecommendHeaderAdapter
        extends RecyclerView.Adapter<RecommendHeaderAdapter.Holder> {
    private List<VideoRes> videos = Collections.emptyList();
    private View.OnClickListener searchClickListener;
    private ViewPager2 banner;

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(RecommendHeaderBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        banner = holder.binding.banner;
        banner.setAdapter(new BannerAdapter(holder.itemView.getContext(), videos));
        holder.binding.rlGoSearch.setOnClickListener(searchClickListener);
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    public void setVideos(List<VideoRes> videos) {
        this.videos = videos == null
                ? Collections.emptyList() : new ArrayList<>(videos);
        notifyItemChanged(0);
    }

    public void setOnSearchClickListener(View.OnClickListener listener) {
        searchClickListener = listener;
    }

    public void advanceBanner() {
        if (banner != null && videos.size() > 1) {
            banner.setCurrentItem((banner.getCurrentItem() + 1) % videos.size(), true);
        }
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final RecommendHeaderBinding binding;

        Holder(RecommendHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
