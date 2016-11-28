package me.ccrama.redditslide;

import javax.inject.Singleton;

import dagger.Component;
import me.ccrama.redditslide.Activities.BaseActivity;

/**
 * Created by marwan on 2016-11-14.
 */
@Component(modules = SlideModule.class)
@Singleton
public interface SlideComponent {
    BaseActivity inject(BaseActivity activity);
}