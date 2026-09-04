package com.app.chao.chaoapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

public class GuideActivity extends AppCompatActivity {
    android.widget.ImageView mBgImg;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable openHome = () -> {
        startActivity(new Intent(GuideActivity.this, HomeActivity.class));
        finish();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //去除title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //去掉Activity上面的状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //getSupportActionBar().hide();
        setContentView(R.layout.activity_guide);
        mBgImg = findViewById(R.id.splash_bg_img);
        //绑定activity
        //Glide.with(this).load(R.drawable.pic_cinema).into(mBgImg);
        mBgImg.setImageResource(R.mipmap.bilibili_start);
        handler.postDelayed(openHome, 3000);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(openHome);
        super.onDestroy();
    }
}
