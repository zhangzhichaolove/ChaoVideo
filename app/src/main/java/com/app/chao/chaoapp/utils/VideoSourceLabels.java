package com.app.chao.chaoapp.utils;

import android.content.Context;
import com.app.chao.chaoapp.R;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.data.VideoSource;

public final class VideoSourceLabels {
    private VideoSourceLabels() { }

    public static String compact(Context context, VideoRes video) {
        String base = video.getSourceBaseUrl();
        // Keep path/port visible in narrow cards instead of spending the first line on "来源：http:".
        return base == null ? label(context, video) : base.replaceFirst("^https?://", "");
    }

    public static String label(Context context, VideoRes video) {
        switch (video.getSourceId()) {
            case VideoSource.LEGACY: return context.getString(R.string.source_legacy);
            case VideoSource.LOCAL: return context.getString(R.string.source_local);
            case VideoSource.DOWNLOAD: return context.getString(R.string.source_download);
            default: return context.getString(R.string.source_api, video.getSourceBaseUrl());
        }
    }
}
