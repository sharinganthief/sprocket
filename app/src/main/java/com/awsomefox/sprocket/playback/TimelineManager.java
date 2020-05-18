/*
 * Copyright (C) 2017 Simon Norberg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.awsomefox.sprocket.playback;

import android.content.SharedPreferences;
import android.text.format.DateUtils;

import com.awsomefox.sprocket.data.api.MediaService;
import com.awsomefox.sprocket.data.local.TrackDAO;
import com.awsomefox.sprocket.data.model.Track;
import com.awsomefox.sprocket.data.repository.MusicRepository;
import com.awsomefox.sprocket.util.Rx;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import timber.log.Timber;

import static android.support.v4.media.session.PlaybackStateCompat.STATE_PAUSED;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_STOPPED;
import static com.awsomefox.sprocket.playback.MusicService.TRACK_KEY;
import static com.awsomefox.sprocket.playback.MusicService.TRACK_LIBRARY_ID;
import static com.awsomefox.sprocket.playback.MusicService.TRACK_PARENT_KEY;
import static com.awsomefox.sprocket.playback.MusicService.TRACK_PROGRESS;
import static com.awsomefox.sprocket.playback.MusicService.TRACK_URI;

/**
 * Updates the Plex server of current playback status
 */
class TimelineManager {

    private final MediaController mediaController;
    private final ContextManager contextManager;
    private final MediaService media;
    private final Rx rx;
    private CompositeDisposable disposables;
    private AtomicLong currentTrackedProgress = new AtomicLong(0L);
    private SharedPreferences preferences;
    private MusicRepository musicRepository;

    TimelineManager(MediaController mediaController, ContextManager contextManager,
                    MediaService media, MusicRepository musicRepository, Rx rx,
                    SharedPreferences preferences) {
        this.mediaController = mediaController;
        this.contextManager = contextManager;
        this.media = media;
        this.rx = rx;
        this.preferences = preferences;
        this.musicRepository = musicRepository;
        disposables = new CompositeDisposable();

    }

    void reset() {
        currentTrackedProgress.set(0L);
        stop();
        disposables = new CompositeDisposable();
        start();
    }

    private void start() {

        disposables.add(Flowable.combineLatest(state(), currentTrack(), progress(),
                (state, track, time) -> new Timeline(state, time, track))
                .observeOn(rx.io())
                .flatMapCompletable(this::updateTimeline)
                .subscribeOn(rx.io())
                .observeOn(rx.io())
                .subscribe(() -> Timber.d("onCompleted"), Rx::onError));
    }

    private Completable updateTimeline(Timeline t) {
        //if no track set the current track to the timeline track
        preferences.edit().putLong(TRACK_PROGRESS, t.time).apply();
//        trackDAO.addTrack(t.track);
        persistCurrentTrack(t.track);
        if (currentTrackedProgress.get() > t.time) {
            currentTrackedProgress.set(0L);
        }

        currentTrackedProgress.set(t.time);
        Timber.d("Sending progress update at %s",
                DateUtils.formatElapsedTime(t.time / 1000));
        return Completable.mergeArray(media.timeline(t.track.uri(), t.track.queueItemId(), t.track.key(),
                t.track.ratingKey(), t.state, t.track.duration(), t.time),
                musicRepository.addTracks(Collections.singletonList(
                        t.track.toBuilder().viewOffset(t.time).build())))
                .onErrorComplete(); // Skip errors;
    }

    private void persistCurrentTrack(Track track) {
        preferences.edit().putString(TRACK_URI, track.uri().toString()).apply();
        preferences.edit().putString(TRACK_KEY, track.key()).apply();
        preferences.edit().putString(TRACK_PARENT_KEY, track.parentKey()).apply();
        preferences.edit().putString(TRACK_LIBRARY_ID, track.libraryId()).apply();
    }

    private Flowable<Long> progress() {
        return mediaController.progress()
                .filter(progress -> (progress - currentTrackedProgress.get()) > 10000 || progress
                        < currentTrackedProgress.get());
        // Send updates every 10 seconds playtime or when current progress out of sync
    }

    private Flowable<Track> currentTrack() {
        return contextManager.queue()
                .filter(pair -> pair.second < pair.first.size())
                .map(pair -> pair.first.get(pair.second));
    }

    private Flowable<String> state() {
        Timber.d("TIMELINE STATE");
        return mediaController.state()
                .filter(state -> state == STATE_PLAYING || state == STATE_PAUSED
                        || state == STATE_STOPPED)
                .map(state -> {
                    if (state == STATE_PLAYING) {
                        return "playing";
                    } else if (state == STATE_PAUSED) {
                        return "paused";
                    }
                    return "stopped";
                });
    }

    void stop() {
        Rx.dispose(disposables);
    }

    private static class Timeline {
        private final String state;
        private final long time;
        private final Track track;

        Timeline(String state, long time, Track track) {
            this.state = state;
            this.time = time;
            this.track = track;
        }
    }
}
