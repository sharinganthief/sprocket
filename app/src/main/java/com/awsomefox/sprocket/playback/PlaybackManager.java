/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.media.session.PlaybackStateCompat.State;

import androidx.annotation.NonNull;

import com.awsomefox.sprocket.AndroidClock;
import com.awsomefox.sprocket.R;
import com.awsomefox.sprocket.data.model.Track;
import com.awsomefox.sprocket.util.Strings;
import com.awsomefox.sprocket.util.Urls;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.HttpUrl;
import timber.log.Timber;

import static android.support.v4.media.session.PlaybackStateCompat.STATE_BUFFERING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_CONNECTING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_ERROR;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_FAST_FORWARDING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_NONE;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_PAUSED;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_REWINDING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_SKIPPING_TO_NEXT;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_STOPPED;
import static com.awsomefox.sprocket.ui.PlayerController.BUNDLE_AUTO;
import static com.awsomefox.sprocket.ui.PlayerController.BUNDLE_TRACK_KEY;
import static com.awsomefox.sprocket.ui.PlayerController.BUNDLE_TRACK_LIBRARY_ID;
import static com.awsomefox.sprocket.ui.PlayerController.BUNDLE_TRACK_PARENT_KEY;
import static com.awsomefox.sprocket.ui.PlayerController.BUNDLE_TRACK_URI;
import static com.awsomefox.sprocket.ui.PlayerController.CUSTOM_ACTION_BACK;
import static com.awsomefox.sprocket.ui.PlayerController.CUSTOM_ACTION_FORWARD;
import static com.awsomefox.sprocket.ui.PlayerController.CUSTOM_ACTION_SPEED;
import static com.awsomefox.sprocket.ui.PlayerController.SPEED;

class PlaybackManager implements Playback.Callback {

    private final QueueManager queueManager;
    private final MediaSessionCallback sessionCallback;
    private final PlaybackServiceCallback serviceCallback;
    private final AndroidClock androidClock;
    private Playback playback;

    PlaybackManager(QueueManager queueManager, PlaybackServiceCallback serviceCallback,
                    AndroidClock androidClock, Playback playback) {
        this.queueManager = queueManager;
        this.serviceCallback = serviceCallback;
        this.androidClock = androidClock;
        this.playback = playback;
        this.playback.setCallback(this);
        this.sessionCallback = new MediaSessionCallback();
    }

    public Playback getPlayback() {
        return playback;
    }

    MediaSessionCompat.Callback getMediaSessionCallback() {
        return sessionCallback;
    }

    private void handlePlayRequest() {
        Track currentQueueItem = queueManager.currentTrack();
        if (currentQueueItem != null) {
            playback.play(currentQueueItem, queueManager.getSpeed());
            serviceCallback.onPlaybackStart();
        }
    }

    private void handlePlayRequest(Track track, float speed) {
        serviceCallback.onPlaybackStart(track, speed);
    }

    private void handlePauseRequest() {
        if (playback.isPlaying()) {
            playback.pause(queueManager.getSpeed());
            serviceCallback.onPlaybackPause();
        }
    }

    void handleStopRequest() {
        playback.stop(true);
        serviceCallback.onPlaybackStop();
        updatePlaybackState();
    }

    void updatePlaybackState() {
        long position = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;
        float speed = 1.0f;
        if (playback != null && playback.isConnected()) {
            position = playback.getCurrentStreamPosition();
            speed = playback.getSpeed();
        }

        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(getAvailableActions());

        addCustomActions(stateBuilder);

        @State int state = playback.getState();
        stateBuilder.setState(state, position, speed, androidClock.elapsedRealTime());

        serviceCallback.onPlaybackStateUpdated(stateBuilder.build(),
                getMetadataBuilder(playback.getCurrentTrack()).build(), getQueueList());

        if (state == STATE_PLAYING || state == STATE_PAUSED) {
            serviceCallback.onNotificationRequired();
        }
    }

