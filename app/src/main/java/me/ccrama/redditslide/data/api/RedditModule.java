package me.ccrama.redditslide.data.api;

import net.dean.jraw.RedditClient;
import net.dean.jraw.fluent.FluentRedditClient;
import net.dean.jraw.http.HttpAdapter;
import net.dean.jraw.http.UserAgent;

import dagger.Module;
import dagger.Provides;
import me.ccrama.redditslide.ApplicationScope;

/**
 * This module handles all networking-related stuff that deals with Reddit's API,
 * authentication, caching, etc
 *
 * Created by marwan on 2016-11-28.
 */
@Module
public final class RedditModule {
    @Provides
    @ApplicationScope
    RedditClient provideRedditClient(UserAgent userAgent, HttpAdapter adapter) {
        return new RedditClient(userAgent, adapter);
    }

    static FluentRedditClient createFluentRedditClient(RedditClient client) {
        return new FluentRedditClient(client);
    }
}
