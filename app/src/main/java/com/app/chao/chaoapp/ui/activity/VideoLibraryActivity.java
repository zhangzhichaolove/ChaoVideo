package com.app.chao.chaoapp.ui.activity;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.chao.chaoapp.R;
import com.app.chao.chaoapp.adapter.VideoListAdapter;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.data.VideoLibraryRepository;
import com.app.chao.chaoapp.utils.GridSpacingItemDecoration;
import com.app.chao.chaoapp.utils.JumpUtil;
import com.app.chao.chaoapp.utils.ScreenUtil;
import com.app.chao.chaoapp.utils.StatusBarUtils;

import java.util.List;

public class VideoLibraryActivity extends BaseActivity {
    public static final String EXTRA_MODE = "mode";
    public static final String MODE_FAVORITES = "favorites";
    public static final String MODE_HISTORY = "history";

    private VideoListAdapter adapter;
    private TextView emptyView;
    private VideoLibraryRepository repository;
    private boolean historyMode;

    public static Intent favoritesIntent(android.content.Context context) {
        return new Intent(context, VideoLibraryActivity.class)
                .putExtra(EXTRA_MODE, MODE_FAVORITES);
    }

    public static Intent historyIntent(android.content.Context context) {
        return new Intent(context, VideoLibraryActivity.class)
                .putExtra(EXTRA_MODE, MODE_HISTORY);
    }

    @Override
    protected int getLayout() {
        return R.layout.activity_video_library;
    }

    @Override
    protected void init() {
        historyMode = MODE_HISTORY.equals(getIntent().getStringExtra(EXTRA_MODE));
        repository = VideoLibraryRepository.get(this);
        Toolbar toolbar = findViewById(R.id.library_toolbar);
        toolbar.setTitle(historyMode ? R.string.watch_history : R.string.video_favorites);
        StatusBarUtils.setTranslucent(this);
        StatusBarUtils.applyTopInset(findViewById(android.R.id.content), toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(view -> finish());

        emptyView = findViewById(R.id.library_empty);
        RecyclerView list = findViewById(R.id.library_list);
        GridLayoutManager manager = new GridLayoutManager(this, 3);
        list.setLayoutManager(manager);
        list.addItemDecoration(new GridSpacingItemDecoration(ScreenUtil.dip2px(this, 8)));
        adapter = new VideoListAdapter();
        list.setAdapter(adapter);
        adapter.setOnItemClickListener(new VideoListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position, VideoRes video) {
                JumpUtil.goGSYYVideoActivity(VideoLibraryActivity.this, video);
            }

            @Override
            public boolean onItemLongClick(int position, VideoRes video) {
                showItemActions(video);
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadItems();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (historyMode) {
            getMenuInflater().inflate(R.menu.video_library, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_clear_history) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.clear_watch_history)
                    .setMessage(R.string.clear_watch_history_confirm)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.clear, (dialog, which) ->
                            repository.clearHistory(this::loadItems))
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadItems() {
        VideoLibraryRepository.ValueCallback<List<VideoRes>> callback = this::showItems;
        if (historyMode) {
            repository.loadHistory(callback);
        } else {
            repository.loadFavorites(callback);
        }
    }

    private void showItems(List<VideoRes> videos) {
        adapter.setData(videos);
        emptyView.setVisibility(videos.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showItemActions(VideoRes video) {
        repository.isFavorite(video, favorite -> {
            String action = getString(favorite ? R.string.remove_favorite : R.string.add_favorite);
            new AlertDialog.Builder(this)
                    .setTitle(video.getTitle())
                    .setItems(new String[]{action}, (dialog, which) ->
                            repository.toggleFavorite(video, saved -> {
                                showToast(getString(saved
                                        ? R.string.favorite_added : R.string.favorite_removed));
                                loadItems();
                            }))
                    .show();
        });
    }
}
