package com.app.chao.chaoapp;

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.flaviofaria.kenburnsview.KenBurnsView;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class GuideActivity extends AppCompatActivity {
    @BindView(R.id.splash_bg_img)
    KenBurnsView mBgImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //去除title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //去掉Activity上面的状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //getSupportActionBar().hide();
        setContentView(R.layout.activity_guide);
        //绑定activity
        ButterKnife.bind(this);
        //Glide.with(this).load(R.drawable.pic_cinema).into(mBgImg);
        mBgImg.setImageResource(R.mipmap.bilibili_start);
        Observable.create(new Observable.OnSubscribe<Drawable>() {
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void call(Subscriber<? super Drawable> subscriber) {
                try {
                    Thread.sleep(3000);//模拟加载广告图片，在IO线程。
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Drawable drawable = getDrawable(R.mipmap.pic_cinema);
                subscriber.onNext(drawable);
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io()) // 指定 subscribe() 发生在 IO 线程
                .observeOn(AndroidSchedulers.mainThread()) // 指定 Subscriber 的回调发生在主线程
                .subscribe(new Observer<Drawable>() {
                    @Override
                    public void onNext(Drawable drawable) {
                        //mBgImg.setImageDrawable(drawable);//TODO 这里图片已经加载成功
                        //mBgImg.setImageResource(R.mipmap.pic_cinema);
                    }

                    @Override
                    public void onCompleted() {
                        startActivity(new Intent(GuideActivity.this, HomeActivity.class));
                        finish();
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        Toast.makeText(GuideActivity.this, "Error!",
                                Toast.LENGTH_SHORT).show();
                    }
                });

    }

    @Override
    protected void onPause() {
        super.onPause();
        mBgImg.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBgImg.resume();
    }
}
