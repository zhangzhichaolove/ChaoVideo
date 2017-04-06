package com.app.chao.chaoapp.dagger;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * 先看实例化部分（容器）
 */
@Module//提供依赖对象的实例
public class MainModule {
    private Context context;

    public MainModule(Context context) {
        this.context = context;
    }

    @Provides
    Context getContext() {
        return context;
    }

    @Provides// 关键字，标明该方法提供依赖对象
    @Singleton
    public Persion getPersion(Context context) {

        //提供Person对象
        return new Persion(context);
    }
}