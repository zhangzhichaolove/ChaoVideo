package com.app.chao.chaoapp.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.chao.chaoapp.adapter.holder.FragmentOneViewHolder;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.R;

import java.util.ArrayList;
import java.util.List;

public final class FragmentOneAdapter extends RecyclerView.Adapter<FragmentOneViewHolder> {
    private final List<VideoRes> items = new ArrayList<>();
    private OnItemClickListener listener;

    public FragmentOneAdapter(android.content.Context ignored) {
    }

    @NonNull
    @Override
    public FragmentOneViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new FragmentOneViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_video, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull FragmentOneViewHolder holder, int position) {
        holder.bind(items.get(position));
        holder.itemView.setOnClickListener(view -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (listener != null && adapterPosition != RecyclerView.NO_POSITION) {
                listener.onItemClick(adapterPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public int getCount() {
        return items.size();
    }

    public VideoRes getItem(int position) {
        return items.get(position);
    }

    public void clear() {
        int count = items.size();
        items.clear();
        notifyItemRangeRemoved(0, count);
    }

    public void addAll(List<VideoRes> videos) {
        if (videos == null || videos.isEmpty()) {
            return;
        }
        int start = items.size();
        items.addAll(videos);
        notifyItemRangeInserted(start, videos.size());
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }
}
