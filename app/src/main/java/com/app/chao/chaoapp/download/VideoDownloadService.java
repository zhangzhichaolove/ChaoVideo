package com.app.chao.chaoapp.download;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;

import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.offline.Download;
import androidx.media3.exoplayer.offline.DownloadManager;
import androidx.media3.exoplayer.offline.DownloadNotificationHelper;
import androidx.media3.exoplayer.offline.DownloadService;
import androidx.media3.exoplayer.scheduler.PlatformScheduler;
import androidx.media3.exoplayer.scheduler.Scheduler;

import com.app.chao.chaoapp.R;
import com.app.chao.chaoapp.ui.activity.DownloadsActivity;

import java.util.List;

@UnstableApi
public class VideoDownloadService extends DownloadService {
    private static final String CHANNEL = "video_downloads";

    public VideoDownloadService() {
        super(2101, DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL, CHANNEL,
                R.string.downloads_title, R.string.downloads_description);
    }

    @Override protected DownloadManager getDownloadManager() {
        DownloadManager manager = VideoDownloads.get(this).manager();
        manager.resumeDownloads();
        return manager;
    }

    @Override protected Scheduler getScheduler() {
        return new PlatformScheduler(this, 2102);
    }

    @Override protected Notification getForegroundNotification(List<Download> downloads, int notMetRequirements) {
        PendingIntent intent = PendingIntent.getActivity(this, 0,
                new Intent(this, DownloadsActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new DownloadNotificationHelper(this, CHANNEL).buildProgressNotification(this,
                android.R.drawable.stat_sys_download, intent, null, downloads, notMetRequirements);
    }
}
