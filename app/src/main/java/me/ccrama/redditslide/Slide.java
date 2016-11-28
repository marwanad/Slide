package me.ccrama.redditslide;

import android.app.Application;
import android.support.multidex.MultiDexApplication;

import me.ccrama.redditslide.data.api.RedditComponent;

/**
 * Created by marwan on 2016-11-14.
 */
public class Slide extends MultiDexApplication {
    private RedditComponent redditComponent;

    @Override
    public void onCreate() {
        super.onCreate();

        redditComponent = DaggerRedditComponent.builder()
                .slideModule(new SlideModule(this)).build();

    }

    public RedditComponent getRedditComponent() {
        return redditComponent;
    }
}
