package com.app.chao.chaoapp.dagger;

import dagger.Module;
import dagger.Provides;

@Module
public class MainModule {

    @Provides
    public Cloth getCloth() {
        Cloth cloth = new Cloth();
        cloth.setColor("红色");
        return cloth;
    }
}