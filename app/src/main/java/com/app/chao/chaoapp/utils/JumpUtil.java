package com.app.chao.chaoapp.utils;

import android.content.Context;
import android.content.Intent;

import com.app.chao.chaoapp.bean.VideoRes;
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