    private List<MediaSessionCompat.QueueItem> getQueueList() {
        List<MediaSessionCompat.QueueItem> upNext = new ArrayList<>();
        for (Track track : queueManager.getUpNextQueue()) {
            upNext.add(new MediaSessionCompat.QueueItem(getDescriptionBuilder(track).build(),
                    track.queueItemId()));
        }
        return upNext;
    }

    @NotNull
    private MediaMetadataCompat.Builder getMetadataBuilder(Track track) {
        MediaMetadataCompat.Builder metadata = new MediaMetadataCompat.Builder();
        if (track != null) {
            metadata.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.artistTitle())
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, track.albumTitle())
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, String.format(Locale.US,
                            "Chapter %d", track.index()))
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, track.duration())
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, track.thumb());
        }
        return metadata;
    }

    @NotNull
    private MediaDescriptionCompat.Builder getDescriptionBuilder(Track track) {
        MediaDescriptionCompat.Builder description = new MediaDescriptionCompat.Builder();
        if (track != null) {
            description.setIconUri(Uri.parse(Uri.decode(track.thumb())))
                    .setTitle(String.format(Locale.US, "Chapter %d", track.index()))
                    .setSubtitle(track.albumTitle());
        }
        return description;
    }

    private void addCustomActions(PlaybackStateCompat.Builder stateBuilder) {
        stateBuilder.addCustomAction(CUSTOM_ACTION_BACK, "Skip Back", R.drawable.skip_back);
        stateBuilder.addCustomAction(CUSTOM_ACTION_FORWARD, "Skip Forward",
                R.drawable.skip_forward);
        stateBuilder.addCustomAction(CUSTOM_ACTION_SPEED, "Speed Control",
                R.drawable.player_speed);
    }

    private long getAvailableActions() {
        long actions = PlaybackStateCompat.ACTION_PLAY_PAUSE
                | PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                | PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH;
        if (playback.isPlaying()) {
            actions |= PlaybackStateCompat.ACTION_PAUSE;
        } else {
            actions |= PlaybackStateCompat.ACTION_PLAY;
        }
        return actions;
    }

    @Override
    public void onCompletion() {
        Timber.d("onCompletion");
        if (queueManager.hasNext()) {
            queueManager.next();
            handlePlayRequest();
        } else {
            handleStopRequest();
        }
    }

    @Override
    public void onPlaybackStatusChanged() {
        Timber.d("onPlaybackStatusChanged");
        updatePlaybackState();
    }

    @Override
    public void setCurrentTrack(Track track) {
        Timber.d("setCurrentTrack %s", track);
        queueManager.setCurrentTrack(track);
    }

    /**
     * Switch to a different Playback instance, maintaining all playback state, if possible.
     *
     * @param newPlayback switch to this playback
     */
    void switchToPlayback(@NonNull Playback newPlayback, boolean resumePlaying) {
        Timber.d("switchToPlayback %s resume %s", newPlayback.getClass().getSimpleName(),
                resumePlaying);
        // Suspend the current one
        @State int oldState = playback.getState();
        long position = playback.getCurrentStreamPosition();
        Track currentMediaId = playback.getCurrentTrack();
        playback.stop(false);
        newPlayback.setCallback(this);
        newPlayback.setCurrentTrack(currentMediaId);
        newPlayback.seekTo(Math.max(position, 0), queueManager.getSpeed());
        newPlayback.start();
        // Finally swap the instance
        playback = newPlayback;
        switch (oldState) {
            case STATE_BUFFERING:
            case STATE_CONNECTING:
            case STATE_PAUSED:
                playback.pause(queueManager.getSpeed());
                break;
            case STATE_PLAYING:
                Track currentQueueItem = queueManager.currentTrack();
                if (resumePlaying && currentQueueItem != null) {
                    playback.play(currentQueueItem, queueManager.getSpeed());
                } else if (!resumePlaying) {
                    playback.pause(queueManager.getSpeed());
                } else {
                    playback.stop(true);
                }
                break;

            case STATE_ERROR:
            case STATE_FAST_FORWARDING:
            case STATE_NONE:
            case STATE_REWINDING:
            case STATE_SKIPPING_TO_NEXT:
            case STATE_SKIPPING_TO_PREVIOUS:
            case STATE_SKIPPING_TO_QUEUE_ITEM:
            case STATE_STOPPED:
            default:
        }
    }

    interface PlaybackServiceCallback {
        void onPlaybackStart();

        void onPlaybackStart(Track track, float speed);

        void onPlaybackPause();

        void onPlaybackStop();

        void onNotificationRequired();

        void onPlaybackStateUpdated(PlaybackStateCompat newState,
                                    MediaMetadataCompat metadataCompat,
                                    List<MediaSessionCompat.QueueItem> queueItemList);
    }

    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            Timber.d("onPlay");
            handlePlayRequest();
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            HttpUrl uri = HttpUrl.get(extras.getString(BUNDLE_TRACK_URI, "http://plex.com"));
            String key = extras.getString(BUNDLE_TRACK_KEY, "1234");
            String parentKey = extras.getString(BUNDLE_TRACK_PARENT_KEY, "1234");
            String libraryId = extras.getString(BUNDLE_TRACK_LIBRARY_ID, "1234");
            Track track = Track.builder().queueItemId(0)
                    .libraryId(libraryId)
                    .key(key)
                    .ratingKey("track.ratingKey")
                    .parentKey(parentKey)
                    .title("track.title")
                    .albumTitle("track.parentTitle")
                    .artistTitle("track.grandparentTitle")
                    .index(0)
                    .duration(0L)
                    .viewOffset(0L)
                    .viewCount(1)
                    .thumb(Strings.isBlank("track.thumb") ? null : Urls.addPathToUrl(uri,
                            "track.thumb").toString())
                    .source(Urls.addPathToUrl(uri, "track.media.part.key").toString())
                    .uri(uri)
                    .recent(true)
                    .build();
            handlePlayRequest(track, extras.getFloat(SPEED, 1.0f));
        }

        @Override
        public void onSkipToQueueItem(long id) {
            Timber.d("onSkipToQueueItem %s", id);
            queueManager.setQueuePosition(id);
            handlePlayRequest();
        }

        @Override
        public void onPause() {
            Timber.d("onPause");
            handlePauseRequest();
        }

        @Override
        public void onSkipToNext() {
            Timber.d("onSkipToNext");
            queueManager.next();
            handlePlayRequest();
        }

        @Override
        public void onSkipToPrevious() {
            Timber.d("onSkipToPrevious");
            if (playback.getCurrentStreamPosition() > 1500) {
                playback.seekTo(0, queueManager.getSpeed());
                return;
            }
            queueManager.previous();
            handlePlayRequest();
        }

        @Override
        public void onStop() {
            Timber.d("onStop");
            handleStopRequest();
        }

        @Override
        public void onSeekTo(long position) {
            Timber.d("onSeekTo %s", position);
            playback.seekTo((int) position, queueManager.getSpeed());
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {
            Timber.d("onCustomAction %s", action);
            if (CUSTOM_ACTION_SPEED.equals(action)) {
                //if not android auto set speed from queumanager
                //if android auto increment the speed
                if (!extras.getBoolean(BUNDLE_AUTO, true)) {
                    playback.setSpeed(queueManager.getSpeed());
                } else {
                    queueManager.setSpeed(queueManager.getSpeed() + .1f);
                    playback.setSpeed(queueManager.getSpeed());
                }
            }
            if (CUSTOM_ACTION_BACK.equals(action)) {
                onSeekTo(playback.getCurrentStreamPosition() - 30000);
            }
            if (CUSTOM_ACTION_FORWARD.equals(action)) {
                onSeekTo(playback.getCurrentStreamPosition() + 30000);
            }
        }
    }
}
