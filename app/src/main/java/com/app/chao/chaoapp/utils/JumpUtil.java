package com.app.chao.chaoapp.utils;

import android.content.Context;
import android.content.Intent;

import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.data.VideoSource;
import com.app.chao.chaoapp.ui.activity.OfflineVideoActivity;
import com.app.chao.chaoapp.ui.activity.GSYVVideoActivity;
import com.app.chao.chaoapp.ui.activity.SearchActivity;


/**
 * Description: JumpUtil
 */
public class JumpUtil {

    /**
     * 播放页
     *
     * @param context
     * @param videoInfo
     */
    public static void goGSYYVideoActivity(Context context, VideoRes videoInfo) {
        if (videoInfo != null && videoInfo.getId() != null) {
            String source = videoInfo.getSourceId();
            boolean legacyStored = VideoSource.LEGACY.equals(source)
                    && videoInfo.getStoredLibraryKey() != null;
            if ((VideoSource.DOWNLOAD.equals(source) || legacyStored)
                    && videoInfo.getId().startsWith("download:")) {
                context.startActivity(new Intent(context, OfflineVideoActivity.class)
                        .putExtra(OfflineVideoActivity.EXTRA_DOWNLOAD_ID,
                                videoInfo.getId().substring("download:".length()))
                        .putExtra(OfflineVideoActivity.EXTRA_LEGACY_RECORD, legacyStored ? videoInfo : null));
                return;
            }
            if ((VideoSource.LOCAL.equals(source) || legacyStored)
                    && videoInfo.getId().startsWith("local:") && videoInfo.getVideo() != null
                    && videoInfo.getVideo().startsWith("content://")) {
                context.startActivity(new Intent(context, OfflineVideoActivity.class)
                        .setData(android.net.Uri.parse(videoInfo.getVideo()))
                        .putExtra(OfflineVideoActivity.EXTRA_LEGACY_RECORD, legacyStored ? videoInfo : null));
                return;
            }
        }
        Intent intent = new Intent(context, GSYVVideoActivity.class);
        intent.putExtra("videoInfo", videoInfo);
        if (videoInfo != null && videoInfo.getLocalWatchedEpisode() > 0) {
            intent.putExtra(GSYVVideoActivity.EXTRA_EPISODE,
                    videoInfo.getLocalWatchedEpisode());
        }
        context.startActivity(intent);
    }

    /**
     * 跳转搜索界面
     *
     * @param context
     */
    public static void goSearchActivity(Context context) {
        Intent intent = new Intent(context, SearchActivity.class);
        context.startActivity(intent);
    }

}
