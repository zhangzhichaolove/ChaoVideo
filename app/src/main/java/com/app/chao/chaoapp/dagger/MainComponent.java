package com.app.chao.chaoapp.dagger;

import android.support.v7.app.AppCompatActivity;

import javax.inject.Singleton;

import dagger.Component;

/**
 * 沟通部分Component
 * Created by Chao on 2017/4/6.
 */
@Singleton
@Component(modules = MainModule.class)// 作为桥梁，沟通调用者和依赖对象库
public interface MainComponent {

    //定义注入的方法
    void inject(AppCompatActivity activity);
}
