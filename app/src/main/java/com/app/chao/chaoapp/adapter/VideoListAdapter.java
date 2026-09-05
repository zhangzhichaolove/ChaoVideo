package com.app.chao.chaoapp.adapter;

import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.app.chao.chaoapp.R;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.data.VideoRecordEntity;
import com.app.chao.chaoapp.databinding.ItemRelatedBinding;
import com.app.chao.chaoapp.utils.ImageLoader;
import com.app.chao.chaoapp.utils.VideoSourceLabels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Grid adapter backed by DiffUtil so list refreshes only rebind changed videos. */
public final class VideoListAdapter extends ListAdapter<VideoRes, VideoListAdapter.Holder> {
    private OnItemClickListener listener;
    private final boolean showSources;

    public VideoListAdapter() {
        this(false);
    }

    public VideoListAdapter(boolean showSources) {
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
                        && Objects.equals(oldItem.getImg(), newItem.getImg())
                        && oldItem.getLocalWatchedEpisode() == newItem.getLocalWatchedEpisode()
                        && oldItem.getLocalProgressMs() == newItem.getLocalProgressMs()
                        && oldItem.getLocalDurationMs() == newItem.getLocalDurationMs();
            }
        });
        this.showSources = showSources;
    }

    private static String key(VideoRes video) {
        return VideoRecordEntity.keyOf(video);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemRelatedBinding.inflate(
                android.view.LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        VideoRes video = getItem(position);
        holder.title.setText(video.getTitle());
        holder.source.setVisibility(showSources ? View.VISIBLE : View.GONE);
        if (showSources) {
            String source = VideoSourceLabels.label(holder.itemView.getContext(), video);
            holder.source.setText(VideoSourceLabels.compact(holder.itemView.getContext(), video));
            holder.source.setContentDescription(source);
        }
        if (video.getLocalProgressMs() > 0) {
            int percent = video.getLocalDurationMs() > 0
                    ? (int) Math.min(100, video.getLocalProgressMs() * 100
                    / video.getLocalDurationMs()) : 0;
            holder.progress.setText(video.getLocalWatchedEpisode() > 0
                    ? holder.itemView.getContext().getString(R.string.video_progress_episode,
                    video.getLocalWatchedEpisode(), percent)
                    : holder.itemView.getContext().getString(
                    R.string.video_progress_percent, percent));
            holder.progress.setVisibility(View.VISIBLE);
        } else {
            holder.progress.setVisibility(View.GONE);
        }
        DisplayMetrics metrics = holder.itemView.getResources().getDisplayMetrics();
        ViewGroup.LayoutParams params = holder.cover.getLayoutParams();
        params.height = (int) ((metrics.widthPixels / 3f) / 1.1f);
        holder.cover.setLayoutParams(params);
        ImageLoader.load(holder.itemView.getContext(), video.getImg(), holder.cover);
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

    final class Holder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView progress;
        final TextView source;
        final ImageView cover;

        Holder(ItemRelatedBinding binding) {
            super(binding.getRoot());
            title = binding.tvTitle;
            progress = binding.tvProgress;
            source = binding.tvSource;
            if (showSources) {
                android.content.res.TypedArray attributes = itemView.getContext().obtainStyledAttributes(
                        new int[]{android.R.attr.textColorPrimary});
                int colors = attributes.getColor(0, android.graphics.Color.BLACK);
                attributes.recycle();
                title.setTextColor(colors);
                progress.setTextColor(colors);
            }
            cover = binding.imgVideo;
            cover.setScaleType(ImageView.ScaleType.FIT_XY);
            itemView.setOnClickListener(view -> {
                int position = getBindingAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(position, getItem(position));
                }
            });
            itemView.setOnLongClickListener(view -> {
                int position = getBindingAdapterPosition();
                return listener != null && position != RecyclerView.NO_POSITION
                        && listener.onItemLongClick(position, getItem(position));
            });
        }
    }

    public interface OnItemClickListener {
        void onItemClick(int position, VideoRes video);

        boolean onItemLongClick(int position, VideoRes video);
    }
}
