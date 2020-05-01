package com.awsomefox.sprocket.playback;

import com.awsomefox.sprocket.util.Rx;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.reactivex.Flowable;

@Module
public class PlaybackModule {

  @Provides Flowable<Long> provideSeconds() {
    return Flowable.interval(1, TimeUnit.SECONDS);
  }

  @Provides Random provideRandom() {
    return new Random();
  }

    @Provides
    @Singleton
    MediaController provideMusicController(Flowable<Long> seconds, Rx rx) {
        return new MediaController(seconds, rx);
  }

    @Provides
    @Singleton
    QueueManager provideQueueManager() {
        return new QueueManager();
  }
}
