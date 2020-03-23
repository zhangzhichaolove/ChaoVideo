package com.app.chao.chaoapp.utils;

import android.content.Context;
import android.content.Intent;

import com.app.chao.chaoapp.bean.VideoInfo;
import com.app.chao.chaoapp.bean.VideoRes;
import com.app.chao.chaoapp.ui.activity.GSYVVideoActivity;
import com.app.chao.chaoapp.ui.activity.JCVideoActivity;
import com.app.chao.chaoapp.ui.activity.SearchActivity;
import com.app.chao.chaoapp.ui.activity.VideoListActivity;


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
        context.startActivity(intent);
    }

    /**
     * 播放页
     *
     * @param context
     * @param videoInfo
     */
    public static void goJCVideoActivity(Context context, VideoInfo videoInfo) {
        Intent intent = new Intent(context, JCVideoActivity.class);
        intent.putExtra("videoInfo", videoInfo);
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

    /**
     * 专题列表页
     *
     * @param context
     * @param catalogId
     * @param title
     */
    public static void go2VideoListActivity(Context context, String catalogId, String title) {
        Intent intent = new Intent(context, VideoListActivity.class);
        intent.putExtra("catalogId", catalogId);
        intent.putExtra("title", title);
        context.startActivity(intent);
    }
//    public static void go2VideoListSearchActivity(Context context, String searchStr, String title) {
//        Intent intent = new Intent(context, VideoListActivity.class);
//        intent.putExtra("searchStr", searchStr);
//        intent.putExtra("title", title);
//        context.startActivity(intent);
//    }
//
//    public static void go2MainActivity(Context context) {
//        jump(context, MainActivity.class);
//        ((WelcomeActivity) context).finish();
//    }
//
//    private static void jump(Context a, Class<?> clazz) {
//        Intent intent = new Intent(a, clazz);
//        a.startActivity(intent);
//    }
}
