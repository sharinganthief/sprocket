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

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.media.session.MediaButtonReceiver;
import androidx.mediarouter.media.MediaRouter;

import com.awsomefox.sprocket.AndroidClock;
import com.awsomefox.sprocket.MediaNotificationManager;
import com.awsomefox.sprocket.SprocketApp;
import com.awsomefox.sprocket.data.LoginManager;
import com.awsomefox.sprocket.data.ServerManager;
import com.awsomefox.sprocket.data.api.MediaService;
import com.awsomefox.sprocket.data.model.Track;
import com.awsomefox.sprocket.data.repository.MusicRepository;
import com.awsomefox.sprocket.ui.PlayerController;
import com.awsomefox.sprocket.ui.SprocketActivity;
import com.awsomefox.sprocket.util.Rx;
import com.awsomefox.sprocket.util.Strings;
import com.awsomefox.sprocket.util.Urls;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManager;
import com.google.android.gms.cast.framework.SessionManagerListener;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import timber.log.Timber;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_NONE;

public class MusicService extends Service implements PlaybackManager.PlaybackServiceCallback {

    public static final String ACTION_STOP_CASTING = "com.awsomefox.sprocket.ACTION_STOP_CASTING";

    private static final int STOP_DELAY = 30000;
    private final IBinder binder = new LocalBinder();
    private final DelayedStopHandler delayedStopHandler = new DelayedStopHandler(this);
    @Inject
    public QueueManager queueManager;
    @Inject
    MediaController mediaController;
    @Inject
    AudioManager audioManager;
    @Inject
    LoginManager loginManager; //needed for access credentials
    @Inject
    WifiManager wifiManager;
    @Inject
    MediaService media;
    @Inject
    public MusicRepository musicRepository;
    @Inject
    public ServerManager serverManager;
    @Inject
    public
    Rx rx;
    @Inject
    @Named("default")
    OkHttpClient client;
    private PlaybackManager playbackManager;
    public MediaSessionCompat session;
    private MediaNotificationManager mediaNotificationManager;
    private MediaRouter mediaRouter;
    private SessionManager castSessionManager;
    private SessionManagerListener<CastSession> castSessionManagerListener;
    private TimelineManager timelineManager;

    SharedPreferences preferences;
    CompositeDisposable disposables;

    private static MusicService instance;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.d("onCreate");
        SprocketApp.get(this).component().inject(this);

        Playback playback = new LocalPlayback(getApplicationContext(), mediaController,
                audioManager, wifiManager, client);

        session = new MediaSessionCompat(this, "MusicService");

        try {
            MediaControllerCompat mediaController =
                    new MediaControllerCompat(this.getApplicationContext(),
                            session.getSessionToken());
            this.mediaController.setMediaController(mediaController);
        } catch (RemoteException e) {
            Timber.e(e, "Could not create MediaController");
            throw new IllegalStateException();
        }
        playbackManager = new PlaybackManager(queueManager, this,
                AndroidClock.DEFAULT, playback);

        session.setCallback(playbackManager.getMediaSessionCallback());

        Context context = getApplicationContext();
        Intent intent = new Intent(context, SprocketActivity.class);
        session.setSessionActivity(PendingIntent.getActivity(context, 99, intent,
                FLAG_UPDATE_CURRENT));

        playbackManager.updatePlaybackState();

        mediaNotificationManager = new MediaNotificationManager(this, mediaController,
                queueManager, rx);

        castSessionManager = CastContext.getSharedInstance(this).getSessionManager();
        castSessionManagerListener = new CastSessionManagerListener();
        castSessionManager.addSessionManagerListener(castSessionManagerListener, CastSession.class);

        mediaRouter = MediaRouter.getInstance(getApplicationContext());

