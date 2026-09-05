package com.app.chao.chaoapp.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.Formatter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.offline.Download;
import androidx.media3.exoplayer.offline.DownloadService;

import com.app.chao.chaoapp.R;
import com.app.chao.chaoapp.download.VideoDownloadService;
import com.app.chao.chaoapp.download.VideoDownloads;
import com.app.chao.chaoapp.utils.StatusBarUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@androidx.annotation.OptIn(markerClass = UnstableApi.class)
public class DownloadsActivity extends AppCompatActivity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Download> downloads = new ArrayList<>();
    private VideoDownloads store;
    private ArrayAdapter<Download> adapter;
    private TextView empty;
    private boolean visible;
    private final Runnable refresh = this::loadDownloads;
    private final ActivityResultLauncher<String[]> filePicker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;
                try {
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException error) {
                    Toast.makeText(this, R.string.local_permission_temporary, Toast.LENGTH_LONG).show();
                }
                startActivity(new Intent(this, OfflineVideoActivity.class).setData(uri));
            });

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_downloads);
        Toolbar toolbar = findViewById(R.id.downloads_toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.downloads_title);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(view -> finish());
        StatusBarUtils.applyPageInsets(findViewById(android.R.id.content), toolbar);
        store = VideoDownloads.get(this);
        empty = findViewById(R.id.downloads_empty);
        findViewById(R.id.open_local_video).setOnClickListener(view -> filePicker.launch(new String[]{"video/*"}));
        ListView list = findViewById(R.id.downloads_list);
        adapter = new ArrayAdapter<Download>(this, android.R.layout.simple_list_item_2, android.R.id.text1, downloads) {
            @Override public View getView(int position, View recycled, ViewGroup parent) {
                View view = super.getView(position, recycled, parent);
                Download download = getItem(position);
                ((TextView) view.findViewById(android.R.id.text1)).setText(VideoDownloads.title(download));
                ((TextView) view.findViewById(android.R.id.text2)).setText(describe(download));
                return view;
            }
        };
        list.setAdapter(adapter);
        list.setEmptyView(empty);
        list.setOnItemClickListener((parent, view, position, id) -> showActions(downloads.get(position)));
    }

    @Override protected void onStart() {
        super.onStart();
        visible = true;
        loadDownloads();
    }

    @Override protected void onStop() {
        visible = false;
        handler.removeCallbacks(refresh);
        super.onStop();
    }

    private void loadDownloads() {
        if (!visible) return;
        store.load(new VideoDownloads.DownloadsCallback() {
            @Override public void onLoaded(List<Download> result) {
                if (!visible || isDestroyed()) return;
                downloads.clear();
                downloads.addAll(result);
                adapter.notifyDataSetChanged();
                empty.setText(R.string.downloads_empty);
                handler.removeCallbacks(refresh);
                handler.postDelayed(refresh, 1000);
            }
            @Override public void onError() {
                if (!visible || isDestroyed()) return;
                empty.setText(R.string.downloads_load_failed);
                handler.postDelayed(refresh, 3000);
            }
        });
    }

    private String describe(Download download) {
        int status;
        switch (download.state) {
            case Download.STATE_COMPLETED: status = R.string.download_complete; break;
            case Download.STATE_DOWNLOADING: status = R.string.download_running; break;
            case Download.STATE_STOPPED: status = R.string.download_paused; break;
            case Download.STATE_FAILED: return store.failure(download);
            case Download.STATE_REMOVING: status = R.string.download_removing; break;
            default: status = R.string.download_waiting;
        }
        String progress = download.getPercentDownloaded() < 0 ? ""
                : String.format(Locale.getDefault(), " · %.0f%%", download.getPercentDownloaded());
        return getString(status) + progress + " · " + Formatter.formatFileSize(this, download.getBytesDownloaded())
                + (download.request.mimeType == null ? "" : " · " + download.request.mimeType);
    }

    private void showActions(Download download) {
        if (download.state == Download.STATE_REMOVING) return;
        ArrayList<String> labels = new ArrayList<>();
        ArrayList<Runnable> actions = new ArrayList<>();
        if (download.state == Download.STATE_COMPLETED) {
            labels.add(getString(R.string.download_play));
            actions.add(() -> startActivity(new Intent(this, OfflineVideoActivity.class)
                    .putExtra(OfflineVideoActivity.EXTRA_DOWNLOAD_ID, download.request.id)));
        }
        if (download.state == Download.STATE_FAILED || download.state == Download.STATE_COMPLETED) {
            labels.add(getString(R.string.download_retry));
            actions.add(() -> {
                // Removal followed by add also repairs a completed but corrupt cache entry.
                DownloadService.sendRemoveDownload(this, VideoDownloadService.class, download.request.id, true);
                DownloadService.sendAddDownload(this, VideoDownloadService.class, download.request, true);
            });
        } else if (download.state == Download.STATE_STOPPED) {
            labels.add(getString(R.string.download_resume));
            actions.add(() -> DownloadService.sendSetStopReason(this, VideoDownloadService.class,
                    download.request.id, Download.STOP_REASON_NONE, true));
        } else {
            labels.add(getString(R.string.download_pause));
            actions.add(() -> DownloadService.sendSetStopReason(this, VideoDownloadService.class,
                    download.request.id, VideoDownloads.USER_PAUSED, true));
        }
        labels.add(getString(R.string.download_delete));
        actions.add(() -> new AlertDialog.Builder(this).setMessage(R.string.download_delete_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.download_delete, (dialog, which) ->
                        DownloadService.sendRemoveDownload(this, VideoDownloadService.class, download.request.id, true)).show());
        new AlertDialog.Builder(this).setTitle(VideoDownloads.title(download))
                .setItems(labels.toArray(new String[0]), (dialog, which) -> actions.get(which).run()).show();
    }
}
