package me.ccrama.redditslide.data.api;

import javax.inject.Singleton;

import dagger.Component;
import me.ccrama.redditslide.Activities.BaseActivity;
import me.ccrama.redditslide.SlideModule;

/**
 * Created by marwan on 2016-11-28.
 */
@Singleton
@Component(modules = {SlideModule.class})

public interface RedditComponent {
    void inject(BaseActivity activity);
}
