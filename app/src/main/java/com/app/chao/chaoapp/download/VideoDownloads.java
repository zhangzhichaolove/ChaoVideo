package com.app.chao.chaoapp.download;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.MediaItem;
import androidx.media3.database.StandaloneDatabaseProvider;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.datasource.cache.NoOpCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;
import androidx.media3.exoplayer.offline.Download;
import androidx.media3.exoplayer.offline.DownloadCursor;
import androidx.media3.exoplayer.offline.DownloadManager;
import androidx.media3.exoplayer.offline.DownloadHelper;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.offline.DownloadRequest;
import androidx.media3.exoplayer.scheduler.Requirements;

import com.app.chao.chaoapp.R;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@UnstableApi
public final class VideoDownloads {
    public static final int USER_PAUSED = 1;
    private static VideoDownloads instance;
    private static final OkHttpClient PROBE_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS).build();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService indexExecutor = Executors.newSingleThreadExecutor();
    private final SimpleCache cache;
    private final DownloadManager manager;
    private final SharedPreferences failures;
    private final Context context;

    private VideoDownloads(Context context) {
        this.context = context.getApplicationContext();
        StandaloneDatabaseProvider database = new StandaloneDatabaseProvider(this.context);
        // Offline downloads must not be evicted by the streaming player's LRU cache.
        cache = new SimpleCache(new File(this.context.getFilesDir(), "offline-media"),
                new NoOpCacheEvictor(), database);
        manager = new DownloadManager(this.context, database, cache,
                new DefaultHttpDataSource.Factory().setUserAgent("ChaoVideo")
                        .setConnectTimeoutMs(15_000).setReadTimeoutMs(20_000),
                Executors.newFixedThreadPool(2));
        manager.setMaxParallelDownloads(2);
        manager.setRequirements(new Requirements(Requirements.NETWORK | Requirements.DEVICE_STORAGE_NOT_LOW));
        failures = this.context.getSharedPreferences("download_failures", Context.MODE_PRIVATE);
        manager.addListener(new DownloadManager.Listener() {
            @Override public void onDownloadChanged(DownloadManager manager, Download download, Exception error) {
                if (download.state == Download.STATE_FAILED) {
                    failures.edit().putString(download.request.id, describeFailure(error)).apply();
                } else {
                    failures.edit().remove(download.request.id).apply();
                }
            }
            @Override public void onDownloadRemoved(DownloadManager manager, Download download) {
                failures.edit().remove(download.request.id).apply();
            }
        });
    }

    public static synchronized VideoDownloads get(Context context) {
        if (instance == null) instance = new VideoDownloads(context);
        return instance;
    }

    public DownloadManager manager() {
        return manager;
    }

    /** No HTTP upstream: offline playback must fail visibly rather than silently stream. */
    public CacheDataSource.Factory offlineDataSource() {
        return new CacheDataSource.Factory().setCache(cache)
                .setUpstreamDataSourceFactory(null).setCacheWriteDataSinkFactory(null);
    }

    @androidx.annotation.MainThread
    public Preparation prepare(String url, String title, RequestCallback callback) {
        Request probe = new Request.Builder().url(url).header("Range", "bytes=0-8191").build();
        Call call = PROBE_CLIENT.newCall(probe);
        Preparation preparation = new Preparation(call, callback);
        main.postDelayed(preparation.timeout, 45_000);
        call.enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException error) {
                main.post(() -> preparation.fail(context.getString(R.string.download_probe_failed)));
            }
            @Override public void onResponse(Call call, Response response) {
                try (Response closeable = response) {
                    if (!closeable.isSuccessful()) {
                        main.post(() -> preparation.fail(context.getString(R.string.download_http_error, closeable.code())));
                        return;
                    }
                    String type = DownloadMediaType.detect(url, closeable.header("Content-Type"),
                            closeable.peekBody(8192).bytes());
                    main.post(() -> {
                        if (call.isCanceled()) return;
                        MediaItem item = new MediaItem.Builder().setUri(url).setMimeType(type).build();
                        DownloadHelper helper = new DownloadHelper.Factory()
                                .setRenderersFactory(new DefaultRenderersFactory(context))
                                .setDataSourceFactory(new DefaultHttpDataSource.Factory().setUserAgent("ChaoVideo")
                                        .setConnectTimeoutMs(15_000).setReadTimeoutMs(20_000)).create(item);
                        preparation.helper = helper;
                        helper.prepare(new DownloadHelper.Callback() {
                            @Override public void onPrepared(DownloadHelper ready, boolean tracksInfoAvailable) {
                                if (preparation.finished) return;
                                DownloadRequest request = ready.getDownloadRequest(DownloadMediaType.id(url),
                                        (title == null ? "" : title).getBytes(StandardCharsets.UTF_8));
                                preparation.cancel();
                                callback.onReady(request);
                            }
                            @Override public void onPrepareError(DownloadHelper failed, IOException error) {
                                preparation.fail(context.getString(R.string.download_prepare_failed));
                            }
                        });
                    });
                } catch (IOException | IllegalArgumentException error) {
                    main.post(() -> preparation.fail(context.getString(R.string.download_probe_failed)));
                }
            }
        });
        return preparation;
    }

    public void load(DownloadsCallback callback) {
        indexExecutor.execute(() -> {
            List<Download> downloads = new ArrayList<>();
            try (DownloadCursor cursor = manager.getDownloadIndex().getDownloads()) {
                while (cursor.moveToNext()) downloads.add(cursor.getDownload());
                java.util.Collections.sort(downloads,
                        (first, second) -> Long.compare(second.startTimeMs, first.startTimeMs));
                main.post(() -> callback.onLoaded(downloads));
            } catch (IOException error) {
                main.post(callback::onError);
            }
        });
    }

    public String failure(Download download) {
        return failures.getString(download.request.id, context.getString(R.string.download_failure_detail));
    }

    private String describeFailure(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof HttpDataSource.InvalidResponseCodeException) {
                return context.getString(R.string.download_http_error,
                        ((HttpDataSource.InvalidResponseCodeException) cause).responseCode);
            }
        }
        return context.getString(R.string.download_failure_detail);
    }

    public static String title(Download download) {
        return new String(download.request.data, StandardCharsets.UTF_8);
    }

    public interface RequestCallback {
        void onReady(DownloadRequest request);
        void onError(String message);
    }

    public final class Preparation {
        private final Call call;
        private final RequestCallback callback;
        private DownloadHelper helper;
        private boolean finished;
        private final Runnable timeout = () -> fail(context.getString(R.string.download_prepare_failed));

        private Preparation(Call call, RequestCallback callback) {
            this.call = call;
            this.callback = callback;
        }

        private void fail(String message) {
            if (finished) return;
            cancel();
            callback.onError(message);
        }

        @androidx.annotation.MainThread
        public void cancel() {
            finished = true;
            main.removeCallbacks(timeout);
            call.cancel();
            if (helper != null) {
                helper.release();
                helper = null;
            }
        }
    }

    public interface DownloadsCallback {
        void onLoaded(List<Download> downloads);
        void onError();
    }
}