        preferences = Objects.requireNonNull(getApplicationContext()).getSharedPreferences(
                "playback-state", Context.MODE_PRIVATE);
        timelineManager = new TimelineManager(mediaController, queueManager, media, rx,
                preferences);
        instance = this;
        disposables = new CompositeDisposable();
        serverManager.refresh();
        if (mediaController.getPlaybackState() == null
                || (mediaController.getPlaybackState().getState() == STATE_NONE)) {
            Timber.d("Restoring state");
            Track track = getPersistedTrack();
            disposables.add(musicRepository.createPlayQueue(track)
                    .compose(rx.singleSchedulers())
                    .subscribe(pair -> {
                        //restore playing
                        queueManager.setQueue(pair.first, pair.second, getPersistedProgress());
                        updateSpeed(preferences.getFloat(PlayerController.SPEED, 1.0f));
                        mediaController.play();
                        mediaController.pause();
                    }, Rx::onError));
        }
    }

    void updateSpeed(float speed) {
        queueManager.setSpeed(speed);
        mediaController.setSpeed(speed);
    }

    Track getPersistedTrack() {
        HttpUrl uri = HttpUrl.get(preferences.getString("trackUri", "http://plex.com"));
        String key = preferences.getString("trackKey", "1234");
        String parentKey = preferences.getString("trackParentKey", "1234");
        String libraryId = preferences.getString("trackLibraryId", "1234");
        return Track.builder().queueItemId(0)
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
    }

    long getPersistedProgress() {
        return preferences.getLong("trackProgress", 0L);
    }

    @Override
    public int onStartCommand(Intent startIntent, int flags, int startId) {
        Timber.d("onStartCommand");
        timelineManager.reset();
        if (startIntent != null) {
            if (ACTION_STOP_CASTING.equals(startIntent.getAction())) {
                CastContext.getSharedInstance(this).getSessionManager().endCurrentSession(true);
                onDestroy();
                onCreate();
            } else {
                // Try to handle the intent as a media button event wrapped by MediaButtonReceiver
                MediaButtonReceiver.handleIntent(session, startIntent);
            }
        }
        delayedStopHandler.removeCallbacksAndMessages(null);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // Service is being killed, so make sure we release our resources
        playbackManager.handleStopRequest();
        mediaNotificationManager.stopNotification();

        if (castSessionManager != null) {
            castSessionManager.removeSessionManagerListener(castSessionManagerListener,
                    CastSession.class);
        }

        timelineManager.stop();

        delayedStopHandler.removeCallbacksAndMessages(null);
        session.release();
        Rx.dispose(disposables);
        instance = null;
    }

    @Override
    public void onCompletion(Track track) {
        media.scrobble(track.uri(), track.ratingKey()).subscribeOn(Schedulers.io()).subscribe();
    }

    @Override
    public void onPlaybackStart(Track track, float speed) {
        Timber.d("ON PLAYBAKC START with track");
        session.setActive(true);

        delayedStopHandler.removeCallbacksAndMessages(null);

        // The service needs to continue running even after the bound client (usually a
        // MediaController) disconnects, otherwise the music playback will stop.
        // Calling startService(Intent) will keep the service running until it is explicitly killed.
        ContextCompat.startForegroundService(getApplicationContext(),
                new Intent(getApplicationContext(), MusicService.class));

        disposables.add(musicRepository.createPlayQueue(track)
                .subscribeOn(Schedulers.io())
                .subscribe(pair -> {
                    queueManager.setQueue(pair.first, pair.second, 0L);
                    updateSpeed(speed);
                    mediaController.play();
                }, Rx::onError));
    }

    @Override
    public void onPlaybackStart() {
        Timber.d("ON PLAYBAKC START");
        session.setActive(true);

        delayedStopHandler.removeCallbacksAndMessages(null);

        // The service needs to continue running even after the bound client (usually a
        // MediaController) disconnects, otherwise the music playback will stop.
        // Calling startService(Intent) will keep the service running until it is explicitly killed.
        ContextCompat.startForegroundService(getApplicationContext(),
                new Intent(getApplicationContext(), MusicService.class));
    }

    @Override
    public void onPlaybackPause() {
        Timber.d("ON PLAYBAKC pause");
        stopForeground(false);
    }

    @Override
    public void onPlaybackStop() {
        session.setActive(false);

        // Reset the delayed stop handler, so after STOP_DELAY it will be executed again,
        // potentially stopping the service.
        delayedStopHandler.removeCallbacksAndMessages(null);
        delayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);
        stopForeground(false);
    }

    @Override
    public void onNotificationRequired() {
        mediaNotificationManager.startNotification();
    }

    @Override
    public void onPlaybackStateUpdated(PlaybackStateCompat newState,
                                       MediaMetadataCompat metadataCompat,
                                       List<MediaSessionCompat.QueueItem> queueItemList) {
        session.setPlaybackState(newState);
        session.setMetadata(metadataCompat);
        session.setQueue(queueItemList);
        session.setQueueTitle(metadataCompat.getString(METADATA_KEY_ALBUM));
    }

    /**
     * A simple handler that stops the service if playback is not active (playing)
     */
    private static class DelayedStopHandler extends Handler {
        private final WeakReference<MusicService> weakReference;

        private DelayedStopHandler(MusicService service) {
            weakReference = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            MusicService service = weakReference.get();
            if (service != null && service.playbackManager.getPlayback() != null) {
                if (!service.playbackManager.getPlayback().isPlaying()) {
                    service.stopSelf();
                }
            }
        }
    }

    public static MusicService getInstance() {
        Timber.d("Get instance is null? %b", (instance == null));
        return instance;
    }

    private static class LocalBinder extends Binder {
    }

    /**
     * Session Manager Listener responsible for switching the Playback instances
     * depending on whether it is connected to a remote player.
     */
    private class CastSessionManagerListener implements SessionManagerListener<CastSession> {
        @Override
        public void onSessionEnded(CastSession castSession, int error) {
            Timber.d("onSessionEnded");
            Intent stopCastIntent = new Intent(getApplicationContext(), MusicService.class);
            stopCastIntent.setAction(MusicService.ACTION_STOP_CASTING);
            ContextCompat.startForegroundService(getInstance(), stopCastIntent);
        }

        @Override
        public void onSessionResumed(CastSession session, boolean wasSuspended) {
        }

        @Override
        public void onSessionStarted(CastSession castSession, String sessionId) {
            Timber.d("onSessionStarted %s", sessionId);
            mediaController.setCastName(castSession.getCastDevice().getFriendlyName());
            Playback playback = new CastPlayback(MusicService.this);
            mediaRouter.setMediaSessionCompat(session);
            playbackManager.switchToPlayback(playback, true);
        }

        @Override
        public void onSessionStarting(CastSession session) {
        }

        @Override
        public void onSessionStartFailed(CastSession session, int error) {
        }

        @Override
        public void onSessionEnding(CastSession session) {
            // This is our final chance to update the underlying stream position
            // In onSessionEnded(), the underlying CastPlayback#mRemoteMediaClient
            // is disconnected and hence we update our local value of stream position
            // to the latest position.
            playbackManager.getPlayback().updateLastKnownStreamPosition();
        }

        @Override
        public void onSessionResuming(CastSession session, String sessionId) {
        }

        @Override
        public void onSessionResumeFailed(CastSession session, int error) {
        }

        @Override
        public void onSessionSuspended(CastSession session, int reason) {
        }
    }
}
