package com.app.chao.chaoapp.utils;

import android.content.Context;
import android.content.Intent;

import com.app.chao.chaoapp.bean.VideoInfo;
import com.app.chao.chaoapp.ui.activity.GSYVVideoActivity;
import com.app.chao.chaoapp.ui.activity.JCVideoActivity;


/**
 * Description: JumpUtil
 */
public class JumpUtil {

    public static void goGSYYVideoActivity(Context context, VideoInfo videoInfo) {
        Intent intent = new Intent(context, GSYVVideoActivity.class);
        intent.putExtra("videoInfo", videoInfo);
        context.startActivity(intent);
    }

    public static void goJCVideoActivity(Context context, VideoInfo videoInfo) {
        Intent intent = new Intent(context, JCVideoActivity.class);
        intent.putExtra("videoInfo", videoInfo);
        context.startActivity(intent);
    }

//    public static void go2VideoListActivity(Context context, String catalogId, String title) {
//        Intent intent = new Intent(context, VideoListActivity.class);
//        intent.putExtra("catalogId", catalogId);
//        intent.putExtra("title", title);
//        context.startActivity(intent);
//    }
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
