package com.app.chao.chaoapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class GuideActivity extends AppCompatActivity {
    private static final long SPLASH_DURATION_MS = 3000;
    private ImageView mBgImg;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable openHome = () -> {
        startActivity(new Intent(GuideActivity.this, HomeActivity.class));
        finish();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(
                getWindow(), getWindow().getDecorView());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        controller.hide(WindowInsetsCompat.Type.systemBars());
        setContentView(R.layout.activity_guide);
        mBgImg = findViewById(R.id.splash_bg_img);
        mBgImg.setImageResource(R.mipmap.bilibili_start);
        handler.postDelayed(openHome, SPLASH_DURATION_MS);
    }

    @Override
    protected void onResume() {
        super.onResume();
        float offset = 12 * getResources().getDisplayMetrics().density;
        mBgImg.animate().cancel();
        mBgImg.setScaleX(1.06f);
        mBgImg.setScaleY(1.06f);
        mBgImg.setTranslationX(-offset);
        mBgImg.setTranslationY(offset / 2);
        mBgImg.animate()
                .scaleX(1.14f)
                .scaleY(1.14f)
                .translationX(offset)
                .translationY(-offset / 2)
                .setDuration(SPLASH_DURATION_MS)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    @Override
    protected void onPause() {
        mBgImg.animate().cancel();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(openHome);
        super.onDestroy();
    }
}
