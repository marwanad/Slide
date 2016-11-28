package me.ccrama.redditslide;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * This module is used to provide a reference to the application context.
 * Created by marwan on 2016-11-14.
 */

@Module
public class SlideModule {
    private Slide app;

    public SlideModule(Slide app) {
        this.app = app;
    }

    @Provides
    @Singleton
    Slide providesApplication() {
        return app;
    }
}
