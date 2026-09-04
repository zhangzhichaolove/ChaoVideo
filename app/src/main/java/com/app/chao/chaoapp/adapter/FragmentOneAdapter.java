package com.app.chao.chaoapp.adapter;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import com.app.chao.chaoapp.adapter.holder.FragmentOneViewHolder;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.databinding.ItemVideoBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class FragmentOneAdapter extends ListAdapter<VideoRes, FragmentOneViewHolder> {
    private OnItemClickListener listener;

    public FragmentOneAdapter(android.content.Context ignored) {
        super(new DiffUtil.ItemCallback<VideoRes>() {
            @Override
            public boolean areItemsTheSame(@NonNull VideoRes oldItem,
                                           @NonNull VideoRes newItem) {
                return key(oldItem).equals(key(newItem));
            }

            @Override
            public boolean areContentsTheSame(@NonNull VideoRes oldItem,
                                              @NonNull VideoRes newItem) {
                return Objects.equals(oldItem.getTitle(), newItem.getTitle())
                        && Objects.equals(oldItem.getImg(), newItem.getImg());
            }
        });
    }

    private static String key(VideoRes video) {
        return video.getId() != null && !video.getId().trim().isEmpty()
                ? "id:" + video.getId().trim()
                : "url:" + Objects.toString(video.video, "");
    }

    @NonNull
    @Override
    public FragmentOneViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new FragmentOneViewHolder(ItemVideoBinding.inflate(
                android.view.LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull FragmentOneViewHolder holder, int position) {
        holder.bind(getItem(position));
        holder.itemView.setOnClickListener(view -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (listener != null
                    && adapterPosition != androidx.recyclerview.widget.RecyclerView.NO_POSITION) {
                listener.onItemClick(adapterPosition);
            }
        });
    }

    public int getCount() {
        return getItemCount();
    }

    @Override
    public VideoRes getItem(int position) {
        return super.getItem(position);
    }

    public void setData(List<VideoRes> videos) {
        submitList(videos == null ? Collections.emptyList() : new ArrayList<>(videos));
    }

    public void addAll(List<VideoRes> videos) {
        if (videos == null || videos.isEmpty()) {
            return;
        }
        List<VideoRes> combined = new ArrayList<>(getCurrentList());
        combined.addAll(videos);
        submitList(combined);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }
}
