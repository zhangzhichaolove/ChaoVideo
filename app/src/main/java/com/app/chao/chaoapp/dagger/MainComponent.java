package com.app.chao.chaoapp.dagger;

import com.app.chao.chaoapp.HomeActivity;

import dagger.Component;

@Component(modules = MainModule.class)
public interface MainComponent {
    void inject(HomeActivity mainActivity);
}